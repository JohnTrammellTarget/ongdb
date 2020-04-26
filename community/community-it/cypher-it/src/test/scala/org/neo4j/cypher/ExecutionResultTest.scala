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
package org.neo4j.cypher

import java.util.regex.Pattern

import org.junit.Assert._
import org.neo4j.cypher.internal.v4_0.util.test_helpers.WindowsStringSafe

import scala.collection.JavaConverters._

class ExecutionResultTest extends ExecutionEngineFunSuite {

  implicit val windowsSafe = WindowsStringSafe

  test("columnOrderIsPreserved") {
    val columns = List("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")

    columns.foreach(createNode)

    val q="match (zero), (one), (two), (three), (four), (five), (six), (seven), (eight), (nine) " +
      "where id(zero) = 0 AND id(one) = 1 AND id(two) = 2 AND id(three) = 3 AND id(four) = 4 AND id(five) = 5 AND id(six) = 6 AND id(seven) = 7 AND id(eight) = 8 AND id(nine) = 9 " +
      "return zero, one, two, three, four, five, six, seven, eight, nine"

    assert(execute(q).columns === columns)

    val regex = "zero.*one.*two.*three.*four.*five.*six.*seven.*eight.*nine"
    val pattern = Pattern.compile(regex)

    val stringDump = graph.withTx(tx => tx.execute(q).resultAsString())
    assertTrue( "Columns did not appear in the expected order: \n" + stringDump, pattern.matcher(stringDump).find() )
  }

  test("correctLabelStatisticsForCreate") {
    val result = execute("create (n:foo:bar)")
    val stats  = result.queryStatistics()

    assert(stats.labelsAdded === 2)
    assert(stats.labelsRemoved === 0)
  }

  test("correctLabelStatisticsForAdd") {
    val n      = createNode()
    val result = execute(s"match (n) where id(n) = ${n.getId} set n:foo:bar")
    val stats  = result.queryStatistics()

    assert(stats.labelsAdded === 2)
    assert(stats.labelsRemoved === 0)
  }

  test("correctLabelStatisticsForRemove") {
    val n      = createNode()
    execute(s"match (n) where id(n) = ${n.getId} set n:foo:bar")
    val result = execute(s"match (n) where id(n) = ${n.getId} remove n:foo:bar")
    val stats  = result.queryStatistics()

    assert(stats.labelsAdded === 0)
    assert(stats.labelsRemoved === 2)
  }

  test("correctLabelStatisticsForAddAndRemove") {
    val n      = createLabeledNode("foo", "bar")
    val result = execute(s"match (n) where id(n) = ${n.getId} set n:baz remove n:foo:bar")
    val stats  = result.queryStatistics()

    assert(stats.labelsAdded === 1)
    assert(stats.labelsRemoved === 2)
  }


  test("correctLabelStatisticsForLabelAddedTwice") {
    val n      = createLabeledNode("foo", "bar")
    val result = execute(s"match (n) where id(n) = ${n.getId} set n:bar:baz")
    val stats  = result.queryStatistics()

    assert(stats.labelsAdded === 1)
    assert(stats.labelsRemoved === 0)
  }

  test("correctLabelStatisticsForRemovalOfUnsetLabel") {
    val n      = createLabeledNode("foo", "bar")
    val result = execute(s"match (n) where id(n) = ${n.getId} remove n:baz:foo")
    val stats  = result.queryStatistics()

    assert(stats.labelsAdded === 0)
    assert(stats.labelsRemoved === 1)
  }

  test("correctIndexStatisticsForIndexAdded") {
    val result = execute("create index on :Person(name)")
    val stats  = result.queryStatistics()

    assert(stats.indexesAdded === 1)
    assert(stats.indexesRemoved === 0)
  }

  test("correctIndexStatisticsForIndexWithNameAdded") {
    val result = execute("create index my_index for (n:Person) on (n.name)")
    val stats  = result.queryStatistics()

    assert(stats.indexesAdded === 1)
    assert(stats.indexesRemoved === 0)
  }

  test("correctConstraintStatisticsForUniquenessConstraintAdded") {
    val result = execute("create constraint on (n:Person) assert n.name is unique")
    val stats  = result.queryStatistics()

    assert(stats.uniqueConstraintsAdded === 1)
    assert(stats.uniqueConstraintsRemoved === 0)
  }

  test("hasNext should not change resultAsString") {
    graph.withTx( tx => {
      val result = tx.execute("UNWIND [1,2,3] AS x RETURN x")
      result.hasNext
      result.resultAsString() should equal(
        """+---+
          || x |
          |+---+
          || 1 |
          || 2 |
          || 3 |
          |+---+
          |3 rows
          |""".stripMargin)
    })
  }

  test("next should change resultAsString") {
    graph.withTx( tx => {
      val result = tx.execute("UNWIND [1,2,3] AS x RETURN x")
      result.next().asScala should equal(Map("x" -> 1))
      result.resultAsString() should equal(
        """+---+
          || x |
          |+---+
          || 2 |
          || 3 |
          |+---+
          |2 rows
          |""".stripMargin)
    })
  }
}
