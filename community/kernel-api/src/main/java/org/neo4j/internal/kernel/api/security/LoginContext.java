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
package org.neo4j.internal.kernel.api.security;

import java.util.function.ToIntFunction;

/**
 * The LoginContext hold the executing authenticated user (subject).
 * By calling {@link #authorize(ToIntFunction, String)} the user is also authorized, and a full SecurityContext is returned,
 * which can be used to assert user permissions during query execution.
 */
public interface LoginContext
{
    /**
     * Get the authenticated user.
     */
    AuthSubject subject();

    /**
     * Authorize the user and return a SecurityContext.
     *
     * @param propertyIdLookup token lookup, used to compile property level security verification
     * @param dbName the name of the database the user should be authorized against
     * @return the security context
     */
    SecurityContext authorize( ToIntFunction<String> propertyIdLookup, String dbName );

    LoginContext AUTH_DISABLED = new LoginContext()
    {
        @Override
        public AuthSubject subject()
        {
            return AuthSubject.AUTH_DISABLED;
        }

        @Override
        public SecurityContext authorize( ToIntFunction<String> propertyIdLookup, String dbName )
        {
            return SecurityContext.AUTH_DISABLED;
        }
    };
}
