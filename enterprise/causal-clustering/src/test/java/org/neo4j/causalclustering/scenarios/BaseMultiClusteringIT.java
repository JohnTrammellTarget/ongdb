/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) as found
 * in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */
package org.neo4j.causalclustering.scenarios;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.causalclustering.core.CausalClusteringSettings;
import org.neo4j.causalclustering.core.consensus.roles.Role;
import org.neo4j.causalclustering.discovery.Cluster;
import org.neo4j.causalclustering.discovery.CoreClusterMember;
import org.neo4j.causalclustering.helpers.DataCreator;
import org.neo4j.causalclustering.identity.StoreId;
import org.neo4j.graphdb.Node;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.test.causalclustering.ClusterRule;
import org.neo4j.test.rule.fs.DefaultFileSystemRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.neo4j.causalclustering.TestStoreId.getStoreIds;
import static org.neo4j.causalclustering.discovery.Cluster.dataMatchesEventually;
import static org.neo4j.graphdb.Label.label;

@RunWith( Parameterized.class )
public abstract class BaseMultiClusteringIT
{
    protected static Set<String> DB_NAMES_1 = Collections.singleton( "default" );
    protected static Set<String> DB_NAMES_2 = Stream.of( "foo", "bar" ).collect( Collectors.toSet() );
    protected static Set<String> DB_NAMES_3 = Stream.of( "foo", "bar", "baz" ).collect( Collectors.toSet() );

    private final Set<String> dbNames;
    private final ClusterRule clusterRule;
    private final DefaultFileSystemRule fileSystemRule;
    private final DiscoveryServiceType discoveryType;

    @Rule
    public final RuleChain ruleChain;
    private Cluster<?> cluster;
    private FileSystemAbstraction fs;
    @Rule
    public Timeout globalTimeout = Timeout.seconds(300);

    protected BaseMultiClusteringIT( String ignoredName, int numCores, int numReplicas, Set<String> dbNames,
            DiscoveryServiceType discoveryServiceType )
    {
        this.dbNames = dbNames;
        this.discoveryType = discoveryServiceType;

        this.clusterRule = new ClusterRule()
            .withNumberOfCoreMembers( numCores )
            .withNumberOfReadReplicas( numReplicas )
            .withDatabaseNames( dbNames );

        this.fileSystemRule = new DefaultFileSystemRule();

        this.ruleChain = RuleChain.outerRule( fileSystemRule ).around( clusterRule );
    }

    @Before
    public void setup() throws Exception
    {
        clusterRule.withDiscoveryServiceType( discoveryType );
        fs = fileSystemRule.get();
        cluster = clusterRule.startCluster();
    }

    @Test
    public void shouldRunDistinctTransactionsAndDiverge() throws Exception
    {
        int numNodes = 1;
        Map<CoreClusterMember, List<CoreClusterMember>> leaderMap = new HashMap<>();
        for ( String dbName : dbNames )
        {
            int i = 0;
            CoreClusterMember leader;

            do
            {
                leader = cluster.coreTx( dbName, ( db, tx ) ->
                {
                    Node node = db.createNode( label("database") );
                    node.setProperty( "name" , dbName );
                    tx.success();
                } );
                i++;
            }
            while ( i < numNodes );

            int leaderId = leader.serverId();
            List<CoreClusterMember> notLeaders = cluster.coreMembers().stream()
                    .filter( m -> m.dbName().equals( dbName ) && m.serverId() != leaderId )
                    .collect( Collectors.toList() );

            leaderMap.put( leader, notLeaders );
            numNodes++;
        }

        Set<Long> nodesPerDb = leaderMap.keySet().stream()
                .map( DataCreator::countNodes ).collect( Collectors.toSet() );
        assertEquals("Each logical database in the multicluster should have a unique number of nodes.", nodesPerDb.size(), dbNames.size() );
        for ( Map.Entry<CoreClusterMember, List<CoreClusterMember>> subCluster : leaderMap.entrySet() )
        {
            dataMatchesEventually( subCluster.getKey(), subCluster.getValue() );
        }

    }

    @Test
    public void distinctDatabasesShouldHaveDistinctStoreIds() throws Exception
    {
        for ( String dbName : dbNames )
        {
            cluster.coreTx( dbName, ( db, tx ) ->
            {
                Node node = db.createNode( label("database") );
                node.setProperty( "name" , dbName );
                tx.success();
            } );
        }

        List<File> storeDirs = cluster.coreMembers().stream()
                .map( CoreClusterMember::databaseDirectory )
                .collect( Collectors.toList() );

        cluster.shutdown();

        Set<StoreId> storeIds = getStoreIds( storeDirs, fs );
        int expectedNumStoreIds = dbNames.size();
        assertEquals( "Expected distinct store ids for distinct sub clusters.", expectedNumStoreIds, storeIds.size());
    }

    @Test
    public void rejoiningFollowerShouldDownloadSnapshotFromCorrectDatabase() throws Exception
    {
        String dbName = getFirstDbName( dbNames );
        int followerId = cluster.getMemberWithAnyRole( dbName, Role.FOLLOWER ).serverId();
        cluster.removeCoreMemberWithServerId( followerId );

        for ( int  i = 0; i < 100; i++ )
        {
            cluster.coreTx( dbName, ( db, tx ) ->
            {
                Node node = db.createNode( label( dbName + "Node" ) );
                node.setProperty( "name", dbName );
                tx.success();
            } );
        }

        for ( CoreClusterMember m : cluster.coreMembers() )
        {
            m.raftLogPruner().prune();
        }

        cluster.addCoreMemberWithId( followerId ).start();

        CoreClusterMember dbLeader = cluster.awaitLeader( dbName );

        boolean followerIsHealthy = cluster.healthyCoreMembers().stream()
                .anyMatch( m -> m.serverId() == followerId );

        assertTrue( "Rejoining / lagging follower is expected to be healthy.", followerIsHealthy );

        CoreClusterMember follower = cluster.getCoreMemberById( followerId );

        dataMatchesEventually( dbLeader, Collections.singleton(follower) );

        List<File> storeDirs = cluster.coreMembers().stream()
                .filter( m -> dbName.equals( m.dbName() ) )
                .map( CoreClusterMember::databaseDirectory )
                .collect( Collectors.toList() );

        cluster.shutdown();

        Set<StoreId> storeIds = getStoreIds( storeDirs, fs );
        String message = "All members of a sub-cluster should have the same store Id after downloading a snapshot.";
        assertEquals( message, 1, storeIds.size() );
    }

    @Test
    public void shouldNotBeAbleToChangeClusterMembersDatabaseName() throws Exception
    {
        CoreClusterMember member = cluster.coreMembers().stream().findFirst().orElseThrow( IllegalArgumentException::new );

        Cluster.shutdownCoreMember( member );

        //given
        member.updateConfig( CausalClusteringSettings.database, "new_name" );

        try
        {
            //when
            Cluster.startCoreMember( member );
            fail( "Cluster member should fail to restart after database name change." );
        }
        catch ( ExecutionException e )
        {
            //expected
        }
    }

    //TODO: Test that rejoining followers wait for majority of hosts *for each database* to be available before joining

    private static String getFirstDbName( Set<String> dbNames ) throws Exception
    {
        return dbNames.stream()
                .findFirst()
                .orElseThrow( () -> new IllegalArgumentException( "The dbNames parameter must not be empty." ) );
    }
}
