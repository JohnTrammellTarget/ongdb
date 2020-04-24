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
package org.neo4j.causalclustering.core;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.neo4j.causalclustering.core.state.storage.SimpleFileStorage;
import org.neo4j.causalclustering.core.state.storage.SimpleStorage;
import org.neo4j.causalclustering.identity.MemberId;
import org.neo4j.graphdb.factory.module.PlatformModule;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;

public class IdentityModule
{
    public static final String CORE_MEMBER_ID_NAME = "core-member-id";

    private MemberId myself;

    IdentityModule( PlatformModule platformModule, File clusterStateDirectory )
    {
        FileSystemAbstraction fileSystem = platformModule.fileSystem;
        LogProvider logProvider = platformModule.logging.getInternalLogProvider();

        Log log = logProvider.getLog( getClass() );

        SimpleStorage<MemberId> memberIdStorage = new SimpleFileStorage<>( fileSystem, clusterStateDirectory,
                CORE_MEMBER_ID_NAME, new MemberId.Marshal(), logProvider );

        try
        {
            if ( memberIdStorage.exists() )
            {
                myself = memberIdStorage.readState();
                if ( myself == null )
                {
                    throw new RuntimeException( "I was null" );
                }
            }
            else
            {
                UUID uuid = UUID.randomUUID();
                myself = new MemberId( uuid );
                memberIdStorage.writeState( myself );

                log.info( String.format( "Generated new id: %s (%s)", myself, uuid ) );
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }

        platformModule.jobScheduler.setTopLevelGroupName( "Core " + myself );
    }

    public MemberId myself()
    {
        return myself;
    }
}
