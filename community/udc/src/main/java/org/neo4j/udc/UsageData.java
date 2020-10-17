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
package org.neo4j.udc;

import java.util.concurrent.ConcurrentHashMap;

import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.scheduler.Group;
import org.neo4j.scheduler.JobHandle;
import org.neo4j.scheduler.JobScheduler;

import static java.util.concurrent.TimeUnit.DAYS;

/**
 * An in-memory storage location for usage metadata.
 * Any component is allowed to publish it's usage date here, and it can be any object,
 * including mutable classes. It is up to the usage data publishing code to choose which items from this repository
 * to publish.
 *
 * This service is meant as a diagnostic and informational tool, notably used by UDC.
 */
public class UsageData extends LifecycleAdapter
{
    private final ConcurrentHashMap<UsageDataKey, Object> store = new ConcurrentHashMap<>();
    private final JobScheduler scheduler;
    private JobHandle featureDecayJob;

    public UsageData( JobScheduler scheduler )
    {
        this.scheduler = scheduler;
    }

    public <T> void set( UsageDataKey<T> key, T value )
    {
        store.put( key, value );
    }

    public <T> T get( UsageDataKey<T> key )
    {
        Object o = store.get( key );
        if ( o == null )
        {
            // When items are missing, if there is a default value, we do a get-or-create style operation
            // This allows outside actors to get-or-create rich objects and know they will get the same object out
            // that other threads would use, which is helpful when we store mutable objects
            T value = key.generateDefaultValue();
            if ( value == null )
            {
                return null;
            }

            store.putIfAbsent( key, value );
            return get( key );
        }
        return (T) o;
    }

    @Override
    public void stop()
    {
        if ( featureDecayJob != null )
        {
            featureDecayJob.cancel( false );
        }
    }

    @Override
    public void start()
    {
        featureDecayJob = scheduler.schedule( Group.UDC, get( UsageDataKeys.features )::sweep, 1, DAYS );
    }
}
