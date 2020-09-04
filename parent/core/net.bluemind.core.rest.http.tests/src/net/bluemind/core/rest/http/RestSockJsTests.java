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
import static org.junit.Assert.assertTrue;

import java.io.PrintStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.internal.SockJsProvider;
import net.bluemind.core.rest.http.internal.VertxSockJsClientFactory;
import net.bluemind.core.rest.tests.services.IRestPathTestService;
import net.bluemind.core.rest.tests.services.IRestPathTestServiceAsync;
import net.bluemind.core.rest.tests.services.IRestTestService;
import net.bluemind.core.rest.tests.services.IRestTestServiceAsync;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.lib.vertx.VertxPlatform;

public class RestSockJsTests {

	private HttpClientProvider provider;
	private HttpClient httpClient;
	private SockJsProvider sockJsProvider;

	@Before
	public void setup() throws Exception {

		VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);

		provider = new HttpClientProvider(VertxPlatform.getVertx());
		httpClient = provider.getClient("localhost", 8090);
		Sessions.get().put("yeah", SecurityContext.SYSTEM);

		sockJsProvider = new SockJsProvider(httpClient, "/eventbus/websocket");
		System.err.println("setup() complete, starting test.");
	}

	@After
	public void after() throws Exception {
		System.err.println("after() test");
		httpClient.close();
	}

	@Test
	public void testSayWithQueryParams() {
		assertEquals("+lach&ain€+" + 40 + true,
				getRestTestService(SecurityContext.ANONYMOUS).sayWithQueryParams("+lach&ain€+", 40, true));
	}

	@Test
	public void testSayWithQueryParamsPerf() {
		IRestTestService service = getRestTestService(SecurityContext.ANONYMOUS);
		String result = "+lach&ain€+" + 40 + true;
		assertEquals(result, service.sayWithQueryParams("+lach&ain€+", 40, true));
		int cnt = 50000;
		PrintStream ps = new PrintStream(ByteStreams.nullOutputStream());
		for (int i = 0; i < cnt; i++) {
			String ret = service.sayWithQueryParams("+lach&ain€+", 40, true);
			ps.println(ret);
			if (i % 10000 == 0) {
				System.err.println(i + " / " + cnt);
			}
		}
	}

	@Test
	public void testEventBusForwarding() throws InterruptedException, ExecutionException, TimeoutException {
		String toAck = UUID.randomUUID().toString();
		CompletableFuture<Void> ack = new CompletableFuture<>();
		CountDownLatch cdl = new CountDownLatch(20000);
		sockJsProvider.ws(socket -> {
			sockJsProvider.registerHandler("yeah", "sockjs.tests.rocks", data -> {
				long cnt = cdl.getCount();
				if (cnt % 1000 == 0) {
					System.err.println("(cdl: " + cdl.getCount() + ") WS receives from bus: '" + data + "'");
				}
				if (!ack.isDone()) {
					JsonObject body = data.getJsonObject("body");
					if (body != null && toAck.equals(body.getString("ack"))) {
						ack.complete(null);
					}
				}
				if (cnt == 1) {
					sockJsProvider.unregisterHandler("yeah", "sockjs.tests.rocks");
				}
				cdl.countDown();

			});
		});
		Thread.sleep(1000);
		DeliveryOptions opts = new DeliveryOptions().setSendTimeout(1000);
		CompletableFuture<Void> cr = new CompletableFuture<>();
		JsonObject req = new JsonObject().put("ack", toAck);
		VertxPlatform.eventBus().request("sockjs.tests.rocks", req, opts, ar -> {
			if (ar.succeeded()) {
				cr.complete(null);
			} else {
				cr.completeExceptionally(ar.cause());
			}
		});
		cr.get(2, TimeUnit.SECONDS);
		ack.get(2, TimeUnit.SECONDS);
		assertTrue(cdl.await(1, TimeUnit.MINUTES));
		Thread.sleep(200);
	}

	public IRestTestService getRestTestService(SecurityContext context) {
		return new VertxSockJsClientFactory<>(IRestTestService.class, IRestTestServiceAsync.class, sockJsProvider)
				.syncClient(context.getSessionId());
	}

	public IRestPathTestService getRestPathTestService(SecurityContext context, String param1, String param2) {
		return new VertxSockJsClientFactory<>(IRestPathTestService.class, IRestPathTestServiceAsync.class,
				sockJsProvider).syncClient(context.getSessionId(), param1, param2);
	}

}
