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

import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.util.concurrent.SettableFuture;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import net.bluemind.core.rest.tests.services.IRestStreamTestService;
import net.bluemind.core.rest.tests.services.IRestStreamTestServiceAsync;
import net.bluemind.core.rest.tests.services.RestStreamServiceTests;
import net.bluemind.lib.vertx.VertxPlatform;

public class RestHttpStreamTests extends RestStreamServiceTests {

	private AsyncHttpClient httpClient;

	@Before
	public void setup() throws Exception {

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		httpClient = new AsyncHttpClient();
	}

	@After
	public void after() throws Exception {
		httpClient.close();
	}

	@Test
	public void testInContentType() throws InterruptedException, ExecutionException {
		Response response = httpClient
				.prepareGet("http://localhost:8090/api/teststream/inContentType?mime=toto&cs=titi").execute().get();
		assertEquals("toto; charset=titi", response.getContentType());
		response = httpClient.prepareGet("http://localhost:8090/api/teststream/inContentType?cs=titi").execute().get();
		assertEquals("application/octet-stream; charset=titi", response.getContentType());
		response = httpClient.prepareGet("http://localhost:8090/api/teststream/inContentType?fn=toto.pdf").execute()
				.get();
		String disposition = response.getHeader("Content-Disposition");
		System.err.println(disposition);
		assertEquals("attachment; filename=\"toto.pdf\";", disposition);
	}

	@Override
	protected IRestStreamTestService getService() {
		return HttpClientFactory.create(IRestStreamTestService.class, IRestStreamTestServiceAsync.class,
				"http://localhost:8090", httpClient).syncClient((String) null);
	}

}
