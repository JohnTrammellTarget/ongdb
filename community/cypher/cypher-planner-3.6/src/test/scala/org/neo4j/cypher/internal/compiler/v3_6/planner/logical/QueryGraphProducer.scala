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
package org.neo4j.cypher.internal.compiler.v3_6.planner.logical

import org.neo4j.cypher.internal.compiler.v3_6._
import org.neo4j.cypher.internal.compiler.v3_6.phases.LogicalPlanState
import org.neo4j.cypher.internal.compiler.v3_6.planner._
import org.neo4j.cypher.internal.compiler.v3_6.test_helpers.ContextHelper
import org.neo4j.cypher.internal.ir.v3_6.PlannerQuery
import org.neo4j.cypher.internal.planner.v3_6.spi.IDPPlannerName
import org.neo4j.cypher.internal.planner.v3_6.spi.PlanningAttributes
import org.neo4j.cypher.internal.v3_6.ast.semantics.SemanticCheckResult
import org.neo4j.cypher.internal.v3_6.ast.semantics.SemanticChecker
import org.neo4j.cypher.internal.v3_6.ast.semantics.SemanticTable
import org.neo4j.cypher.internal.v3_6.ast.Query
import org.neo4j.cypher.internal.v3_6.ast.Statement
import org.neo4j.cypher.internal.v3_6.frontend.phases.CNFNormalizer
import org.neo4j.cypher.internal.v3_6.frontend.phases.LateAstRewriting
import org.neo4j.cypher.internal.v3_6.frontend.phases.Namespacer
import org.neo4j.cypher.internal.v3_6.frontend.phases.rewriteEqualityToInPredicate
import org.neo4j.cypher.internal.v3_6.rewriting.rewriters._
import org.neo4j.cypher.internal.v3_6.util.inSequence
import org.scalatest.mock.MockitoSugar

trait QueryGraphProducer extends MockitoSugar {

  self: LogicalPlanningTestSupport =>

  import org.neo4j.cypher.internal.compiler.v3_6.ast.convert.plannerQuery.StatementConverters._

  def producePlannerQueryForPattern(query: String): (PlannerQuery, SemanticTable) = {
    val q = query + " RETURN 1 AS Result"
    val ast = parser.parse(q)
    val mkException = new SyntaxExceptionCreator(query, Some(pos))
    val cleanedStatement: Statement = ast.endoRewrite(inSequence(normalizeWithAndReturnClauses(mkException)))
    val onError = SyntaxExceptionCreator.throwOnError(mkException)
    val SemanticCheckResult(semanticState, errors) = SemanticChecker.check(cleanedStatement)
    onError(errors)

    val (firstRewriteStep, _, _) = astRewriter.rewrite(query, cleanedStatement, semanticState)
    val state = LogicalPlanState(query, None, IDPPlannerName, PlanningAttributes(new StubSolveds, new StubCardinalities, new StubProvidedOrders), Some(firstRewriteStep), Some(semanticState))
    val context = ContextHelper.create(logicalPlanIdGen = idGen)
    val output = (Namespacer andThen rewriteEqualityToInPredicate andThen CNFNormalizer andThen LateAstRewriting).transform(state, context)

    (toUnionQuery(output.statement().asInstanceOf[Query], output.semanticTable()).queries.head, output.semanticTable())
  }
}
