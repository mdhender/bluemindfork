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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;

public class SubmitCommand implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(SubmitCommand.class);

	@Override
	public void handle(final HttpServerRequest event) {
		event.bodyHandler((Buffer body) -> {
			JsonObject jso = new JsonObject(body.toString());
			logger.debug("EB cmd.request ! {}", jso);
			event.pause();
			VertxPlatform.eventBus().request("cmd.request", jso, new Handler<AsyncResult<Message<Long>>>() {

				@Override
				public void handle(AsyncResult<Message<Long>> ebr) {

					event.resume();
					if (ebr.failed()) {
						event.response().setStatusCode(503).end();
						return;
					}

					long pid = ebr.result().body();
					HttpServerResponse r = event.response();
					if (pid > 0) {
						r.headers().add("Pid", String.valueOf(pid));
						event.response().setStatusCode(201).end();
					} else {
						event.response().setStatusCode(503).end();
					}
				}
			});

		});

	}
}
