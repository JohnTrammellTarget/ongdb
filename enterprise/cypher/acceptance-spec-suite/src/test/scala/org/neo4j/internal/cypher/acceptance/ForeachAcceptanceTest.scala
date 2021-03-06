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
package org.neo4j.internal.cypher.acceptance

import org.neo4j.cypher.ExecutionEngineFunSuite
import org.neo4j.cypher.QueryStatisticsTestSupport
import org.neo4j.cypher.SyntaxException
import org.neo4j.internal.cypher.acceptance.comparisonsupport.ComparePlansWithAssertion
import org.neo4j.internal.cypher.acceptance.comparisonsupport.Configs
import org.neo4j.internal.cypher.acceptance.comparisonsupport.CypherComparisonSupport

class ForeachAcceptanceTest extends ExecutionEngineFunSuite with CypherComparisonSupport with QueryStatisticsTestSupport {

  test("should understand symbols introduced by FOREACH") {
    createLabeledNode("Label")
    createLabeledNode("Label")
    createLabeledNode("Label2")
    val query =
      """MATCH (a:Label)
        |WITH collect(a) as nodes
        |MATCH (b:Label2)
        |FOREACH(n in nodes |
        |  CREATE (n)-[:SELF]->(b))""".stripMargin

    // should work
    executeOfficial(query)
  }

  test("nested foreach") {
    // given
    createLabeledNode("Root")

    // when
    val query =
      """MATCH (r:Root)
        |FOREACH (i IN range(1, 10) |
        | CREATE (r)-[:PARENT]->(c:Child { id:i })
        | FOREACH (j IN range(1, 10) |
        |   CREATE (c)-[:PARENT]->(:Child { id: c.id * 10 + j })
        | )
        |)""".stripMargin

    val result = executeWith(Configs.InterpretedAndSlotted - Configs.Cost2_3, query)

    // then
    assertStats(result, nodesCreated = 110, relationshipsCreated = 110, propertiesWritten = 110, labelsAdded = 110)
    val rows = executeScalar[Number]("MATCH (:Root)-[:PARENT]->(:Child) RETURN count(*)")
    rows should equal(10)
    val ids = executeWith(Configs.InterpretedAndSlotted, "MATCH (:Root)-[:PARENT*]->(c:Child) RETURN c.id AS id ORDER BY c.id").toList
    ids should equal((1 to 110).map(i => Map("id" -> i)))
  }

  test("foreach should return no results") {
    // given
    val query = "FOREACH( n in range( 0, 1 ) | CREATE (p:Person) )"

    // when
    val result = executeWith(Configs.InterpretedAndSlotted - Configs.Cost2_3, query,
      planComparisonStrategy = ComparePlansWithAssertion(plan => {
        //THEN
        plan should includeSomewhere.aPlan("Foreach").withRHS(aPlan("Create"))
      }, Configs.RulePlanner + Configs.Version3_1))

    // then
    assertStats(result, nodesCreated = 2, labelsAdded = 2)
    result shouldBe empty
  }

  test("foreach should not expose inner variables") {
    val query =
      """MATCH (n)
        |FOREACH (i IN [0, 1, 2]
        |  CREATE (m)
        |)
        |SET m.prop = 0
      """.stripMargin

    a[SyntaxException] should be thrownBy executeWith(Configs.Empty, query)
  }

  test("foreach should let you use inner variables from create relationship patterns") {
    // given
    val query =
      """FOREACH (x in [1] |
        |CREATE (e:Event)-[i:IN]->(p:Place)
        |SET e.foo='e_bar'
        |SET i.foo='i_bar'
        |SET p.foo='p_bar')
        |WITH 0 as dummy
        |MATCH (e:Event)-[i:IN]->(p:Place)
        |RETURN e.foo, i.foo, p.foo""".stripMargin

    // when
    val result = executeWith(Configs.InterpretedAndSlotted - Configs.Cost2_3, query)

    // then
    assertStats(result, nodesCreated = 2, relationshipsCreated = 1, labelsAdded = 2, propertiesWritten = 3)
    val resultList = result.toList
    resultList.head.get("e.foo") should equal(Some("e_bar"))
    resultList.head.get("i.foo") should equal(Some("i_bar"))
    resultList.head.get("p.foo") should equal(Some("p_bar"))
  }

