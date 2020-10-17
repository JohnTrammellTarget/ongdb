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
package org.neo4j.tooling.procedure.procedures.invalid.bad_context_field;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.UserAggregationFunction;
import org.neo4j.procedure.UserAggregationResult;
import org.neo4j.procedure.UserAggregationUpdate;
import org.neo4j.procedure.UserFunction;

public class BadContextFields
{

    @Context
    public static GraphDatabaseService shouldBeNonStatic;
    public static String value;
    @Context
    public final GraphDatabaseService shouldBeNonFinal = null;
    @Context
    public GraphDatabaseService db;
    @Context
    protected GraphDatabaseService shouldBePublic;
    String shouldBeStatic;

    @Procedure
    public void sproc1()
    {
    }

    @Procedure
    public void sproc2()
    {
    }

    @UserFunction
    public Long function()
    {
        return 42L;
    }

    @UserAggregationFunction
    public MyAggregation aggregation()
    {
        return new MyAggregation();
    }

    public static class MyAggregation
    {
        @UserAggregationResult
        public Long result()
        {
            return 42L;
        }

        @UserAggregationUpdate
        public void woot( @Name( "undostres" ) String onetwothree )
        {

        }
    }
}
