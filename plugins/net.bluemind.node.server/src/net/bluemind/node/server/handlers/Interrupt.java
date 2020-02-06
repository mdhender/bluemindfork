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
package net.bluemind.node.server.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;

public class Interrupt implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(Interrupt.class);

	@Override
	public void handle(HttpServerRequest req) {
		req.endHandler(v -> {
			try {
				long pid = Long.parseLong(req.params().get("reqId"));
				VertxPlatform.eventBus().request("cmd.interrupt", new JsonObject().put("pid", pid),
						new DeliveryOptions().setSendTimeout(2000), (AsyncResult<Message<JsonObject>> ar) -> {
							if (ar.succeeded()) {
								logger.info("{} interrupted.", pid);
								req.response().setStatusCode(200).end();
							} else {
								logger.error(ar.cause().getMessage(), ar.cause());
								req.response().setStatusCode(503).end();
							}
						});
			} catch (NumberFormatException nfe) {
				logger.error("request error: {}", nfe.getMessage());
				req.response().setStatusCode(500).end();
			}

		});
	}

}
