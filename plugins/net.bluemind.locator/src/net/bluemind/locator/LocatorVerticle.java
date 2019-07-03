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
package net.bluemind.locator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

public class LocatorVerticle extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(LocatorVerticle.class);

	private static final ExecutorService blockingPool = Executors
			.newFixedThreadPool(2 * Runtime.getRuntime().availableProcessors());

	public LocatorVerticle() {
	}

	public void start(Future<Void> startedResult) {
		logger.info("Spawing a locator server instance...");
		HttpServer httpServer = vertx.createHttpServer();
		httpServer.setUsePooledBuffers(true).setTCPNoDelay(true).setReuseAddress(true);
		httpServer.setAcceptBacklog(1024);

		RouteMatcher rm = new RouteMatcher();
		HostLocationHandler hls = new HostLocationHandler(vertx, blockingPool);
		rm.get("/location/host/:kind/:tag/:latd", hls);

		httpServer.requestHandler(rm).listen(LocatorService.LOCATOR_PORT, new Handler<AsyncResult<HttpServer>>() {

			@Override
			public void handle(AsyncResult<HttpServer> event) {
				if (event.succeeded()) {
					logger.info("Bound to {}", LocatorService.LOCATOR_PORT);
					startedResult.setResult(null);
				} else {
					logger.error(event.cause().getMessage(), event.cause());
					startedResult.setFailure(event.cause());
				}
			}
		});
	}
}
