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
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;

public class Executions implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(Executions.class);

	@Override
	public void handle(HttpServerRequest req) {
		req.endHandler(v -> {
			JsonObject js = new JsonObject();
			js.put("group", req.params().get("group"));
			js.put("name", req.params().get("name"));
			VertxPlatform.eventBus().request("cmd.executions", js, new DeliveryOptions().setSendTimeout(2000),
					(AsyncResult<Message<JsonObject>> ar) -> {
						if (ar.succeeded()) {
							Message<JsonObject> msg = ar.result();
							String json = msg.body().encode();
							logger.debug("status received: {}", json);
							HttpServerResponse r = req.response();
							r.headers().add("Content-Type", "application/json");
							r.end(json);
						} else {
							logger.error(ar.cause().getMessage(), ar.cause());
							req.response().setStatusCode(503).end();
						}
					});

		});
	}

}
