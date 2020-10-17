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
package org.neo4j.kernel.impl.index.schema;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.api.index.IndexDirectoryStructure;

import static org.neo4j.kernel.impl.index.schema.NativeIndexes.deleteIndex;

public class FileSystemIndexDropAction implements IndexDropAction
{
    private final FileSystemAbstraction fs;
    private final IndexDirectoryStructure directoryStructure;

    public FileSystemIndexDropAction( FileSystemAbstraction fs, IndexDirectoryStructure directoryStructure )
    {
        this.fs = fs;
        this.directoryStructure = directoryStructure;
    }

    @Override
    public void drop( long indexId, boolean archiveExistentIndex )
    {
        try
        {
            deleteIndex( fs, directoryStructure, indexId, archiveExistentIndex );
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }
}
