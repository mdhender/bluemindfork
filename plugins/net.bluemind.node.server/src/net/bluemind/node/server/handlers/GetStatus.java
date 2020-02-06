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
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;

public class GetStatus implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(GetStatus.class);

	public GetStatus() {
	}

	@Override
	public void handle(final HttpServerRequest event) {
		event.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void v) {
				long pid = Long.parseLong(event.params().get("reqId"));
				JsonObject jso = new JsonObject().put("pid", pid);
				event.pause();
				VertxPlatform.eventBus().request("cmd.status", jso, (AsyncResult<Message<JsonObject>> jsRep) -> {
					String json = jsRep.result().body().encode();
					logger.debug("status received: {}", json);
					HttpServerResponse r = event.response();
					r.headers().add("Content-Type", "application/json");
					r.end(json);
					event.resume();
				});
			}

		});

	}
}
