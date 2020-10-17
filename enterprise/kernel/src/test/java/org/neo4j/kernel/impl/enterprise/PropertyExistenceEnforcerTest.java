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
package org.neo4j.kernel.impl.enterprise;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.neo4j.internal.kernel.api.schema.constraints.ConstraintDescriptor;
import org.neo4j.kernel.api.schema.constraints.ConstraintDescriptorFactory;
import org.neo4j.kernel.api.schema.constraints.NodeKeyConstraintDescriptor;
import org.neo4j.kernel.api.schema.constraints.RelExistenceConstraintDescriptor;
import org.neo4j.kernel.api.schema.constraints.UniquenessConstraintDescriptor;
import org.neo4j.storageengine.api.StorageReader;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PropertyExistenceEnforcerTest
{
    @Test
    public void constraintPropertyIdsNotUpdatedByConstraintEnforcer()
    {
        UniquenessConstraintDescriptor uniquenessConstraint = ConstraintDescriptorFactory.uniqueForLabel( 1, 1, 70, 8 );
        NodeKeyConstraintDescriptor nodeKeyConstraint = ConstraintDescriptorFactory.nodeKeyForLabel( 2, 12, 7, 13 );
        RelExistenceConstraintDescriptor relTypeConstraint =
                ConstraintDescriptorFactory.existsForRelType( 3, 5, 13, 8 );
        List<ConstraintDescriptor> descriptors =
                Arrays.asList( uniquenessConstraint, nodeKeyConstraint, relTypeConstraint );

        StorageReader storageReader = prepareStorageReaderMock( descriptors );

        PropertyExistenceEnforcer.getOrCreatePropertyExistenceEnforcerFrom( storageReader );

        assertArrayEquals( "Property ids should remain untouched.", new int[]{1, 70, 8},
                uniquenessConstraint.schema().getPropertyIds() );
        assertArrayEquals( "Property ids should remain untouched.", new int[]{12, 7, 13},
                nodeKeyConstraint.schema().getPropertyIds() );
        assertArrayEquals( "Property ids should remain untouched.", new int[]{5, 13, 8},
                relTypeConstraint.schema().getPropertyIds() );
    }

    @SuppressWarnings( "unchecked" )
    private StorageReader prepareStorageReaderMock( List<ConstraintDescriptor> descriptors )
    {
        StorageReader storageReader = Mockito.mock( StorageReader.class );
        when( storageReader.constraintsGetAll() ).thenReturn( descriptors.iterator() );
        when( storageReader.getOrCreateSchemaDependantState( eq( PropertyExistenceEnforcer.class ),
                any( Function.class) ) ).thenAnswer( invocation ->
        {
            Function<StorageReader,PropertyExistenceEnforcer> function = invocation.getArgument( 1 );
            return function.apply( storageReader );
        } );
        return storageReader;
    }
}
