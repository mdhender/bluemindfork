/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.retry.support.tests;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Test;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.retry.support.RetryQueueVerticle;
import net.bluemind.retry.support.RetryQueueVerticle.RetryProcessor;
import net.bluemind.retry.support.RetryRequester;

public class RetryQueuesTests {

	public static class TestQueueVerticle extends RetryQueueVerticle {

		protected TestQueueVerticle(RetryProcessor rp) {
			super("junit-" + System.nanoTime(), rp);
		}

	}

	public static class Retry implements RetryProcessor {

		private boolean failNext;
		private int processed;
		private int failed;

		@Override
		public void retry(JsonObject js) {
			if (failNext) {
				failNext = false;
				failed++;
				throw new IllegalArgumentException("we were asked to fail for " + js.encode());
			}
			System.err.println(js.encodePrettily());
			processed++;
		}

		public void failNext() {
			this.failNext = true;
		}

	}

	@Test
	public void createRetryQueue() throws InterruptedException {

		Vertx vertx = VertxPlatform.getVertx();

		Retry retry = new Retry();

		TestQueueVerticle verticle = new TestQueueVerticle(retry);
		vertx.deployVerticle(() -> verticle, new DeploymentOptions().setInstances(1)).toCompletionStage()
				.toCompletableFuture().orTimeout(2, TimeUnit.SECONDS).join();

		RetryRequester req = new RetryRequester(vertx.eventBus(), verticle.topic());
		int i = 0;

		req.request(new JsonObject().put("she.says", "yeah " + (i++)));
		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> retry.processed == 1);

		retry.failNext();
		req.request(new JsonObject().put("she.says", "yeah " + (i++)));
		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> retry.failed == 1);

		req.request(new JsonObject().put("she.says", "yeah " + (i++)));

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> retry.processed == 3);
	}

}
