/*
 * Copyright (c) 2018-2020 "Graph Foundation,"
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
package org.neo4j.server;

import com.sun.jersey.api.client.Client;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import javax.ws.rs.core.MediaType;

import org.neo4j.server.helpers.ServerHelper;
import org.neo4j.server.rest.JaxRsResponse;
import org.neo4j.server.rest.RestRequest;
import org.neo4j.test.server.ExclusiveServerTestBase;

import static org.junit.Assert.assertEquals;

public class RedirectToBrowserTestIT extends ExclusiveServerTestBase
{
    private static NeoServer server;

    @BeforeClass
    public static void startServer() throws IOException
    {
        server = ServerHelper.createNonPersistentServer();
    }

    @AfterClass
    public static void stopServer()
    {
        if ( server != null )
        {
            server.stop();
        }
    }

    @Test
    public void shouldRedirectToBrowser() throws Exception
    {
        Client nonRedirectingClient = Client.create();
        nonRedirectingClient.setFollowRedirects( false );
        final JaxRsResponse response = new RestRequest( server.baseUri(), nonRedirectingClient ).accept( MediaType
                .TEXT_HTML_TYPE ).get( server.baseUri().toString() );

        assertEquals( 303, response.getStatus() );
        assertEquals( new URI( server.baseUri() + "browser/" ), response.getLocation() );
        response.close();
    }

    @Test
    public void shouldRedirectToBrowserUsingXForwardedHeaders() throws Exception
    {
        Client nonRedirectingClient = Client.create();
        nonRedirectingClient.setFollowRedirects( false );
        final JaxRsResponse response = new RestRequest( server.baseUri(), nonRedirectingClient ).accept( MediaType
                .TEXT_HTML_TYPE ).header( "X-Forwarded-Host", "foo.bar:8734" ).header( "X-Forwarded-Proto",
                "https" ).get( server.baseUri().toString() );

        assertEquals( 303, response.getStatus() );
        assertEquals( new URI( "https://foo.bar:8734/browser/" ), response.getLocation() );
        response.close();
    }
}
