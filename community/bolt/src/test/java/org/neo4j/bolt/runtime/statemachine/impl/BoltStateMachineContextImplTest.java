/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
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
package org.neo4j.bolt.runtime.statemachine.impl;

import io.netty.channel.Channel;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import org.neo4j.bolt.BoltChannel;
import org.neo4j.bolt.messaging.BoltIOException;
import org.neo4j.bolt.runtime.BoltConnectionFatality;
import org.neo4j.bolt.runtime.BoltProtocolBreachFatality;
import org.neo4j.bolt.runtime.statemachine.BoltStateMachine;
import org.neo4j.bolt.runtime.statemachine.BoltStateMachineSPI;
import org.neo4j.bolt.runtime.statemachine.MutableConnectionState;
import org.neo4j.bolt.runtime.statemachine.StatementProcessor;

<<<<<<< HEAD
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
=======
import static org.assertj.core.api.Assertions.assertThat;
>>>>>>> neo4j/4.1
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.neo4j.bolt.runtime.statemachine.StatementProcessor.EMPTY;
import static org.neo4j.bolt.testing.BoltTestUtil.newTestBoltChannel;

class BoltStateMachineContextImplTest
{
    private static final String DB_NAME = "Molly";
    private static final String DB_NAME_DEFAULT = "Polly";

    @Test
    void shouldHandleFailure() throws BoltConnectionFatality
    {
        BoltStateMachine machine = mock( BoltStateMachine.class );
        BoltStateMachineContextImpl context = newContext( machine, mock( BoltStateMachineSPI.class ) );

        RuntimeException cause = new RuntimeException();
        context.handleFailure( cause, true );

        verify( machine ).handleFailure( cause, true );
    }

    @Test
    void shouldResetMachine() throws BoltConnectionFatality
    {
        BoltStateMachine machine = mock( BoltStateMachine.class );
        BoltStateMachineContextImpl context = newContext( machine, mock( BoltStateMachineSPI.class ) );

        context.resetMachine();

        verify( machine ).reset();
    }

    @Test
    void shouldAllowToSetNewStatementProcessor() throws Throwable
    {
        // Given
        StatementProcessor txStateMachine = mock( StatementProcessor.class );
        // Then we can set tx state machine on a context.
        boltStateMachineContextWithStatementProcessor( txStateMachine, DB_NAME );
    }

    @Test
    void shouldErrorToSetNewStatementProcessorWhilePreviousIsNotReleased() throws Throwable
    {
        // Given a context that has a active tx state machine set.
        StatementProcessor txStateMachine = mock( StatementProcessor.class );
        BoltStateMachineContextImpl context = boltStateMachineContextWithStatementProcessor( txStateMachine, DB_NAME );

        // When & Then
        BoltProtocolBreachFatality error = assertThrows( BoltProtocolBreachFatality.class,
                    () -> context.setCurrentStatementProcessorForDatabase( "Bossi" ) );
        assertThat( error.getMessage() ).contains( "Changing database without closing the previous is forbidden." );
        assertThat( context.connectionState().getStatementProcessor() ).isEqualTo( txStateMachine );
    }

    @Test
    void shouldReturnTheSameStatementProcessorIfDatabaseNameAreTheSame() throws Throwable
    {
        // Given a context that has a active tx state machine set.
        StatementProcessor txStateMachine = mock( StatementProcessor.class );
        BoltStateMachineContextImpl context = boltStateMachineContextWithStatementProcessor( txStateMachine, DB_NAME );
        StatementProcessor molly = context.connectionState().getStatementProcessor();

        // When & Then
        StatementProcessor processor = context.setCurrentStatementProcessorForDatabase( DB_NAME );
        assertThat( processor ).isEqualTo( molly );
    }

    @Test
    void releaseShouldResetStatementProcessorBackToEmpty() throws Throwable
    {
        // Given a context that has a active tx state machine set.
        StatementProcessor txStateMachine = mock( StatementProcessor.class );
        BoltStateMachineContextImpl context = boltStateMachineContextWithStatementProcessor( txStateMachine, DB_NAME );

        // When
        context.releaseStatementProcessor();

        // Then
        assertThat( context.connectionState().getStatementProcessor() ).isEqualTo( EMPTY );
    }

