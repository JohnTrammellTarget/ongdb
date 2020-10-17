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
package org.neo4j.server.rest.repr;

import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;

public class RelationshipIndexRootRepresentation extends MappingRepresentation
{
    private IndexManager indexManager;

    public RelationshipIndexRootRepresentation( IndexManager indexManager )
    {
        super( "relationship-index" );
        this.indexManager = indexManager;
    }

    @Override
    protected void serialize( MappingSerializer serializer )
    {
        for ( String indexName : indexManager.relationshipIndexNames() )
        {
            RelationshipIndex index = indexManager.forRelationships( indexName );
            serializer.putMapping( indexName,
                    new RelationshipIndexRepresentation( indexName, indexManager.getConfiguration( index ) ) );
        }
    }
}
