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
package org.neo4j.causalclustering.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.neo4j.function.ThrowingConsumer;

public class CompositeSuspendable implements Suspendable
{
    private final List<Suspendable> suspendables = new ArrayList<>();

    public void add( Suspendable suspendable )
    {
        suspendables.add( suspendable );
    }

    @Override
    public void enable()
    {
        doOperation( Suspendable::enable, "Enable" );
    }

    @Override
    public void disable()
    {
        doOperation( Suspendable::disable, "Disable" );
    }

    private void doOperation( ThrowingConsumer<Suspendable,Throwable> operation, String description )
    {
        ErrorHandler.runAll( description, suspendables.stream()
                .map( (Function<Suspendable,ErrorHandler.ThrowingRunnable>) suspendable -> () -> operation.accept( suspendable ) )
                .toArray( ErrorHandler.ThrowingRunnable[]::new ) );
    }
}
