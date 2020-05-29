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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import net.bluemind.lib.vertx.RouteMatcher;

public class LocatorVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(LocatorVerticle.class);

	@Override
	public void start() {
		logger.info("Spawing a locator server instance...");
		HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setUsePooledBuffers(true)
				.setTcpNoDelay(true).setAcceptBacklog(1024).setReuseAddress(true));

		RouteMatcher rm = new RouteMatcher(vertx);
		HostLocationHandler hls = new HostLocationHandler(vertx);
		rm.get("/location/host/:kind/:tag/:latd", hls);
		rm.noMatch(req -> req.response().setStatusCode(404).end());

		httpServer.requestHandler(rm);

		tryListen(httpServer);

	}

	private void tryListen(HttpServer httpServer) {
		httpServer.listen(LocatorService.LOCATOR_PORT, (AsyncResult<HttpServer> event) -> {
			if (event.succeeded()) {
				logger.info("Bound to {}", LocatorService.LOCATOR_PORT);
			} else {
				logger.error("Retrying in 5sec (cause: {})", event.cause().getMessage());
				vertx.setTimer(5000, tid -> tryListen(httpServer));
			}
		});
	}
}
