/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2018 "Neo4j,"
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
package org.neo4j.cypher.internal.compatibility.v3_6.runtime

import org.neo4j.cypher.internal.v3_6.util.symbols.CypherType

sealed trait Slot {
  def offset: Int
  def nullable: Boolean
  def typ: CypherType
  def isTypeCompatibleWith(other: Slot): Boolean
  def isLongSlot: Boolean
  def asNullable: Slot
}

case class LongSlot(offset: Int, nullable: Boolean, typ: CypherType) extends Slot {
  override def isTypeCompatibleWith(other: Slot): Boolean = other match {
    case LongSlot(_, _, otherTyp) =>
      typ.isAssignableFrom(otherTyp) || otherTyp.isAssignableFrom(typ)
    case _ => false
  }

  override def isLongSlot: Boolean = true

  override def asNullable = LongSlot(offset, true, typ)
}

case class RefSlot(offset: Int, nullable: Boolean, typ: CypherType) extends Slot {
  override def isTypeCompatibleWith(other: Slot): Boolean = other match {
    case RefSlot(_, _, otherTyp) =>
      typ.isAssignableFrom(otherTyp) || otherTyp.isAssignableFrom(typ)
    case _ => false
  }

  override def isLongSlot: Boolean = false

  override def asNullable = RefSlot(offset, true, typ)
}

sealed trait SlotWithAliases {
  def slot: Slot
  def aliases: Set[String]

  protected def makeString: String = {
    val aliasesString = s"${aliases.mkString("'", "','", "'")}"
    f"$slot%-30s $aliasesString%-10s"
  }
}

case class LongSlotWithAliases(slot: LongSlot, aliases: Set[String]) extends SlotWithAliases {
  override def toString: String = makeString
}

case class RefSlotWithAliases(slot: RefSlot, aliases: Set[String]) extends SlotWithAliases {
  override def toString: String = makeString
}
