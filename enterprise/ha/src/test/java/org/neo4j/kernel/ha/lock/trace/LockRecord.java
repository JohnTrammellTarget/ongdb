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
package org.neo4j.kernel.ha.lock.trace;

import java.util.Objects;

import org.neo4j.storageengine.api.lock.ResourceType;

public class LockRecord
{
    private boolean exclusive;
    private ResourceType resourceType;
    private long resourceId;

    public static LockRecord of( boolean exclusive, ResourceType resourceType, long resourceId )
    {
        return new LockRecord( exclusive, resourceType, resourceId );
    }

    private LockRecord( boolean exclusive, ResourceType resourceType, long resourceId )
    {
        this.exclusive = exclusive;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        LockRecord that = (LockRecord) o;
        return exclusive == that.exclusive && resourceId == that.resourceId &&
                Objects.equals( resourceType, that.resourceType );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( exclusive, resourceType, resourceId );
    }
}
