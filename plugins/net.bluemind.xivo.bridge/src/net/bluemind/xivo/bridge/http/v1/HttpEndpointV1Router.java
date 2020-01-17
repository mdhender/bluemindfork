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
package net.bluemind.xivo.bridge.http.v1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.lib.vertx.RouteMatcher;

public class HttpEndpointV1Router extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(HttpEndpointV1Router.class);

	public HttpEndpointV1Router() {

	}

	@Override
	public void start() {

		HttpServer srv = vertx.createHttpServer(new HttpServerOptions().setTcpNoDelay(true).setAcceptBacklog(1024)
				.setReuseAddress(true).setUsePooledBuffers(true));

		RouteMatcher rm = new RouteMatcher(vertx);
		rm.post("/xivo/1.0/event/:domain/dropbox/", new EventsDropboxHandler(vertx.eventBus()));
		rm.get("/xivo/1.0/status/:domain/:login/", new GetStatusHandler());

		rm.noMatch(new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest event) {
				logger.warn("no match for {} {}", event.method(), event.path());
				event.response().end();
			}
		});

		srv.requestHandler(rm);
		srv.listen(9091);
	}
}
