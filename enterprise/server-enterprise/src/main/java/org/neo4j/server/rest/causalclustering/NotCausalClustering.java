/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) as found
 * in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */
package org.neo4j.server.rest.causalclustering;

import javax.ws.rs.core.Response;

import org.neo4j.server.rest.repr.OutputFormat;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.status;

class NotCausalClustering extends BaseStatus
{
    NotCausalClustering( OutputFormat output )
    {
        super( output );
    }

    @Override
    public Response discover()
    {
        return status( FORBIDDEN ).build();
    }

    @Override
    public Response available()
    {
        return status( FORBIDDEN ).build();
    }

    @Override
    public Response readonly()
    {
        return status( FORBIDDEN ).build();
    }

    @Override
    public Response writable()
    {
        return status( FORBIDDEN ).build();
    }

    @Override
    public Response description()
    {
        return Response.status( FORBIDDEN ).build();
    }
}
