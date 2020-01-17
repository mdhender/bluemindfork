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

import org.junit.After;
import org.junit.Before;

import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.internal.SockJsProvider;
import net.bluemind.core.rest.http.internal.VertxSockJsClientFactory;
import net.bluemind.core.rest.tests.services.IRestPathTestService;
import net.bluemind.core.rest.tests.services.IRestPathTestServiceAsync;
import net.bluemind.core.rest.tests.services.IRestTestService;
import net.bluemind.core.rest.tests.services.IRestTestServiceAsync;
import net.bluemind.lib.vertx.VertxPlatform;

public class RestSockJsTests { // extends RestTestServiceTests {

	private HttpClientProvider provider;
	private HttpClient httpClient;
	private SockJsProvider sockJsProvider;

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

		provider = new HttpClientProvider(VertxPlatform.getVertx());
		httpClient = provider.getClient("localhost", 8090);

		sockJsProvider = new SockJsProvider(httpClient, "/eventbus/websocket");

	}

	@After
	public void after() throws Exception {
		// httpClient.close();
	}

	// @Override
	public IRestTestService getRestTestService(SecurityContext context) {
		return new VertxSockJsClientFactory<>(IRestTestService.class, IRestTestServiceAsync.class, sockJsProvider)
				.syncClient(context.getSessionId());
	}

	// @Override
	public IRestPathTestService getRestPathTestService(SecurityContext context, String param1, String param2) {
		return new VertxSockJsClientFactory<>(IRestPathTestService.class, IRestPathTestServiceAsync.class,
				sockJsProvider).syncClient(context.getSessionId(), param1, param2);
	}

}
