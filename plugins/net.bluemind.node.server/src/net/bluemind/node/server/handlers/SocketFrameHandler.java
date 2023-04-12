/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.node.server.handlers;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;

public class SocketFrameHandler implements Handler<WebSocketFrame> {

	private static final Logger logger = LoggerFactory.getLogger(SocketFrameHandler.class);
	private StringBuilder current;
	private final String addr;
	private final DeliveryOptions replyDeliveryOptions = new DeliveryOptions()
			.setSendTimeout(TimeUnit.SECONDS.toMillis(40));
	private final DeliveryOptions commandDeliveryOptions = new DeliveryOptions()
			.setSendTimeout(TimeUnit.SECONDS.toMillis(120));

	public SocketFrameHandler(ServerWebSocket ws, Vertx vertx) {
		current = new StringBuilder();
		this.addr = ws.textHandlerID() + ".bm";
		MessageConsumer<String> withReply = vertx.eventBus().localConsumer(addr);
		withReply.handler(strMsg -> {
			ws.writeFinalTextFrame(strMsg.body());
			if (ws.writeQueueFull()) {
				ws.drainHandler(v -> strMsg.reply(null, replyDeliveryOptions));
			} else {
				strMsg.reply(null, replyDeliveryOptions);
			}
		});
	}

	@Override
	public void handle(WebSocketFrame event) {
		if (!event.isText()) {
			// ignore
			return;
		}
		current.append(event.textData());
		if (!event.isFinal()) {
			return;
		}
		JsonObject msg = new JsonObject(current.toString());
		current = new StringBuilder();
		process(msg);
	}

	private void process(JsonObject msg) {
		if (logger.isDebugEnabled()) {
			logger.debug("WS - C: {}", msg.encodePrettily());
		}
		long rid = msg.getLong("ws-rid", 0L);
		if (rid > 0) {
			msg.put("ws-target", addr);
			VertxPlatform.eventBus().request("cmd.request", msg, commandDeliveryOptions, ar -> {
				if (ar.failed()) {
					logger.error("cmd.request failed: {}", ar.cause().getMessage(), ar.cause());
				}
			});
		} else {
			logger.warn("Command over websocket without ws-rid.");
		}
	}

}
