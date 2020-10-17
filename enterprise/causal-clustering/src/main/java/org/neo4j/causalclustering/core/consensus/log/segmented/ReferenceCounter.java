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
package org.neo4j.causalclustering.core.consensus.log.segmented;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keeps a reference count using CAS.
 */
class ReferenceCounter
{
    private static final int DISPOSED_VALUE = -1;

    private AtomicInteger count = new AtomicInteger();

    boolean increase()
    {
        while ( true )
        {
            int pre = count.get();
            if ( pre == DISPOSED_VALUE )
            {
                return false;
            }
            else if ( count.compareAndSet( pre, pre + 1 ) )
            {
                return true;
            }
        }
    }

    void decrease()
    {
        while ( true )
        {
            int pre = count.get();
            if ( pre <= 0 )
            {
                throw new IllegalStateException( "Illegal count: " + pre );
            }
            else if ( count.compareAndSet( pre, pre - 1 ) )
            {
                return;
            }
        }
    }

    /**
     * Idempotently try to dispose this reference counter.
     *
     * @return True if the reference counter was or is now disposed.
     */
    boolean tryDispose()
    {
        return count.get() == DISPOSED_VALUE || count.compareAndSet( 0, DISPOSED_VALUE );
    }

    public int get()
    {
        return count.get();
    }
}
