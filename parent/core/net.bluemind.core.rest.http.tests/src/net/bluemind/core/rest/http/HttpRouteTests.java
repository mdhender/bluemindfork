/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.rest.http;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.lib.vertx.VertxPlatform;

public class HttpRouteTests {

	private AsyncHttpClient httpClient;

	@Before
	public void setup() throws Exception {
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		httpClient = new DefaultAsyncHttpClient();
	}

	@After
	public void after() throws IOException {
		httpClient.close();
	}

	@Test
	public void testHttpRouteWellBinded() throws InterruptedException, ExecutionException, TimeoutException {
		Response resp = httpClient.prepareGet("http://localhost:8090/route-test/binded").execute().get(30,
				TimeUnit.SECONDS);
		assertEquals(200, resp.getStatusCode());
		assertEquals("OK-TEST", resp.getResponseBody());
	}
}
