/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.lib.vertx.VertxPlatform;

public class InlineApiTest {

	private AsyncHttpClient httpClient;

	@Before
	public void setup() throws Exception {
		VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);
		httpClient = new DefaultAsyncHttpClient();
	}

	@Test
	public void testInlineCall() throws Exception {
		AsyncHttpClient asyncHttpClient = httpClient;
		Future<Response> f = asyncHttpClient.prepareGet("http://localhost:8090/api/testinline").execute();
		Response r = f.get();
		Assert.assertEquals("\"hello\"", new String(r.getResponseBodyAsBytes()));

	}
}
