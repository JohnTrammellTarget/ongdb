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
package org.neo4j.cypher.internal.runtime.vectorized

import org.neo4j.cypher.internal.runtime.QueryContext
import org.neo4j.cypher.internal.runtime.parallel.{Scheduler, SchedulerTracer, SingleThreadScheduler}
import org.neo4j.cypher.result.QueryResult.QueryResultVisitor
import org.neo4j.values.virtual.MapValue

class Dispatcher(morselSize: Int, scheduler: Scheduler) {

  def execute[E <: Exception](operators: Pipeline,
                              queryContext: QueryContext,
                              params: MapValue,
                              schedulerTracer: SchedulerTracer)
                             (visitor: QueryResultVisitor[E]): Unit = {
    val leaf = getLeaf(operators)

    val state = QueryState(params, visitor, morselSize, singeThreaded = scheduler.isInstanceOf[SingleThreadScheduler])
    val initialTask = leaf.init(MorselExecutionContext.EMPTY, queryContext, state)
    val queryExecution = scheduler.execute(initialTask, schedulerTracer)
    val maybeError = queryExecution.await()
    if (maybeError.isDefined)
      throw maybeError.get
  }

  private def getLeaf(pipeline: Pipeline): StreamingPipeline = {
    var leafOp = pipeline
    while (leafOp.upstream.nonEmpty) {
      leafOp = leafOp.upstream.get
    }

    leafOp.asInstanceOf[StreamingPipeline]
  }
}
