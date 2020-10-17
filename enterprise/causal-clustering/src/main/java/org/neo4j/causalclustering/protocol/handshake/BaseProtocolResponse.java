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
package org.neo4j.causalclustering.protocol.handshake;

import java.util.Objects;

public abstract class BaseProtocolResponse<IMPL extends Comparable<IMPL>> implements ClientMessage
{
    private final StatusCode statusCode;
    private final String protocolName;
    private final IMPL version;

    BaseProtocolResponse( StatusCode statusCode, String protocolName, IMPL version )
    {
        this.statusCode = statusCode;
        this.protocolName = protocolName;
        this.version = version;
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
        BaseProtocolResponse that = (BaseProtocolResponse) o;
        return Objects.equals( version, that.version ) && Objects.equals( protocolName, that.protocolName );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( protocolName, version );
    }

    public StatusCode statusCode()
    {
        return statusCode;
    }

    public String protocolName()
    {
        return protocolName;
    }

    public IMPL version()
    {
        return version;
    }

    @Override
    public String toString()
    {
        return "BaseProtocolResponse{" + "statusCode=" + statusCode + ", protocolName='" + protocolName + '\'' + ", version=" + version + '}';
    }
}
