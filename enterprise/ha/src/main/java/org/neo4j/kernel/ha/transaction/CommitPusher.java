/*
 * Copyright (c) 2018-2020 "Graph Foundation,"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */
package org.neo4j.kernel.ha.transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.neo4j.com.Response;
import org.neo4j.kernel.ha.com.master.Slave;
import org.neo4j.scheduler.Group;
import org.neo4j.scheduler.JobScheduler;

public class CommitPusher
{
    private static class PullUpdateFuture
            extends FutureTask<Object>
    {
        private final Slave slave;
        private final long txId;

        PullUpdateFuture( Slave slave, long txId )
        {
            super( () -> null );
            this.slave = slave;
            this.txId = txId;
        }

        @Override
        public void done()
        {
            super.set( null );
            super.done();
        }

        @Override
        public void setException( Throwable t )
        {
            super.setException( t );
        }

        public Slave getSlave()
        {
            return slave;
        }

        private long getTxId()
        {
            return txId;
        }
    }

    private static final int PULL_UPDATES_QUEUE_SIZE = 100;

    private final Map<Integer, BlockingQueue<PullUpdateFuture>> pullUpdateQueues = new HashMap<>();
    private final JobScheduler scheduler;

    public CommitPusher( JobScheduler scheduler )
    {
        this.scheduler = scheduler;
    }

    public void queuePush( Slave slave, final long txId )
    {
        PullUpdateFuture pullRequest = new PullUpdateFuture( slave, txId );

        BlockingQueue<PullUpdateFuture> queue = getOrCreateQueue( slave );

        // Add our request to the queue
        while ( !queue.offer( pullRequest ) )
        {
            Thread.yield();
        }

        try
        {
            // Wait for request to finish
            pullRequest.get();
        }
        catch ( InterruptedException e )
        {
            Thread.interrupted(); // Clear interrupt flag
            throw new RuntimeException( e );
        }
        catch ( ExecutionException e )
        {
            if ( e.getCause() instanceof RuntimeException )
            {
                throw (RuntimeException) e.getCause();
            }
            else
            {
                throw new RuntimeException( e.getCause() );
            }
        }
    }

    private synchronized BlockingQueue<PullUpdateFuture> getOrCreateQueue( Slave slave )
    {
        BlockingQueue<PullUpdateFuture> queue = pullUpdateQueues.get( slave.getServerId() );
        if ( queue == null )
        {
            // Create queue and worker
            queue = new ArrayBlockingQueue<>( PULL_UPDATES_QUEUE_SIZE );
            pullUpdateQueues.put( slave.getServerId(), queue );

            final BlockingQueue<PullUpdateFuture> finalQueue = queue;
            scheduler.schedule( Group.MASTER_TRANSACTION_PUSHING, new Runnable()
            {
                List<PullUpdateFuture> currentPulls = new ArrayList<>();

                @Override
                public void run()
                {
                    try
                    {
                        while ( true )
                        {
                            // Poll queue and call pullUpdate
                            currentPulls.clear();
                            currentPulls.add( finalQueue.take() );

                            finalQueue.drainTo( currentPulls );

                            try
                            {
                                PullUpdateFuture pullUpdateFuture = currentPulls.get( 0 );
                                askSlaveToPullUpdates( pullUpdateFuture );

                                // Notify the futures
                                for ( PullUpdateFuture currentPull : currentPulls )
                                {
                                    currentPull.done();
                                }
                            }
                            catch ( Exception e )
                            {
                                // Notify the futures
                                for ( PullUpdateFuture currentPull : currentPulls )
                                {
                                    currentPull.setException( e );
                                }
                            }
                        }
                    }
                    catch ( InterruptedException e )
                    {
                        // Quit
                    }
                }
            } );
        }
        return queue;
    }

    private void askSlaveToPullUpdates( PullUpdateFuture pullUpdateFuture )
    {
        Slave slave = pullUpdateFuture.getSlave();
        long lastTxId = pullUpdateFuture.getTxId();
        try ( Response<Void> ignored = slave.pullUpdates( lastTxId ) )
        {
            // Slave will come back to me(master) and pull updates
        }
    }
}