  test("Foreach and delete should work together without breaking on unknown identifier types") {
    // given
    val node = createLabeledNode("Label")
    relate(node, createNode())

    val query =
      """MATCH (n: Label)
        |OPTIONAL MATCH (n)-[rel]->()
        |FOREACH (r IN CASE WHEN rel IS NOT NULL THEN [rel] ELSE [] END | DELETE r )""".stripMargin

    // when
    val result = executeWith(Configs.InterpretedAndSlotted - Configs.Cost2_3, query)

    // then
    assertStats(result, relationshipsDeleted = 1)
  }

  test("merge inside foreach in compatibility mode should work nicely") {
    // given

    val query =
      """|FOREACH(v IN [1] |
         |  CREATE (a), (b)
         |  MERGE (a)-[:FOO]->(b))""".stripMargin

    // when
    val result = executeWith(Configs.InterpretedAndSlotted - Configs.Cost2_3, query)

    // then
    assertStats(result, nodesCreated = 2, relationshipsCreated = 1)
  }

  test("foreach with non-trivially typed collection and create pattern should not create bound node") {
    // given
    val query =
      """CREATE (a),(b)
        |WITH a, collect(b) as nodes, true as condition
        |FOREACH (x IN CASE WHEN condition THEN nodes ELSE [] END | CREATE (a)-[:X]->(x) );""".stripMargin

    // when
    val result = executeWith(Configs.InterpretedAndSlotted - Configs.Cost2_3, query)

    // then
    assertStats(result, nodesCreated = 2, relationshipsCreated = 1)
  }

  test("foreach with non-trivially typed collection and merge pattern should not create bound node") {
    // given
    createLabeledNode("Foo")
    createLabeledNode("Bar")

    val query =
      """MATCH (n:Foo),(m:Bar)
        |FOREACH (x IN CASE WHEN true THEN [n] ELSE [] END |
        |   MERGE (x)-[:FOOBAR]->(m) );""".stripMargin

    // when
    val result = executeWith(Configs.InterpretedAndSlotted - Configs.Cost2_3, query)

    // then
    assertStats(result, relationshipsCreated = 1)
  }

  test("foreach with mixed type collection should not plan create of bound node and fail at runtime") {
    // given
    createLabeledNode("Foo")
    createLabeledNode("Bar")

    val query =
      """MATCH (n:Foo),(m:Bar)
        |WITH n, [m, 42] as mixedTypeCollection
        |FOREACH (x IN mixedTypeCollection | CREATE (n)-[:FOOBAR]->(x) );""".stripMargin

    // when
    val explain = executeWith(Configs.InterpretedAndSlotted - Configs.Version2_3, s"EXPLAIN $query")

    // then
    explain.executionPlanDescription().toString shouldNot include("CreateNode")

    // when
    val config = Configs.InterpretedAndSlotted - Configs.Version2_3 - Configs.Rule3_1
    failWithError(config, query, List("Expected to find a node at"))
  }

  test("foreach with inner function invocation should work ") {
    // given
    val a = createNode()
    val b = createNode()
    relate(a, b)

    val query =
      """MATCH p = ()-->()
        |FOREACH (n IN nodes(p) | SET n.marked = true)""".stripMargin
    val result = executeWith(Configs.InterpretedAndSlotted - Configs.Cost2_3, query)
    assertStats(result, propertiesWritten = 2)
  }

  test("should handle building FOREACH on pattern comprehension") {
    graph.execute("CREATE (:X)-[:T]->(), (:X)")

    val result = executeWith(
      expectSucceed = Configs.InterpretedAndSlotted /\ Configs.Version3_5,
      query = "FOREACH (x in  [ (n:X)-->() | n ] | SET x.prop = 12 )")
    assertStats(result, propertiesWritten = 1)
  }
}
