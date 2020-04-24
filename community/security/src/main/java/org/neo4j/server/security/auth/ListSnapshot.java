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
package org.neo4j.server.security.auth;

import java.util.List;

public class ListSnapshot<T>
{
    private final long timestamp;
    private final List<T> values;
    private final boolean fromPersisted;

    public ListSnapshot( long timestamp, List<T> values, boolean fromPersisted )
    {
        this.timestamp = timestamp;
        this.values = values;
        this.fromPersisted = fromPersisted;
    }

    public long timestamp()
    {
        return timestamp;
    }

    public List<T> values()
    {
        return values;
    }

    public boolean fromPersisted()
    {
        return fromPersisted;
    }

    public static final boolean FROM_PERSISTED = true;
    public static final boolean FROM_MEMORY = false;
}
