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
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.platform.Verticle;

public class WebModuleServerVerticle extends Verticle {
	private static final Logger logger = LoggerFactory.getLogger(WebModuleServerVerticle.class);

	public static final int PORT = 8080;

	@Override
	public void start() {

		HttpServer httpServer = vertx.createHttpServer();
		httpServer.setAcceptBacklog(1024);
		httpServer.setTCPNoDelay(true);
		httpServer.setReuseAddress(true);
		httpServer.setUsePooledBuffers(true);

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
