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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.Topic;

public class EventsDropboxHandler implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(EventsDropboxHandler.class);

	private final EventBus eb;

	public EventsDropboxHandler(EventBus eb) {
		this.eb = eb;
	}

	@Override
	public void handle(final HttpServerRequest req) {
		logger.info("handle {}", req.path());
		req.bodyHandler(new Handler<Buffer>() {

			@Override
			public void handle(Buffer event) {
				JsonObject jso = null;
				try {
					jso = new JsonObject(event.toString());
				} catch (Exception e) {
					logger.error("Sending bad request: {}", e.getMessage());
					req.response().setStatusCode(400).end();
					return;
				}

				String domain = req.params().get("domain");
				jso.put("domain", domain);
				if (logger.isDebugEnabled()) {
					logger.debug("[{}] json: {}", Thread.currentThread().getName(), jso.encodePrettily());
				}

				eb.request(Topic.XIVO_PHONE_STATUS, jso, new DeliveryOptions().setSendTimeout(5000),
						new Handler<AsyncResult<Message<JsonObject>>>() {

							@Override
							public void handle(AsyncResult<Message<JsonObject>> fwd) {
								if (fwd.failed()) {
									req.response().setStatusCode(500).end();
								} else {
									logger.info("[{}] Forwarded to hornetq", Thread.currentThread().getName());
									req.response().end();
								}
							}
						});
			}
		});
	}

}
