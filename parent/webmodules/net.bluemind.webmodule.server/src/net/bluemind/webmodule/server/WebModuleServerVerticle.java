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
package net.bluemind.webmodule.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

public class WebModuleServerVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(WebModuleServerVerticle.class);

	public static final int PORT = 8080;

	@Override
	public void start() {

		HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setAcceptBacklog(1024)
				.setTcpNoDelay(true).setReuseAddress(true).setUsePooledBuffers(true));

		WebModuleRootHandler rootHandler = WebModuleRootHandler.build(getVertx());

		httpServer.requestHandler(rootHandler).listen(PORT, new Handler<AsyncResult<HttpServer>>() {

			@Override
			public void handle(AsyncResult<HttpServer> event) {
				if (event.succeeded()) {
					logger.info("Bound to port {}.", PORT);
				} else {
					logger.error("Failed to bind to port {}.", PORT, event.cause());
				}
			}
		});
	}
}
