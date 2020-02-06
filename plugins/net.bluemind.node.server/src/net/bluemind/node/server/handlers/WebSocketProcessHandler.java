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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;

public class WebSocketProcessHandler implements Handler<ServerWebSocket> {

	private static final Logger logger = LoggerFactory.getLogger(WebSocketProcessHandler.class);
	private final Vertx vertx;

	public WebSocketProcessHandler(Vertx vertx) {
		this.vertx = vertx;
		logger.debug("created for vertx {}", this.vertx);
	}

	@Override
	public void handle(ServerWebSocket ws) {
		if (!ws.path().equals("/ws")) {
			logger.error("Rejecting websocket at path '{}'", ws.path());
			ws.reject();
		}
		ws.exceptionHandler(t -> {
			logger.error(t.getMessage(), t);
		});
		logger.info("Accepted websocket connection {}", ws);

		ws.frameHandler(new SocketFrameHandler(ws, vertx));
	}

}
