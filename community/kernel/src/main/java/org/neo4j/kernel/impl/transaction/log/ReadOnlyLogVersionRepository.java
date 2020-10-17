/*
 * Copyright (c) 2018-2020 "Graph Foundation,"
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
package org.neo4j.kernel.impl.transaction.log;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;

import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.io.pagecache.PageCache;
import org.neo4j.kernel.impl.store.MetaDataStore;

public class ReadOnlyLogVersionRepository implements LogVersionRepository
{
    private final long logVersion;
    private volatile boolean incrementVersionCalled;

    public ReadOnlyLogVersionRepository( PageCache pageCache, DatabaseLayout databaseLayout ) throws IOException
    {
        File neoStore = databaseLayout.metadataStore();
        this.logVersion = readLogVersion( pageCache, neoStore );
    }

    @Override
    public long getCurrentLogVersion()
    {
        // We can expect a call to this during shutting down, if we have a LogFile using us.
        // So it's sort of OK.
        if ( incrementVersionCalled )
        {
            throw new IllegalStateException( "Read-only log version repository has observed a call to " +
                    "incrementVersion, which indicates that it's been shut down" );
        }
        return logVersion;
    }

    @Override
    public void setCurrentLogVersion( long version )
    {
        throw new UnsupportedOperationException( "Can't set current log version in read only version repository." );
    }

    @Override
    public long incrementAndGetVersion()
    {   // We can expect a call to this during shutting down, if we have a LogFile using us.
        // So it's sort of OK.
        if ( incrementVersionCalled )
        {
            throw new IllegalStateException( "Read-only log version repository only allows " +
                    "to call incrementVersion once, during shutdown" );
        }
        incrementVersionCalled = true;
        return logVersion;
    }

    private static long readLogVersion( PageCache pageCache, File neoStore ) throws IOException
    {
        try
        {
            return MetaDataStore.getRecord( pageCache, neoStore, MetaDataStore.Position.LOG_VERSION );
        }
        catch ( NoSuchFileException ignore )
        {
            return 0;
        }
    }
}
