/*
 * Copyright (c) 2018-2020 "Graph Foundation"
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
package org.neo4j.server.rest.dbms;

import org.apache.commons.lang3.StringUtils;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AuthorizationHeaders
{
    private AuthorizationHeaders()
    {
    }

    /**
     * Extract the encoded username and password from a HTTP Authorization header value.
     */
    public static String[] decode( String authorizationHeader )
    {
        String[] parts = authorizationHeader.trim().split( " " );
        String tokenSegment = parts[parts.length - 1];

        if ( tokenSegment.trim().length() == 0 )
        {
            return null;
        }

        String decoded = decodeBase64( tokenSegment );
        if ( decoded.length() < 1 )
        {
            return null;
        }

        String[] userAndPassword = decoded.split( ":", 2 );
        if ( userAndPassword.length != 2 )
        {
            return null;
        }

        return userAndPassword;
    }

    private static String decodeBase64( String base64 )
    {
        try
        {
            byte[] decodedBytes = Base64.getDecoder().decode( base64 );
            return new String( decodedBytes, UTF_8 );
        }
        catch ( IllegalArgumentException e )
        {
            return StringUtils.EMPTY;
        }
    }
}
