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
package org.neo4j.causalclustering.core.state;

import java.util.Optional;

import org.neo4j.causalclustering.catchup.CatchupAddressProvider;
import org.neo4j.causalclustering.catchup.storecopy.LocalDatabase;
import org.neo4j.causalclustering.core.consensus.RaftMachine;
import org.neo4j.causalclustering.core.consensus.RaftMessages;
import org.neo4j.causalclustering.core.consensus.outcome.ConsensusOutcome;
import org.neo4j.causalclustering.core.state.snapshot.CoreStateDownloaderService;
import org.neo4j.causalclustering.identity.ClusterId;
import org.neo4j.causalclustering.messaging.LifecycleMessageHandler;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;
import org.neo4j.scheduler.JobHandle;

public class RaftMessageApplier implements LifecycleMessageHandler<RaftMessages.ReceivedInstantClusterIdAwareMessage<?>>
{
    private final LocalDatabase localDatabase;
    private final Log log;
    private final RaftMachine raftMachine;
    private final CoreStateDownloaderService downloadService;
    private final CommandApplicationProcess applicationProcess;
    private CatchupAddressProvider.PrioritisingUpstreamStrategyBasedAddressProvider catchupAddressProvider;

    public RaftMessageApplier( LocalDatabase localDatabase, LogProvider logProvider, RaftMachine raftMachine, CoreStateDownloaderService downloadService,
            CommandApplicationProcess applicationProcess, CatchupAddressProvider.PrioritisingUpstreamStrategyBasedAddressProvider catchupAddressProvider )
    {
        this.localDatabase = localDatabase;
        this.log = logProvider.getLog( getClass() );
        this.raftMachine = raftMachine;
        this.downloadService = downloadService;
        this.applicationProcess = applicationProcess;
        this.catchupAddressProvider = catchupAddressProvider;
    }

    @Override
    public synchronized void handle( RaftMessages.ReceivedInstantClusterIdAwareMessage<?> wrappedMessage )
    {
        try
        {
            ConsensusOutcome outcome = raftMachine.handle( wrappedMessage.message() );
            if ( outcome.needsFreshSnapshot() )
            {
                Optional<JobHandle> downloadJob = downloadService.scheduleDownload( catchupAddressProvider );
                if ( downloadJob.isPresent() )
                {
                    downloadJob.get().waitTermination();
                }
            }
            else
            {
                notifyCommitted( outcome.getCommitIndex() );
            }
        }
        catch ( Throwable e )
        {
            log.error( "Error handling message", e );
            raftMachine.panic();
            localDatabase.panic( e );
        }
    }

    @Override
    public synchronized void start( ClusterId clusterId )
    {
        // no-op
    }

    @Override
    public synchronized void stop()
    {
        // no-op
    }

    private void notifyCommitted( long commitIndex )
    {
        applicationProcess.notifyCommitted( commitIndex );
    }
}
