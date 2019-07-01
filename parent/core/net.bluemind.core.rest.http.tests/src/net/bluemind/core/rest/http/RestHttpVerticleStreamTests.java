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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.util.concurrent.SettableFuture;
import com.ning.http.client.AsyncHttpClient;

import net.bluemind.core.rest.tests.services.IRestStreamTestService;
import net.bluemind.core.rest.tests.services.RestStreamServiceTests;
import net.bluemind.lib.vertx.VertxPlatform;

public class RestHttpVerticleStreamTests extends RestStreamServiceTests {

	private AsyncHttpClient httpClient;

	@Before
	public void setup() throws Exception {
		super.before();

		final SettableFuture<Void> future = SettableFuture.<Void> create();
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
	public void after() {
		httpClient.close();
	}

	@Override
	protected IRestStreamTestService getService() {
		return HttpClientFactory.create(IRestStreamTestService.class, null, "http://localhost:8090", httpClient)
				.syncClient((String) null);
	}

	// @Test
	public void testReadSpeed2() throws InterruptedException {

		List<Callable<Void>> calls = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			final int current = i;
			calls.add(new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					try {
						if ((current % 4) == 0) {
							System.out.println("read speed " + Thread.currentThread());
							testReadSpeed();

							System.out.println("FINISHED read speed " + Thread.currentThread());
						} else if (current % 4 == 1) {
							System.out.println("lot of calls " + Thread.currentThread());
							testLotOfCalls();

							System.out.println("FINISHED lot of calls " + Thread.currentThread());
						} else if (current % 4 == 2) {
							System.out.println("read speed " + Thread.currentThread());
							testReadSpeed();

							System.out.println("FINISHED read speed " + Thread.currentThread());
						} else {
							System.out.println("test out 2 " + Thread.currentThread());
							testOut2();
							System.out.println("FINISHED out 2 " + Thread.currentThread());
						}

					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return null;
				}
			});
		}

		ExecutorService pool = Executors.newFixedThreadPool(50);
		List<Future<Void>> futures = pool.invokeAll(calls);
		for (java.util.concurrent.Future<Void> f : futures) {
			try {
				System.out.println("wait 40 secs");
				f.get(10, TimeUnit.SECONDS);
			} catch (ExecutionException e) {
				e.printStackTrace();
				fail(e.getCause().getMessage());
			} catch (TimeoutException e) {
				fail(e.getMessage());
			}
		}

		System.out.println("FINISHED");
		pool.shutdown();
	}
}
