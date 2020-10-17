/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * Copyright (c) 2018-2020 "Graph Foundation,"
 * Graph Foundation, Inc. [https://graphfoundation.org]
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
package org.neo4j.collection.primitive;

import java.util.function.IntFunction;

import static java.util.Objects.requireNonNull;

public interface PrimitiveIntObjectMap<VALUE> extends PrimitiveIntCollection
{
    VALUE put( int key, VALUE value );

    boolean containsKey( int key );

    VALUE get( int key );

    VALUE remove( int key );

    /**
     * Visit the entries of this map, until all have been visited or the visitor returns 'true'.
     */
    <E extends Exception> void visitEntries( PrimitiveIntObjectVisitor<VALUE, E> visitor ) throws E;

    default VALUE computeIfAbsent( int key, IntFunction<VALUE> function )
    {
        requireNonNull( function );
        VALUE value = get( key );
        if ( value == null )
        {
            value = function.apply( key );
            put( key, value );
        }
        return value;
    }
}
