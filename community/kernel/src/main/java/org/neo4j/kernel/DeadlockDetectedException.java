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
package org.neo4j.kernel;

import org.neo4j.graphdb.TransientTransactionFailureException;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.util.DocumentationURLs;

/**
 * Signals that a deadlock between two or more transactions has been detected.
 */
public class DeadlockDetectedException extends TransientTransactionFailureException implements Status.HasStatus
{
    public DeadlockDetectedException( String message )
    {
        super( message, null );
    }

    public DeadlockDetectedException( String message, Throwable cause )
    {
        super( "Don't panic.\n" +
                "\n" +
                "A deadlock scenario has been detected and avoided. This means that two or more transactions, which were " +
                "holding locks, were wanting to await locks held by one another, which would have resulted in a deadlock " +
                "between these transactions. This exception was thrown instead of ending up in that deadlock.\n" +
                "\n" +
                "See the deadlock section in the Neo4j Java developer reference for how to avoid this: " +
                DocumentationURLs.TRANSACTION_DEADLOCK + "\n" +
                "\n" +
                "Details: '" + message + "'.", cause );
    }

    @Override
    public Status status()
    {
        return Status.Transaction.DeadlockDetected;
    }
}
