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
package net.bluemind.eas.http.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.platform.Verticle;

import net.bluemind.eas.config.global.GlobalConfig;

public class EASHttpVerticle extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(EASHttpVerticle.class);

	@Override
	public void start(final Future<Void> future) {
		HttpServer httpServer = vertx.createHttpServer();
		httpServer.setAcceptBacklog(1024);
		httpServer.setUsePooledBuffers(true);
		httpServer.setTCPNoDelay(true);
		httpServer.setReuseAddress(true);
		httpServer.setCompressionSupported(true);

		EASRouter router = new EASRouter(vertx);

		httpServer.requestHandler(router);

		httpServer.listen(GlobalConfig.EAS_PORT, new Handler<AsyncResult<HttpServer>>() {

			@Override
			public void handle(AsyncResult<HttpServer> event) {
				if (event.succeeded()) {
					logger.info("Bound to port {}", GlobalConfig.EAS_PORT);
					future.setResult(null);
				} else {
					Throwable t = event.cause();
					logger.error(t.getMessage(), t);
					future.setFailure(t);
				}
			}
		});

	}

}
