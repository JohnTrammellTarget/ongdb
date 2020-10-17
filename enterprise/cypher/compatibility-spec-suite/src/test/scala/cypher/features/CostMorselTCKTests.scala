/*
 * Copyright (c) 2018-2020 "Graph Foundation,"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */
package cypher.features

import java.util

import cypher.features.ScenarioTestHelper.{createTests, printComputedBlacklist}
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.{Disabled, DynamicTest, TestFactory}
import org.neo4j.test.TestEnterpriseGraphDatabaseFactory

class CostMorselTCKTests extends EnterpriseBaseTCKTests {

  // If you want to only run a specific feature or scenario, go to the BaseTCKTests
  // Note: The 2 test configs were removed from TestConfig.scala in 3.5.2  See: community/cypher/spec-suite-tools/src/test/scala/cypher/features/TestConfig.scala
  case object CostMorselTestConfigSingleThreaded extends TestConfig(Some("cost-morsel.txt"), "CYPHER planner=cost runtime=morsel debug=singlethreaded")

  case object CostMorselTestConfig extends TestConfig(Some("cost-morsel.txt"), "CYPHER planner=cost runtime=morsel")

  @TestFactory
  def runCostMorselSingleThreaded(): util.Collection[DynamicTest] = {
    createTests(scenarios, CostMorselTestConfigSingleThreaded, new TestEnterpriseGraphDatabaseFactory())
  }

  @TestFactory
  def runCostMorsel(): util.Collection[DynamicTest] = {
    createTests(scenarios, CostMorselTestConfig, new TestEnterpriseGraphDatabaseFactory())
  }

  @Disabled
  def generateBlacklistTCKTestCostMorsel(): Unit = {
    printComputedBlacklist(scenarios, CostMorselTestConfig, new TestEnterpriseGraphDatabaseFactory())
    fail("Do not forget to add @Disabled to this method")
  }
}
