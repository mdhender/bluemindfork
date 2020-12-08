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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import net.bluemind.eas.config.global.GlobalConfig;

public class EASHttpVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(EASHttpVerticle.class);

	@Override
	public void start(final Promise<Void> future) {
		HttpServer httpServer = vertx.createHttpServer(
				new HttpServerOptions().setAcceptBacklog(1024).setTcpNoDelay(true).setReuseAddress(true));

		EASRouter router = new EASRouter(vertx);

		httpServer.requestHandler(router);

		httpServer.listen(GlobalConfig.EAS_PORT, (AsyncResult<HttpServer> event) -> {
			if (event.succeeded()) {
				logger.info("Bound to port {}", GlobalConfig.EAS_PORT);
				future.complete();
			} else {
				Throwable t = event.cause();
				logger.error(t.getMessage(), t);
				future.fail(t);
			}
		});

	}

}
