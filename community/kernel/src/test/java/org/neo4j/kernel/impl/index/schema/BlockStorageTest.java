/*
 * Copyright (c) 2002-2019 "Neo4j,"
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
package org.neo4j.kernel.impl.index.schema;

import org.apache.commons.lang3.mutable.MutableLong;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.index.internal.gbptree.SimpleLongLayout;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.test.extension.Inject;
import org.neo4j.test.extension.RandomExtension;
import org.neo4j.test.extension.TestDirectoryExtension;
import org.neo4j.test.rule.RandomRule;
import org.neo4j.test.rule.TestDirectory;

import static java.lang.Math.toIntExact;
import static java.nio.ByteBuffer.allocate;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparingLong;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.neo4j.kernel.impl.index.schema.BlockStorage.Monitor.NO_MONITOR;

@ExtendWith( {TestDirectoryExtension.class, RandomExtension.class} )
class BlockStorageTest
{
    private static final ByteBufferFactory BUFFER_FACTORY = bufferSize -> allocate( toIntExact( bufferSize ) );

    @Inject
    TestDirectory directory;
    @Inject
    RandomRule random;

    private File file;
    private FileSystemAbstraction fileSystem;
    private SimpleLongLayout layout;

    @BeforeEach
    void setup()
    {
        file = directory.file( "block" );
        fileSystem = directory.getFileSystem();
        layout = SimpleLongLayout.longLayout()
                .withFixedSize( random.nextBoolean() )
                .withKeyPadding( random.nextInt( 10 ) )
                .build();
    }

    @Test
    void shouldCreateAndCloseTheBlockFile() throws IOException
    {
        // given
        assertFalse( fileSystem.fileExists( file ) );
        try ( BlockStorage<MutableLong,MutableLong> ignored = new BlockStorage<>( layout, BUFFER_FACTORY, fileSystem, file, NO_MONITOR, 100 ) )
        {
            // then
            assertTrue( fileSystem.fileExists( file ) );
        }
    }

    @Test
    void shouldAddSingleEntryInLastBlock() throws IOException
    {
        // given
        TrackingMonitor monitor = new TrackingMonitor();
        int blockSize = 100;
        MutableLong key = new MutableLong( 10 );
        MutableLong value = new MutableLong( 20 );
        try ( BlockStorage<MutableLong,MutableLong> storage = new BlockStorage<>( layout, BUFFER_FACTORY, fileSystem, file, monitor, blockSize ) )
        {
            // when
            storage.add( key, value );
            storage.doneAdding();

            // then
            assertEquals( 1, monitor.blockFlushedCallCount );
            assertEquals( 1, monitor.lastKeyCount );
            assertEquals( BlockStorage.BLOCK_HEADER_SIZE + monitor.totalEntrySize, monitor.lastNumberOfBytes );
            assertEquals( blockSize, monitor.lastPositionAfterFlush );
            assertThat( monitor.lastNumberOfBytes, lessThan( blockSize ) );
            assertContents( layout, storage, singletonList( singletonList( new BlockEntry<>( key, value ) ) ) );
        }
    }

    @Test
    void shouldSortAndAddMultipleEntriesInLastBlock() throws IOException
    {
        // given
        TrackingMonitor monitor = new TrackingMonitor();
        int blockSize = 1_000;
        List<BlockEntry<MutableLong,MutableLong>> expected = new ArrayList<>();
        try ( BlockStorage<MutableLong,MutableLong> storage = new BlockStorage<>( layout, BUFFER_FACTORY, fileSystem, file, monitor, blockSize ) )
        {
            // when
            for ( int i = 0; i < 10; i++ )
            {
                long keyNumber = random.nextLong( 10_000_000 );
                MutableLong key = new MutableLong( keyNumber );
                MutableLong value = new MutableLong( i );
                storage.add( key, value );
                expected.add( new BlockEntry<>( key, value ) );
            }
            storage.doneAdding();

            // then
            sort( expected );
            assertContents( layout, storage, singletonList( expected ) );
        }
    }

    @Test
    void shouldSortAndAddMultipleEntriesInMultipleBlocks() throws IOException
    {
        // given
        TrackingMonitor monitor = new TrackingMonitor();
        int blockSize = 1_000;
        try ( BlockStorage<MutableLong,MutableLong> storage = new BlockStorage<>( layout, BUFFER_FACTORY, fileSystem, file, monitor, blockSize ) )
        {
            // when
            List<List<BlockEntry<MutableLong,MutableLong>>> expectedBlocks = addACoupleOfBlocksOfEntries( monitor, storage, 3 );

            // then
            assertContents( layout, storage, expectedBlocks );
        }
    }

    @Test
    void shouldMergeWhenEmpty() throws IOException
    {
        // given
        TrackingMonitor monitor = new TrackingMonitor();
        int blockSize = 1_000;
        try ( BlockStorage<MutableLong,MutableLong> storage = new BlockStorage<>( layout, BUFFER_FACTORY, fileSystem, file, monitor, blockSize ) )
        {
            // when
            storage.merge( randomMergeFactor() );

            // then
            assertEquals( 0, monitor.mergeIterationCallCount );
            assertContents( layout, storage, emptyList() );
        }
    }

    @Test
    void shouldMergeSingleBlock() throws IOException
    {
        // given
        TrackingMonitor monitor = new TrackingMonitor();
        int blockSize = 1_000;
        try ( BlockStorage<MutableLong,MutableLong> storage = new BlockStorage<>( layout, BUFFER_FACTORY, fileSystem, file, monitor, blockSize ) )
        {
            List<List<BlockEntry<MutableLong,MutableLong>>> expectedBlocks = singletonList( addEntries( storage, 4 ) );
            storage.doneAdding();

            // when
            storage.merge( randomMergeFactor() );

            // then
            assertEquals( 0, monitor.mergeIterationCallCount );
            assertContents( layout, storage, expectedBlocks );
        }
    }

    @Test
    void shouldMergeMultipleBlocks() throws IOException
    {
        // given
        TrackingMonitor monitor = new TrackingMonitor();
        int blockSize = 1_000;
        try ( BlockStorage<MutableLong,MutableLong> storage = new BlockStorage<>( layout, BUFFER_FACTORY, fileSystem, file, monitor, blockSize ) )
        {
            int numberOfBlocks = random.nextInt( 100 ) + 2;
            List<List<BlockEntry<MutableLong,MutableLong>>> expectedBlocks = addACoupleOfBlocksOfEntries( monitor, storage, numberOfBlocks );
            storage.doneAdding();

            // when
            storage.merge( randomMergeFactor() );

            // then
            assertContents( layout, storage, asOneBigBlock( expectedBlocks ) );
        }
    }

    // todo shouldOnlyLeaveSingleFileAfterMerge
//    @Test
//    void shouldOnlyLeaveSingleFileAfterMerge() throws IOException
//    {
//        TrackingMonitor monitor = new TrackingMonitor();
//        int blockSize = 1_000;
//        try ( BlockStorage<MutableLong,MutableLong> storage = new BlockStorage<>( layout, BUFFER_FACTORY, fileSystem, file, monitor, blockSize ) )
//        {
//            int numberOfBlocks = random.nextInt( 100 ) + 2;
//            List<List<BlockEntry<MutableLong,MutableLong>>> expectedBlocks = addACoupleOfBlocksOfEntries( monitor, storage, numberOfBlocks );
//            storage.doneAdding();
//
//            // when
//            storage.merge();
//
//            // then
//            // todo continue here
//            File[] files = fileSystem.listFiles( directory.directory() );
//            Arrays.stream( files ).forEach( f -> System.out.println( f.getName() ) );
//        }
//    }

    @Test
    void shouldNotAcceptAddedEntriesAfterDoneAdding() throws IOException
    {
        // given
        try ( BlockStorage<MutableLong,MutableLong> storage = new BlockStorage<>( layout, BUFFER_FACTORY, fileSystem, file, NO_MONITOR, 100 ) )
        {
            // when
            storage.doneAdding();

            // then
            assertThrows( IllegalStateException.class, () -> storage.add( new MutableLong( 0 ), new MutableLong( 1 ) ) );
        }
    }
    @Test
    void shouldNotFlushAnythingOnEmptyBufferInDoneAdding() throws IOException
    {
        // given
        TrackingMonitor monitor = new TrackingMonitor();
        try ( BlockStorage<MutableLong,MutableLong> storage = new BlockStorage<>( layout, BUFFER_FACTORY, fileSystem, file, monitor, 100 ) )
        {
            // when
            storage.doneAdding();

            // then
            assertEquals( 0, monitor.blockFlushedCallCount );
        }
    }

    private Iterable<List<BlockEntry<MutableLong,MutableLong>>> asOneBigBlock( List<List<BlockEntry<MutableLong,MutableLong>>> expectedBlocks )
    {
        List<BlockEntry<MutableLong,MutableLong>> all = new ArrayList<>();
        for ( List<BlockEntry<MutableLong,MutableLong>> expectedBlock : expectedBlocks )
        {
            all.addAll( expectedBlock );
        }
        sort( all );
        return singletonList( all );
    }

    private int randomMergeFactor()
    {
        return random.nextInt( 2, 8 );
    }

    private List<BlockEntry<MutableLong,MutableLong>> addEntries( BlockStorage<MutableLong,MutableLong> storage, int numberOfEntries ) throws IOException
    {
        MutableLongSet uniqueKeys = LongSets.mutable.empty();
        List<BlockEntry<MutableLong,MutableLong>> entries = new ArrayList<>();
        for ( int i = 0; i < numberOfEntries; i++ )
        {
            MutableLong key = uniqueKey( uniqueKeys );
            MutableLong value = new MutableLong( random.nextLong( 10_000_000 ) );
            storage.add( key, value );
            entries.add( new BlockEntry<>( key, value ) );
        }
        sort( entries );
        return entries;
    }

    private List<List<BlockEntry<MutableLong,MutableLong>>> addACoupleOfBlocksOfEntries( TrackingMonitor monitor,
            BlockStorage<MutableLong,MutableLong> storage, int numberOfBlocks ) throws IOException
    {
        assert numberOfBlocks != 1;

        MutableLongSet uniqueKeys = LongSets.mutable.empty();
        List<List<BlockEntry<MutableLong,MutableLong>>> expected = new ArrayList<>();
        List<BlockEntry<MutableLong,MutableLong>> currentExpected = new ArrayList<>();
        long currentBlock = 0;
        while ( monitor.blockFlushedCallCount < numberOfBlocks - 1 )
        {
            MutableLong key = uniqueKey( uniqueKeys );
            MutableLong value = new MutableLong( random.nextLong( 10_000_000) );

            storage.add( key, value );
            if ( monitor.blockFlushedCallCount > currentBlock )
            {
                sort( currentExpected );
                expected.add( currentExpected );
                currentExpected = new ArrayList<>();
                currentBlock = monitor.blockFlushedCallCount;
            }
            currentExpected.add( new BlockEntry<>( key, value ) );
        }
        storage.doneAdding();
        if ( !currentExpected.isEmpty() )
        {
            expected.add( currentExpected );
        }
        return expected;
    }

    private MutableLong uniqueKey( MutableLongSet uniqueKeys )
    {
        MutableLong key;
        do
        {
            key = new MutableLong( random.nextLong( 10_000_000 ) );
        }
        while ( !uniqueKeys.add( key.longValue() ) );
        return key;
    }

    private void sort( List<BlockEntry<MutableLong,MutableLong>> entries )
    {
        entries.sort( comparingLong( p -> p.key().longValue() ) );
    }

    private void assertContents( SimpleLongLayout layout, BlockStorage<MutableLong,MutableLong> storage,
            Iterable<List<BlockEntry<MutableLong,MutableLong>>> expectedBlocks )
            throws IOException
    {
        try ( BlockReader<MutableLong,MutableLong> reader = storage.reader() )
        {
            for ( List<BlockEntry<MutableLong,MutableLong>> expectedBlock : expectedBlocks )
            {
                try ( BlockEntryReader<MutableLong,MutableLong> block = reader.nextBlock() )
                {
                    assertNotNull( block );
                    assertEquals( expectedBlock.size(), block.entryCount() );
                    for ( BlockEntry<MutableLong,MutableLong> expectedEntry : expectedBlock )
                    {
                        assertTrue( block.next() );
                        assertEquals( 0, layout.compare( expectedEntry.key(), block.key() ) );
                        assertEquals( expectedEntry.value(), block.value() );
                    }
                }
            }
        }
    }

    private static class TrackingMonitor implements BlockStorage.Monitor
    {
        // For entryAdded
        private int entryAddedCallCount;
        private int lastEntrySize;
        private long totalEntrySize;

        // For blockFlushed
        private int blockFlushedCallCount;
        private long lastKeyCount;
        private int lastNumberOfBytes;
        private long lastPositionAfterFlush;

        // For mergeIteration
        private int mergeIterationCallCount;
        private int lastNumberOfBlocksBefore;
        private int lastNumberOfBlocksAfter;

        // For mergeBlocks
        @Override
        public void entryAdded( int entrySize )
        {
            entryAddedCallCount++;
            lastEntrySize = entrySize;
            totalEntrySize += entrySize;
        }

        @Override
        public void blockFlushed( long keyCount, int numberOfBytes, long positionAfterFlush )
        {
            blockFlushedCallCount++;
            lastKeyCount = keyCount;
            lastNumberOfBytes = numberOfBytes;
            lastPositionAfterFlush = positionAfterFlush;
        }

        @Override
        public void mergeIterationFinished( int numberOfBlocksBefore, int numberOfBlocksAfter )
        {
            mergeIterationCallCount++;
            lastNumberOfBlocksBefore = numberOfBlocksBefore;
            lastNumberOfBlocksAfter = numberOfBlocksAfter;
        }

        @Override
        public void mergedBlocks( long resultingBlockSize, long resultingEntryCount, int numberOfBlocks )
        {   // no-op
        }
    }
}