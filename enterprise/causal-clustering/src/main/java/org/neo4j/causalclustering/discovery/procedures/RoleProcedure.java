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
package org.neo4j.causalclustering.discovery.procedures;

import org.neo4j.causalclustering.discovery.RoleInfo;
import org.neo4j.collection.RawIterator;
import org.neo4j.internal.kernel.api.exceptions.ProcedureException;
import org.neo4j.internal.kernel.api.procs.Neo4jTypes;
import org.neo4j.internal.kernel.api.procs.QualifiedName;
import org.neo4j.kernel.api.ResourceTracker;
import org.neo4j.kernel.api.proc.CallableProcedure;
import org.neo4j.kernel.api.proc.Context;

import static org.neo4j.internal.kernel.api.procs.ProcedureSignature.procedureSignature;

abstract class RoleProcedure extends CallableProcedure.BasicProcedure
{
    private static final String PROCEDURE_NAME = "role";
    private static final String[] PROCEDURE_NAMESPACE = {"dbms", "cluster"};
    private static final String OUTPUT_NAME = "role";

    RoleProcedure()
    {
        super( procedureSignature( new QualifiedName( PROCEDURE_NAMESPACE, PROCEDURE_NAME ) )
                .out( OUTPUT_NAME, Neo4jTypes.NTString )
                .description( "The role of a specific instance in the cluster." )
                .build() );
    }

    @Override
    public RawIterator<Object[],ProcedureException> apply(
            Context ctx, Object[] input, ResourceTracker resourceTracker )
    {
        return RawIterator.<Object[],ProcedureException>of( new Object[]{role().name()} );
    }

    abstract RoleInfo role();
}
