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
package org.neo4j.kernel.extension;

import org.neo4j.helpers.Service;
import org.neo4j.kernel.impl.spi.KernelContext;
import org.neo4j.kernel.lifecycle.Lifecycle;

public abstract class KernelExtensionFactory<DEPENDENCIES> extends Service
{
    private final ExtensionType extensionType;

    protected KernelExtensionFactory( String key )
    {
        this( ExtensionType.GLOBAL, key );
    }

    protected KernelExtensionFactory( ExtensionType extensionType, String key )
    {
        super( key );
        this.extensionType = extensionType;
    }

    /**
     * Create a new instance of this kernel extension.
     *
     * @param context the context the extension should be created for
     * @param dependencies deprecated
     * @return the {@link Lifecycle} for the extension
     */
    public abstract Lifecycle newInstance( KernelContext context, DEPENDENCIES dependencies );

    @Override
    public String toString()
    {
        return "KernelExtension:" + getClass().getSimpleName() + getKeys();
    }

    ExtensionType getExtensionType()
    {
        return extensionType;
    }
}
