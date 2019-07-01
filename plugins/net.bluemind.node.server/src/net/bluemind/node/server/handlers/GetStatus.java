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
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;

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
				JsonObject jso = new JsonObject().putNumber("pid", pid);
				event.pause();
				VertxPlatform.eventBus().send("cmd.status", jso, new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> jsRep) {
						String json = jsRep.body().encode();
						logger.debug("status received: {}", json);
						HttpServerResponse r = event.response();
						r.headers().add("Content-Type", "application/json");
						r.end(json);
						event.resume();
					}
				});
			}

		});

	}
}
