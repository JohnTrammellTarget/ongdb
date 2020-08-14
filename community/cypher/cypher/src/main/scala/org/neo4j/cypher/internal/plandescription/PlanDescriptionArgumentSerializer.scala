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
package org.neo4j.cypher.internal.plandescription

import org.neo4j.cypher.internal.plandescription.Arguments.ByteCode
import org.neo4j.cypher.internal.plandescription.Arguments.DbHits
import org.neo4j.cypher.internal.plandescription.Arguments.Details
import org.neo4j.cypher.internal.plandescription.Arguments.EstimatedRows
import org.neo4j.cypher.internal.plandescription.Arguments.GlobalMemory
import org.neo4j.cypher.internal.plandescription.Arguments.Memory
import org.neo4j.cypher.internal.plandescription.Arguments.Order
import org.neo4j.cypher.internal.plandescription.Arguments.PageCacheHitRatio
import org.neo4j.cypher.internal.plandescription.Arguments.PageCacheHits
import org.neo4j.cypher.internal.plandescription.Arguments.PageCacheMisses
import org.neo4j.cypher.internal.plandescription.Arguments.PipelineInfo
import org.neo4j.cypher.internal.plandescription.Arguments.Planner
import org.neo4j.cypher.internal.plandescription.Arguments.PlannerImpl
import org.neo4j.cypher.internal.plandescription.Arguments.PlannerVersion
import org.neo4j.cypher.internal.plandescription.Arguments.Rows
import org.neo4j.cypher.internal.plandescription.Arguments.Runtime
import org.neo4j.cypher.internal.plandescription.Arguments.RuntimeImpl
import org.neo4j.cypher.internal.plandescription.Arguments.RuntimeVersion
import org.neo4j.cypher.internal.plandescription.Arguments.SourceCode
import org.neo4j.cypher.internal.plandescription.Arguments.Time
import org.neo4j.cypher.internal.plandescription.Arguments.Version
import org.neo4j.cypher.internal.plandescription.asPrettyString.PrettyStringMaker

object PlanDescriptionArgumentSerializer {

  def serialize(arg: Argument): AnyRef = {
    arg match {
      case Details(info) => info.mkPrettyString(", ").prettifiedString
      case DbHits(value) => Long.box(value)
      case Memory(value) => Long.box(value)
      case GlobalMemory(value) => Long.box(value)
      case PageCacheHits(value) => Long.box(value)
      case PageCacheMisses(value) => Long.box(value)
      case PageCacheHitRatio(value) => Double.box(value)
      case Rows(value) => Long.box(value)
      case Time(value) => Long.box(value)
      case EstimatedRows(value) => Double.box(value)
      case Order(providedOrder) => providedOrder.prettifiedString
      case Version(version) => version
      case Planner(planner) => planner
      case PlannerImpl(plannerName) => plannerName
      case PlannerVersion(value) => value
      case Runtime(runtime) => runtime
      case RuntimeVersion(value) => value
<<<<<<< HEAD
      case DbmsAction(action) => action
      case DatabaseAction(action) => action
      case Database(name) => name
      case Role(name) => name
      case User(name) => name
      case Qualifier(name) => name
      case Scope(name) => name
      case SourceCode(className, sourceCode) => sourceCode
      case ByteCode(className, byteCode) => byteCode
      case RuntimeImpl(runtimeName) => runtimeName
      case ExpandExpression(from, rel, typeNames, to, dir: SemanticDirection, min, max) =>
        val left = if (dir == SemanticDirection.INCOMING) "<-" else "-"
        val right = if (dir == SemanticDirection.OUTGOING) "->" else "-"
        val types = typeNames.mkString(":", "|", "")
        val lengthDescr = (min, max) match {
          case (1, Some(1)) => ""
          case (1, None) => "*"
          case (1, Some(m)) => s"*..$m"
          case _ => s"*$min..${max.getOrElse("")}"
        }
        val relInfo = if (lengthDescr == "" && typeNames.isEmpty && rel.unnamed) "" else s"[$rel$types$lengthDescr]"
        s"($from)$left$relInfo$right($to)"
      case CountNodesExpression(ident, label) =>
        val node = label.map(":" + _).mkString
        s"count( ($node) )" + (if (ident.startsWith(" ")) "" else s" AS $ident")
      case CountRelationshipsExpression(ident, startLabel, typeNames, endLabel) =>
        val start = startLabel.map(l => ":" + l).mkString
        val end = endLabel.map(l => ":" + l).mkString
        val types = typeNames.mkString(":", "|", "")
        s"count( ($start)-[$types]->($end) )" + (if (ident.unnamed) "" else s" AS $ident")
      case Signature(procedureName, args, results) =>
        val argString = args.mkString(", ")
        val resultString = results.map { case (name, typ) => s"$name :: $typ" }.mkString(", ")
        s"$procedureName($argString) :: ($resultString)"
=======
      case SourceCode(className, sourceCode) => sourceCode
      case ByteCode(className, byteCode) => byteCode
      case RuntimeImpl(runtimeName) => runtimeName
      case PipelineInfo(pipelineId, fused) =>
        val fusion = if (fused) "Fused in" else "In"
        s"$fusion Pipeline $pipelineId"
>>>>>>> neo4j/4.1

      // Do not add a fallthrough here - we rely on exhaustive checking to ensure
      // that we don't forget to add new types of arguments here
    }
  }
}
