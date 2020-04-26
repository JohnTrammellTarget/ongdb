/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB.
 *
 * ONgDB is free software: you can redistribute it and/or modify
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
package org.neo4j.kernel.impl.index.schema.fusion;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.neo4j.annotations.documented.ReporterFactory;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.internal.helpers.collection.BoundedIterable;
import org.neo4j.internal.helpers.collection.Iterables;
import org.neo4j.internal.schema.IndexDescriptor;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.pagecache.IOLimiter;
import org.neo4j.kernel.api.exceptions.index.IndexEntryConflictException;
import org.neo4j.kernel.api.index.IndexAccessor;
import org.neo4j.kernel.api.index.IndexConfigProvider;
import org.neo4j.kernel.api.index.IndexDirectoryStructure;
import org.neo4j.kernel.api.index.IndexReader;
import org.neo4j.kernel.api.index.IndexUpdater;
import org.neo4j.kernel.impl.api.index.IndexUpdateMode;
import org.neo4j.kernel.impl.index.schema.IndexFiles;
import org.neo4j.storageengine.api.NodePropertyAccessor;
import org.neo4j.values.storable.Value;

import static org.neo4j.internal.helpers.collection.Iterators.concatResourceIterators;

class FusionIndexAccessor extends FusionIndexBase<IndexAccessor> implements IndexAccessor
{
    private final IndexDescriptor descriptor;
    private final IndexFiles indexFiles;

    FusionIndexAccessor( SlotSelector slotSelector,
            InstanceSelector<IndexAccessor> instanceSelector,
            IndexDescriptor descriptor,
            FileSystemAbstraction fs,
            IndexDirectoryStructure directoryStructure )
    {
        super( slotSelector, instanceSelector );
        this.descriptor = descriptor;
        this.indexFiles = new IndexFiles.Directory( fs, directoryStructure, descriptor.getId() );
    }

    @Override
    public void drop()
    {
        instanceSelector.forAll( IndexAccessor::drop );
        indexFiles.clear();
    }

    @Override
    public IndexUpdater newUpdater( IndexUpdateMode mode )
    {
        LazyInstanceSelector<IndexUpdater> updaterSelector = new LazyInstanceSelector<>( slot -> instanceSelector.select( slot ).newUpdater( mode ) );
        return new FusionIndexUpdater( slotSelector, updaterSelector );
    }

    @Override
    public void force( IOLimiter ioLimiter )
    {
        instanceSelector.forAll( accessor -> accessor.force( ioLimiter ) );
    }

    @Override
    public void refresh()
    {
        instanceSelector.forAll( IndexAccessor::refresh );
    }

    @Override
    public void close()
    {
        instanceSelector.close( IndexAccessor::close );
    }

    @Override
    public IndexReader newReader()
    {
        LazyInstanceSelector<IndexReader> readerSelector = new LazyInstanceSelector<>( slot -> instanceSelector.select( slot ).newReader() );
        return new FusionIndexReader( slotSelector, readerSelector, descriptor );
    }

    @Override
    public BoundedIterable<Long> newAllEntriesReader( long fromIdInclusive, long toIdExclusive )
    {
        Iterable<BoundedIterable<Long>> entries =
                instanceSelector.transform( indexAccessor -> indexAccessor.newAllEntriesReader( fromIdInclusive, toIdExclusive ) );
        return new BoundedIterable<>()
        {
            @Override
            public long maxCount()
            {
                long sum = 0;
                for ( BoundedIterable entry : entries )
                {
                    long maxCount = entry.maxCount();
                    if ( maxCount == UNKNOWN_MAX_COUNT )
                    {
                        return UNKNOWN_MAX_COUNT;
                    }
                    sum += maxCount;
                }
                return sum;
            }

            @Override
            public void close() throws Exception
            {
                forAll( BoundedIterable::close, entries );
            }

            @Override
            public Iterator<Long> iterator()
            {
                return Iterables.concat( entries ).iterator();
            }
        };
    }

    @Override
    public ResourceIterator<File> snapshotFiles()
    {
        return concatResourceIterators( instanceSelector.transform( IndexAccessor::snapshotFiles ).iterator() );
    }

    @Override
    public Map<String,Value> indexConfig()
    {
        Map<String,Value> indexConfig = new HashMap<>();
        instanceSelector.transform( IndexAccessor::indexConfig ).forEach( source -> IndexConfigProvider.putAllNoOverwrite( indexConfig, source ) );
        return indexConfig;
    }

    @Override
    public void verifyDeferredConstraints( NodePropertyAccessor nodePropertyAccessor ) throws IndexEntryConflictException
    {
        for ( IndexSlot slot : IndexSlot.values() )
        {
            instanceSelector.select( slot ).verifyDeferredConstraints( nodePropertyAccessor );
        }
    }

    @Override
    public boolean isDirty()
    {
        return Iterables.stream( instanceSelector.transform( IndexAccessor::isDirty ) ).anyMatch( Boolean::booleanValue );
    }

    @Override
    public void validateBeforeCommit( Value[] tuple )
    {
        instanceSelector.select( slotSelector.selectSlot( tuple, CATEGORY_OF ) ).validateBeforeCommit( tuple );
    }

    @Override
    public boolean consistencyCheck( ReporterFactory reporterFactory )
    {
        return FusionIndexBase.consistencyCheck( instanceSelector.instances.values(), reporterFactory );
    }

    @Override
    public long estimateNumberOfEntries()
    {
        List<Long> counts = instanceSelector.transform( IndexAccessor::estimateNumberOfEntries );
        return counts.stream().anyMatch( count -> count == UNKNOWN_NUMBER_OF_ENTRIES )
               ? UNKNOWN_NUMBER_OF_ENTRIES
               : counts.stream().mapToLong( Long::longValue ).sum();
    }
}
