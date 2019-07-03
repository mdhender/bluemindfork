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
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.lib.vertx.VertxPlatform;

public class SubmitCommand implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(SubmitCommand.class);

	public SubmitCommand() {
	}

	@Override
	public void handle(final HttpServerRequest event) {
		event.bodyHandler(new Handler<Buffer>() {

			@Override
			public void handle(Buffer body) {
				JsonObject jso = new JsonObject(body.toString());
				logger.debug("EB cmd.request ! {}", jso);
				event.pause();
				VertxPlatform.eventBus().send("cmd.request", jso, new Handler<Message<Long>>() {

					@Override
					public void handle(Message<Long> ebr) {
						event.resume();
						long pid = ebr.body();
						HttpServerResponse r = event.response();
						if (pid > 0) {
							r.headers().add("Pid", String.valueOf(pid));
							event.response().setStatusCode(201).end();
						} else {
							event.response().setStatusCode(503).end();
						}
					}
				});
			}
		});

	}
}
