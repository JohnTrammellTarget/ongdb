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
package org.neo4j.values.virtual;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.neo4j.values.storable.Values.NO_VALUE;
import static org.neo4j.values.storable.Values.longValue;
import static org.neo4j.values.virtual.VirtualValues.list;

class DropNoValuesListValueTest
{
    @Test
    void shouldFilterList()
    {
        // Given
        ListValue inner = list( NO_VALUE, longValue( 6L ), NO_VALUE,
                                longValue( 8L ), longValue( 9L ), longValue( 11L ), NO_VALUE );

        // When
        ListValue filter = inner.dropNoValues();

        // Then
        ListValue expected = list( longValue( 6L ), longValue( 8L ), longValue( 9L ), longValue( 11L ) );
        assertEquals( filter, expected );
        assertEquals( filter.hashCode(), expected.hashCode() );
        assertArrayEquals( filter.asArray(), expected.asArray() );
    }
}
