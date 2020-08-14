/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
<<<<<<< HEAD
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
=======
>>>>>>> neo4j/4.1
import java.util.concurrent.locks.LockSupport;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.extension.DbmsExtension;
import org.neo4j.test.extension.Inject;
import org.neo4j.util.concurrent.Futures;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.neo4j.internal.helpers.collection.Iterables.asSet;

@DbmsExtension
class TokenCreationIT
{
    private static final int WORKERS = 10;

    @Inject
    private GraphDatabaseService db;

<<<<<<< HEAD
    /**
     * Token creation should be able to handle cases of concurrent token creation
     * with different/same names. Short random interval (1-3) give a high chances of the same token name in this test.
     * <p>
     * Newly created token should be visible only when token cache already have both mappings:
     * "name -> id" and "id -> name" populated.
     * Otherwise, attempt to retrieve labels from the newly created node can fail.
     */
=======
    private volatile boolean stop;
    private ExecutorService executorService;

    @BeforeEach
    void setUp()
    {
        executorService = Executors.newFixedThreadPool( WORKERS );
    }

    @AfterEach
    void tearDown()
    {
        executorService.shutdown();
    }

>>>>>>> neo4j/4.1
    @RepeatedTest( 5 )
    void concurrentLabelTokenCreation() throws InterruptedException, ExecutionException
    {
<<<<<<< HEAD
        AtomicBoolean stop = new AtomicBoolean();
        int concurrentWorkers = 10;
        CountDownLatch latch = new CountDownLatch( concurrentWorkers );
        for ( int i = 0; i < concurrentWorkers; i++ )
        {
            new LabelCreator( db, latch, stop ).start();
        }
        LockSupport.parkNanos( TimeUnit.MILLISECONDS.toNanos( 500 ) );
        stop.set( true );
=======
        CountDownLatch latch = new CountDownLatch( WORKERS );
        List<Future<?>> futures = new ArrayList<>();
        for ( int i = 0; i < WORKERS; i++ )
        {
            futures.add( executorService.submit( new LabelCreator( db, latch ) ) );
        }
        LockSupport.parkNanos( MILLISECONDS.toNanos( 500 ) );
        stop = true;
>>>>>>> neo4j/4.1
        latch.await();
        consumeFutures( futures );
    }

    private void consumeFutures( List<Future<?>> futures ) throws ExecutionException
    {
        Futures.getAll( futures );
    }

<<<<<<< HEAD
    private static class LabelCreator extends Thread
=======
    private Label[] getLabels()
    {
        int randomLabelValue = ThreadLocalRandom.current().nextInt( 2 ) + 1;
        Label[] labels = new Label[randomLabelValue];
        for ( int i = 0; i < labels.length; i++ )
        {
            labels[i] = Label.label( randomAlphanumeric( randomLabelValue ) );
        }
        return labels;
    }

    private class LabelCreator implements Runnable
>>>>>>> neo4j/4.1
    {
        private final GraphDatabaseService database;
        private final CountDownLatch createLatch;
        private final AtomicBoolean stop;

        LabelCreator( GraphDatabaseService database, CountDownLatch createLatch, AtomicBoolean stop )
        {
            this.database = database;
            this.createLatch = createLatch;
            this.stop = stop;
        }

        @Override
        public void run()
        {
            try
            {
                while ( !stop.get() )
                {

                    try ( Transaction transaction = database.beginTx() )
                    {
                        Label[] createdLabels = getLabels();
                        Node node = transaction.createNode( createdLabels );
                        Iterable<Label> nodeLabels = node.getLabels();
                        assertEquals( asSet( asList( createdLabels ) ), asSet( nodeLabels ) );
                        transaction.commit();
                    }
                    catch ( Exception e )
                    {
                        stop.set( true );
                        throw e;
                    }
                }
            }
            finally
            {
                createLatch.countDown();
            }
        }

        private Label[] getLabels()
        {
            int randomLabelValue = ThreadLocalRandom.current().nextInt( 2 ) + 1;
            Label[] labels = new Label[randomLabelValue];
            for ( int i = 0; i < labels.length; i++ )
            {
                labels[i] = Label.label( RandomStringUtils.randomAscii( randomLabelValue ) );
            }
            return labels;
        }
    }
}
