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
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.tests.services.IRestSecuredTestService;
import net.bluemind.core.rest.tests.services.IRestSecuredTestServiceAsync;
import net.bluemind.core.rest.tests.services.IRestTestService;
import net.bluemind.core.rest.tests.services.IRestTestServiceAsync;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.vertx.VertxPlatform;

public class RestSimpleHttpTests {

	private AsyncHttpClient httpClient;

	@Before
	public void setup() throws Exception {
		VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);
		httpClient = new DefaultAsyncHttpClient();
	}

	@After
	public void after() throws Exception {
		httpClient.close();
	}

	@Test
	public void testProduces() throws Exception {

		AsyncHttpClient asyncHttpClient = httpClient;
		Future<Response> f = asyncHttpClient.prepareGet("http://localhost:8090/api/test/mime").execute();
		Response r = f.get();
		Assert.assertEquals("hello", new String(r.getResponseBodyAsBytes()));
		Assert.assertEquals("application/binary", r.getContentType());

		f = asyncHttpClient.prepareGet("http://localhost:8090/api/test/bytearray").execute();
		r = f.get();
		Assert.assertEquals("hello", new String(r.getResponseBodyAsBytes()));
		Assert.assertEquals("application/binary", r.getContentType());

		// test with client
		IRestTestService client = HttpClientFactory
				.create(IRestTestService.class, IRestTestServiceAsync.class, "http://localhost:8090", httpClient)
				.syncClient((String) null);

		Assert.assertEquals("hello", client.mime());

		// test with client
		Assert.assertArrayEquals("hello".getBytes(), client.bytearray());

	}

	@Test
	public void testRestAuthentified() throws Exception {
		SecurityContext sc = new SecurityContext("myFakeKey", "admin0@global.virt", Arrays.<String>asList(),
				Arrays.<String>asList(), null);
		Sessions.get().put("myFakeKey", sc);

		AsyncHttpClient asyncHttpClient = httpClient;
		Future<Response> f = asyncHttpClient.prepareGet("http://localhost:8090/api/test/toto/hello")
				.setHeader("X-BM-ApiKey", "myFakeKey").execute();
		Response r = f.get();
		Assert.assertEquals("hello toto admin0@global.virt", JsonUtils.read(r.getResponseBody(), String.class));
		Assert.assertEquals("application/json", r.getContentType());
		asyncHttpClient.close();
	}

	@Test
	public void testRestAuthentifiedClient() throws Exception {
		SecurityContext sc = new SecurityContext("myFakeKey", "admin0@global.virt", Arrays.<String>asList(),
				Arrays.<String>asList(), null);
		Sessions.get().put("myFakeKey", sc);

		// FIXME test async api
		String resp = HttpClientFactory
				.create(IRestTestService.class, IRestTestServiceAsync.class, "http://localhost:8090", httpClient)
				.syncClient("myFakeKey").sayHello("toto");

		Assert.assertEquals("hello toto admin0@global.virt", resp);

		String sResp = HttpClientFactory
				.create(IRestTestService.class, IRestTestServiceAsync.class, "http://localhost:8090", httpClient)
				.syncClient("myFakeKey").sayHello("toto");

		Assert.assertEquals("hello toto admin0@global.virt", sResp);
	}

	@Test
	public void testRestAuthFailure() throws Exception {
		try {
			HttpClientFactory.create(IRestSecuredTestService.class, IRestSecuredTestServiceAsync.class,
					"http://localhost:8090", httpClient).syncClient("" + System.nanoTime()).helloMaster();
			fail("The call should throw an fault");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.AUTHENTICATION_FAIL, e.getCode());
		}

	}

	@Test
	public void testRestPerfSync() throws Exception {

		IRestTestService client = HttpClientFactory
				.create(IRestTestService.class, IRestTestServiceAsync.class, "http://localhost:8090", httpClient)
				.syncClient((String) null);

		// warmup
		for (int i = 0; i < 2; i++) {

			String resp = client.sayHello("toto");
			Assert.assertEquals("hello toto anonymous", resp);

		}

		long time = System.nanoTime();

		int CALL_COUNT = 1000;
		for (int i = 0; i < CALL_COUNT; i++) {
			String resp = client.sayHello("toto");
			Assert.assertEquals("hello toto anonymous", resp);
		}

		long elaspedTime = System.nanoTime() - time;
		System.out.println("1 call sync in " + ((elaspedTime / (float) CALL_COUNT) / (float) (1000 * 1000)) + " ms");

	}

	@Test
	public void testPathParamEncoding() throws Exception {
		IRestTestService client = HttpClientFactory
				.create(IRestTestService.class, IRestTestServiceAsync.class, "http://localhost:8090", httpClient)
				.syncClient((String) null);
		String resp = client.sayHello("bm++==");
		Assert.assertEquals("hello bm++== anonymous", resp);
	}

}
