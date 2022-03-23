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

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.tests.services.IRestPathTestService;
import net.bluemind.core.rest.tests.services.IRestPathTestServiceAsync;
import net.bluemind.core.rest.tests.services.IRestTestService;
import net.bluemind.core.rest.tests.services.IRestTestServiceAsync;
import net.bluemind.core.rest.tests.services.RestTestServiceTests;
import net.bluemind.lib.vertx.VertxPlatform;

public class RestHttpTests extends RestTestServiceTests {

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

	@Test(timeout = 10000)
	public void callBlackHole() {
		IRestTestService testService = getRestTestService(SecurityContext.ANONYMOUS, 5);
		try {
			testService.blackHole();
			fail("got reply");
		} catch (Exception e) {
			System.err.println("Got a timeout as expected: " + e.getMessage());
		}
	}

	@Test
	public void testRestPathServiceSpeed() throws InterruptedException, ExecutionException, TimeoutException {
		IRestPathTestService srv = getRestPathTestService(SecurityContext.SYSTEM, "container", "user.root");
		ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
		int cnt = 500_000;
		List<ListenableFuture<String>> list = new ArrayList<>(cnt);
		for (int i = 0; i < cnt; i++) {
			ListenableFuture<String> fut = pool.submit(() -> srv.goodMorning("yeah"));
			list.add(fut);
		}
		CompletableFuture<Void> endOfRun = new CompletableFuture<>();
		Futures.whenAllComplete(list).run(() -> endOfRun.complete(null), MoreExecutors.directExecutor());
		endOfRun.get(5, TimeUnit.MINUTES);
	}

	@Override
	public IRestTestService getRestTestService(SecurityContext context) {
		return HttpClientFactory.create(IRestTestService.class, IRestTestServiceAsync.class, "http://localhost:8090")
				.syncClient(context.getSessionId());
	}

	public IRestTestService getRestTestService(SecurityContext context, int timeoutSec) {
		return ClientSideServiceProvider
				.getProvider("http://localhost:8090", context.getSessionId(), timeoutSec, timeoutSec, timeoutSec)
				.instance(IRestTestService.class);
	}

	@Override
	public IRestPathTestService getRestPathTestService(SecurityContext context, String param1, String param2) {
		return HttpClientFactory
				.create(IRestPathTestService.class, IRestPathTestServiceAsync.class, "http://localhost:8090")
				.syncClient(context.getSessionId(), param1, param2);
	}

}
