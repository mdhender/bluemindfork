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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import net.bluemind.lib.vertx.BlockingCode;

public class SimpleHttpServer extends AbstractVerticle {

	public static final Logger logger = LoggerFactory.getLogger(BlockingCodeTests.class);

	private static final ExecutorService blockingPool = Executors.newFixedThreadPool(1);

	public SimpleHttpServer() {

	}

	private static BiConsumer<String, String> usedThreads;

	public static void setThreadsRecorder(BiConsumer<String, String> ut) {
		usedThreads = ut;
	}

	public void start() {
		vertx.createHttpServer().requestHandler(req -> {
			req.exceptionHandler(t -> logger.error(t.getMessage(), t));
			req.bodyHandler(buf -> {
				String eventLoop = Thread.currentThread().getName();
				logger.info("Req body handler...");

				BlockingCode.forVertx(vertx).withExecutor(blockingPool).run(() -> {
					String blockingCodeThread = Thread.currentThread().getName();
					logger.info("Blocking code starts...");
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
					}
					logger.info("Blocking code finishes.");
					return blockingCodeThread;
				}).thenAccept(value -> {
					logger.info("Got value {}", value);
					String afterBlocking = Thread.currentThread().getName();
					usedThreads.accept(eventLoop, afterBlocking);
					req.response().end(buf);
				});
				logger.info("After body handler.");
			});
		}).listen(6666);
	}

}