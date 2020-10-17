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
package org.neo4j.cluster.protocol.atomicbroadcast;

import java.util.HashMap;
import java.util.Map;

public class VersionMapper
{
    private static final Map<String, Long> classNameToSerialVersionUID = new HashMap<>();

    public long mappingFor( String className )
    {
        return classNameToSerialVersionUID.get( className );
    }

    public boolean hasMappingFor( String className )
    {
        return classNameToSerialVersionUID.containsKey( className );
    }

    public void addMappingFor( String wireClassDescriptorName, long serialVersionUID )
    {
        classNameToSerialVersionUID.put(wireClassDescriptorName, serialVersionUID);
    }
}
