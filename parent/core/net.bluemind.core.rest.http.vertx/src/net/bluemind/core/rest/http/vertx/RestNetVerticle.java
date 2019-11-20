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
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.java.platform.Verticle;

import net.bluemind.core.rest.base.RestRootHandler;
import net.bluemind.core.rest.sockjs.vertx.RestSockJSProxyServer;

public class RestNetVerticle extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(RestNetVerticle.class);

	private static final int PORT = Integer.parseInt(System.getProperty("bm.rest.net.verticle.port", "8090"));

	@Override
	public void start(Future<Void> started) {
		HttpServer httpServer = vertx.createHttpServer();
		httpServer.setAcceptBacklog(1024);
		httpServer.setTCPKeepAlive(true);
		httpServer.setTCPNoDelay(true);
		httpServer.setReuseAddress(true);
		httpServer.setUsePooledBuffers(true);

		RouteMatcher routeMatcher = new RouteMatcher();
		RestRootHandler rootHandler = new RestRootHandler(vertx);
		routeMatcher.noMatch(new RestHttpProxyHandler(getVertx(), rootHandler));
		// new VertxClientCallHandler(vertx)));
		// add http handlers route
		HttpRoutes.bindRoutes(vertx, rootHandler.executor(), routeMatcher);

		httpServer.requestHandler(routeMatcher);
		SockJSServer sockServer = vertx.createSockJSServer(httpServer);

		JsonObject rconfig = new JsonObject();
		rconfig.putString("prefix", "/eventbus");
		rconfig.putBoolean("insert_JSESSIONID", false);
		rconfig.putString("library_url", "https://cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js");
		rconfig.putNumber("heartbeat_period", 10 * 1000);
		sockServer.installApp(rconfig, new RestSockJSProxyServer(vertx, rootHandler, rootHandler));

		httpServer.listen(PORT, new Handler<AsyncResult<HttpServer>>() {

			@Override
			public void handle(AsyncResult<HttpServer> event) {
				if (event.succeeded()) {
					logger.info("Bound to port {}.", PORT);
					started.setResult(null);
				} else {
					logger.error("Failed to bind to port {}.", PORT, event.cause());
					started.setFailure(event.cause());
				}
			}
		});

	}
}