    @Test
    void shouldThreadsNameBeUnchangedIfSwitchedOff() throws Throwable
    {
        // Given a context that has a active tx state machine set.
        StatementProcessor txStateMachine = mock( StatementProcessor.class );
        BoltStateMachineContextImpl context;

        // When thread rename is not switched on then DB name should not be included
        context = boltStateMachineContextWithStatementProcessor( txStateMachine, DB_NAME, false, DB_NAME_DEFAULT );
        assertThat( Thread.currentThread().getName(), not( containsString( DB_NAME ) ) );
        context.releaseStatementProcessor();
        assertThat( Thread.currentThread().getName(), not( containsString( DB_NAME ) ) );
    }

    @Test
    void shouldThreadsNameBeCorrectIfSwitchedOn() throws Throwable
    {
        // Given a context that has a active tx state machine set.
        StatementProcessor txStateMachine = mock( StatementProcessor.class );
        BoltStateMachineContextImpl context;

        // When thread rename is switched on then DB name should be included
        context = boltStateMachineContextWithStatementProcessor( txStateMachine, DB_NAME, true, DB_NAME_DEFAULT );
        assertThat( Thread.currentThread().getName(), containsString( DB_NAME ) );
        context.releaseStatementProcessor();
        assertThat( Thread.currentThread().getName(), not( containsString( DB_NAME ) ) );
    }

    @Test
    void shouldThreadsNameBeCorrectIfSwitchedOnAndDefaultDatabase() throws Throwable
    {
        // Given a context that has a active tx state machine set.
        StatementProcessor txStateMachine = mock( StatementProcessor.class );
        BoltStateMachineContextImpl context;

        // When thread rename is switched on then with default DB name its name should not be included
        context = boltStateMachineContextWithStatementProcessor( txStateMachine, "", true, DB_NAME_DEFAULT );
        assertThat( Thread.currentThread().getName(), containsString( DB_NAME_DEFAULT ) );
        context.releaseStatementProcessor();
        assertThat( Thread.currentThread().getName(), not( containsString( DB_NAME_DEFAULT ) ) );
    }

    private static BoltStateMachineContextImpl boltStateMachineContextWithStatementProcessor( StatementProcessor txStateMachine, String databaseName )

            throws BoltProtocolBreachFatality, BoltIOException
    {
        return boltStateMachineContextWithStatementProcessor(  txStateMachine, databaseName, false, DB_NAME_DEFAULT );
    }

    private static BoltStateMachineContextImpl boltStateMachineContextWithStatementProcessor( StatementProcessor txStateMachine, String databaseName,
                                                                                              boolean renameThreads, String defaultDatabaseName )
            throws BoltProtocolBreachFatality, BoltIOException
    {
        StatementProcessorProvider provider = mock( StatementProcessorProvider.class );
        when( provider.getStatementProcessor( databaseName ) ).thenReturn( txStateMachine );
        when( txStateMachine.databaseName() ).thenReturn( databaseName );

        BoltStateMachineContextImpl context = newContext( mock( BoltStateMachine.class ), mock( BoltStateMachineSPI.class ),
                                                          renameThreads, defaultDatabaseName );
        context.setStatementProcessorProvider( provider );
        assertThat( context.connectionState().getStatementProcessor() ).isEqualTo( EMPTY );

        StatementProcessor processor = context.setCurrentStatementProcessorForDatabase( databaseName );

        assertThat( processor ).isEqualTo( txStateMachine );
        assertThat( context.connectionState().getStatementProcessor() ).isEqualTo( txStateMachine );
        return context;
    }

    private static BoltStateMachineContextImpl newContext( BoltStateMachine machine, BoltStateMachineSPI boltSPI )
    {
<<<<<<< HEAD
        return newContext( machine, boltSPI, false, DB_NAME_DEFAULT );
    }

    private static BoltStateMachineContextImpl newContext( BoltStateMachine machine, BoltStateMachineSPI boltSPI,
                                                           boolean renameThreads, String defaultDatabaseName )
    {
        BoltChannel boltChannel = new BoltChannel( "bolt-1", "bolt", mock( Channel.class ) );
        return new BoltStateMachineContextImpl( machine, boltChannel, boltSPI, new MutableConnectionState(), Clock.systemUTC(),
                                                renameThreads, defaultDatabaseName );
=======
        BoltChannel boltChannel = newTestBoltChannel( mock( Channel.class ) );
        return new BoltStateMachineContextImpl( machine, boltChannel, boltSPI, new MutableConnectionState(), Clock.systemUTC() );
>>>>>>> neo4j/4.1
    }
}
