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
package org.neo4j.cypher.internal.runtime.interpreted.commands

import org.neo4j.cypher.internal.runtime.interpreted.commands.expressions.Variable
import org.neo4j.cypher.internal.runtime.interpreted.symbols.SymbolTable
import org.neo4j.cypher.internal.v3_6.util.symbols._
import org.neo4j.cypher.internal.v3_6.util.test_helpers.CypherFunSuite

class AllVariablesTest extends CypherFunSuite {
  val x = AllVariables()

  test("nodes") {
    val symbols = getSymbols("n" -> CTNode)

     x.expressions(symbols) should equal(Map("n" -> Variable("n")))
  }

  test("relationships") {
    val symbols = getSymbols("r" -> CTRelationship)

     x.expressions(symbols) should equal(Map("r" -> Variable("r")))
  }

  test("paths") {
    val symbols = getSymbols("p" -> CTPath)

     x.expressions(symbols) should equal(Map("p" -> Variable("p")))
  }

  private def getSymbols(k: (String, CypherType)*) = SymbolTable(k.toMap)
}
