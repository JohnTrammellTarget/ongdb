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
package org.neo4j.cypher.internal.runtime.compiled.codegen.ir

import org.neo4j.cypher.internal.runtime.compiled.codegen.CodeGenContext
import org.neo4j.cypher.internal.runtime.compiled.codegen.ir.expressions.CodeGenExpression
import org.neo4j.cypher.internal.runtime.compiled.codegen.spi.{LessThanEqual, MethodStructure}

case class DecreaseAndReturnWhenZero(opName: String, variableName: String, action: Instruction, startValue: CodeGenExpression)
  extends Instruction {

  override def init[E](generator: MethodStructure[E])(implicit context: CodeGenContext): Unit = {
    startValue.init(generator)
    val expression = generator.box(startValue.generateExpression(generator), startValue.codeGenType)
    generator.declareCounter(variableName, expression)
    generator.ifStatement(generator.checkInteger(variableName, LessThanEqual, 0L)) { onTrue =>
      onTrue.returnSuccessfully()
    }
    action.init(generator)
  }

  override def body[E](generator: MethodStructure[E])(implicit context: CodeGenContext): Unit = {
    action.body(generator)

    generator.trace(opName) { l1 =>
      l1.incrementRows()
      l1.decrementInteger(variableName)
      l1.ifStatement(l1.checkInteger(variableName, LessThanEqual, 0L)) { l2 =>
        l2.returnSuccessfully()
      }
    }
  }

  override protected def children: Seq[Instruction] = Seq(action)

  override protected def operatorId = Set(opName)
}
