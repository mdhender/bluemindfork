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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.lib.vertx.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.vertx.testhelper.Deploy;

public class BlockingCodeTests {

	static final Logger logger = LoggerFactory.getLogger(BlockingCodeTests.class);

	private static class ThreadPair {
		public ThreadPair(String thread1, String thread2) {
			this.thread1 = thread1;
			this.thread2 = thread2;
		}

		String thread1;
		String thread2;
	}

	@Test
	public void testBlockingCodeFromEventLoop() throws InterruptedException, ExecutionException, TimeoutException {
		Vertx pm = VertxPlatform.getVertx();
		CompletableFuture<String> deployement = new CompletableFuture<>();
		Deploy.verticles(false, SimpleHttpServer::new);
		String depId = deployement.get(1, TimeUnit.SECONDS);
		assertNotNull(depId);

		// server is deployed
		CompletableFuture<ThreadPair> pair = new CompletableFuture<>();
		SimpleHttpServer.setThreadsRecorder((t1, t2) -> {
			logger.info("Threads are {} {}", t1, t2);
			pair.complete(new ThreadPair(t1, t2));
		});
		CompletableFuture<Integer> httpResponseStatus = new CompletableFuture<>();
		HttpClient client = VertxPlatform.getVertx()
				.createHttpClient(new HttpClientOptions().setDefaultHost("127.0.0.1").setDefaultPort(6666));
		client.get("/", httpResp -> {
			httpResp.bodyHandler(buf -> {
				httpResponseStatus.complete(httpResp.statusCode());
			});
		}).end();
		int httpStatus = httpResponseStatus.get(1, TimeUnit.SECONDS);
		assertEquals(200, httpStatus);
		ThreadPair usedThreads = pair.get();
		assertEquals(usedThreads.thread1, usedThreads.thread2);

		CompletableFuture<Void> undeploy = new CompletableFuture<>();
		pm.undeploy(depId, result -> {
			undeploy.complete(null);
		});
		undeploy.join();
	}

}
