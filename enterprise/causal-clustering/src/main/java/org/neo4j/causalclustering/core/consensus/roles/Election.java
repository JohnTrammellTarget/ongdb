/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) as found
 * in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */
package org.neo4j.causalclustering.core.consensus.roles;

import java.io.IOException;
import java.util.Set;

import org.neo4j.causalclustering.core.consensus.RaftMessages;
import org.neo4j.causalclustering.core.consensus.outcome.Outcome;
import org.neo4j.causalclustering.core.consensus.state.ReadableRaftState;
import org.neo4j.causalclustering.identity.MemberId;
import org.neo4j.logging.Log;

public class Election
{
    private Election()
    {
    }

    public static boolean startRealElection( ReadableRaftState ctx, Outcome outcome, Log log ) throws IOException
    {
        Set<MemberId> currentMembers = ctx.votingMembers();
        if ( currentMembers == null || !currentMembers.contains( ctx.myself() ) )
        {
            log.info( "Election attempted but not started, current members are %s, I am %s",
                    currentMembers, ctx.myself()  );
            return false;
        }

        outcome.setNextTerm( ctx.term() + 1 );

        RaftMessages.Vote.Request voteForMe =
                new RaftMessages.Vote.Request( ctx.myself(), outcome.getTerm(), ctx.myself(), ctx.entryLog()
                        .appendIndex(), ctx.entryLog().readEntryTerm( ctx.entryLog().appendIndex() ) );

        currentMembers.stream().filter( member -> !member.equals( ctx.myself() ) ).forEach( member ->
            outcome.addOutgoingMessage( new RaftMessages.Directed( member, voteForMe ) )
        );

        outcome.setVotedFor( ctx.myself() );
        log.info( "Election started with vote request: %s and members: %s", voteForMe, currentMembers );
        return true;
    }

    public static boolean startPreElection( ReadableRaftState ctx, Outcome outcome, Log log ) throws IOException
    {
        Set<MemberId> currentMembers = ctx.votingMembers();
        if ( currentMembers == null || !currentMembers.contains( ctx.myself() ) )
        {
            log.info( "Pre-election attempted but not started, current members are %s, I am %s",
                    currentMembers, ctx.myself()  );
            return false;
        }

        RaftMessages.PreVote.Request preVoteForMe =
                new RaftMessages.PreVote.Request( ctx.myself(), outcome.getTerm(), ctx.myself(), ctx.entryLog()
                        .appendIndex(), ctx.entryLog().readEntryTerm( ctx.entryLog().appendIndex() ) );

        currentMembers.stream().filter( member -> !member.equals( ctx.myself() ) ).forEach( member ->
                outcome.addOutgoingMessage( new RaftMessages.Directed( member, preVoteForMe ) )
        );

        log.info( "Pre-election started with: %s and members: %s", preVoteForMe, currentMembers );
        return true;
    }
}
