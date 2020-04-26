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
package org.neo4j.cypher.internal.runtime.interpreted.pipes.aggregation

import org.neo4j.cypher.internal.runtime.IsNoValue
import org.neo4j.cypher.internal.runtime.interpreted.commands.expressions.Expression
import org.neo4j.exceptions.CypherTypeException
import org.neo4j.values.AnyValue
import org.neo4j.values.storable.NumberValue

trait NumericExpressionOnly {
  def name: String

  def value: Expression

  def actOnNumber[U](obj: AnyValue, f: NumberValue => U) {
    obj match {
      case IsNoValue() =>
      case number: NumberValue => f(number)
      case _ =>
        throw new CypherTypeException("%s(%s) can only handle numerical values, or null.".format(name, value))
    }
  }
}
