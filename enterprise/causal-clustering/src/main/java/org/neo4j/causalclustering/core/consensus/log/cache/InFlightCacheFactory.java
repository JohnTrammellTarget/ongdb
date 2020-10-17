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
package org.neo4j.causalclustering.core.consensus.log.cache;

import org.neo4j.causalclustering.core.CausalClusteringSettings;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.monitoring.Monitors;

import static org.neo4j.causalclustering.core.CausalClusteringSettings.in_flight_cache_max_bytes;
import static org.neo4j.causalclustering.core.CausalClusteringSettings.in_flight_cache_max_entries;

public class InFlightCacheFactory
{
    public static InFlightCache create( Config config, Monitors monitors )
    {
        return config.get( CausalClusteringSettings.in_flight_cache_type ).create( config, monitors );
    }

    public enum Type
    {
        NONE
                {
                    @Override
                    InFlightCache create( Config config, Monitors monitors )
                    {
                        return new VoidInFlightCache();
                    }
                },
        CONSECUTIVE
                {
                    @Override
                    InFlightCache create( Config config, Monitors monitors )
                    {
                        return new ConsecutiveInFlightCache( config.get( in_flight_cache_max_entries ), config.get( in_flight_cache_max_bytes ),
                                monitors.newMonitor( InFlightCacheMonitor.class ), false );
                    }
                },
        UNBOUNDED
                {
                    @Override
                    InFlightCache create( Config config, Monitors monitors )
                    {
                        return new UnboundedInFlightCache();
                    }
                };

        abstract InFlightCache create( Config config, Monitors monitors );
    }
}
