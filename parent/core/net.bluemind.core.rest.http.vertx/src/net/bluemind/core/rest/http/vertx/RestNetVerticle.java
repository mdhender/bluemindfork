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
package net.bluemind.core.rest.http.vertx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import net.bluemind.core.rest.base.RestRootHandler;
import net.bluemind.core.rest.sockjs.vertx.RestSockJSProxyServer;
import net.bluemind.lib.vertx.RouteMatcher;

public class RestNetVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(RestNetVerticle.class);

	private static final int PORT = Integer.parseInt(System.getProperty("bm.rest.net.verticle.port", "8090"));

	@Override
	public void start(Promise<Void> started) {
		HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setAcceptBacklog(1024)
				.setTcpKeepAlive(true).setTcpNoDelay(true).setTcpFastOpen(true).setReuseAddress(true));

		RouteMatcher routeMatcher = new RouteMatcher(vertx);
		RestRootHandler rootHandler = new RestRootHandler(vertx);
		routeMatcher.noMatch(new RestHttpProxyHandler(getVertx(), rootHandler));
		HttpRoutes.bindRoutes(vertx, rootHandler.executor(), routeMatcher);

		httpServer.requestHandler(routeMatcher);

		RestSockJSProxyServer sockProxy = new RestSockJSProxyServer(vertx, rootHandler, rootHandler);
		routeMatcher.websocket("/eventbus",
				new SockJSHandlerOptions().setInsertJSESSIONID(false)
						.setLibraryURL("https://cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js")
						.setRegisterWriteHandler(true).setHeartbeatInterval(50000),
				sockProxy);

		httpServer.listen(PORT, (AsyncResult<HttpServer> event) -> {
			if (event.succeeded()) {
				logger.info("Bound to port {}.", PORT);
				started.complete(null);
			} else {
				logger.error("Failed to bind to port {}.", PORT, event.cause());
				started.fail(event.cause());
			}
		});

	}
}
