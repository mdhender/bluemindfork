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
package net.bluemind.webmodule.server.tests;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.lib.vertx.VertxPlatform;

public class SimpleTests {

	private AsyncHttpClient client;

	@Before
	public void setup() {
		VertxPlatform.spawnBlocking(1, TimeUnit.MINUTES);
		client = new DefaultAsyncHttpClient();
	}

	@Test
	public void testSimple() throws Exception {

		for (int i = 0; i < 1000; i++) {
			Response resp = client.prepareGet("http://localhost:" + 8081 + "/tests/statics/toto.html").execute().get();
			assertEquals(200, resp.getStatusCode());
		}

		for (int i = 0; i < 100000; i++) {
			long time = System.nanoTime();
			Response resp = client.prepareGet("http://localhost:" + 8081 + "/tests/statics/toto.html").execute().get();
			assertEquals(200, resp.getStatusCode());
			long e = System.nanoTime() - time;
			if (e > 1 * 1000 * 1000) {
				System.err.println("time 200 " + (e / 1000000.0));
			}
		}
	}
}
