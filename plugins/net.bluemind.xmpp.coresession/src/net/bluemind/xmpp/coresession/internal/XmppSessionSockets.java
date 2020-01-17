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
package net.bluemind.xmpp.coresession.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

public class XmppSessionSockets {

	private static final Logger logger = LoggerFactory.getLogger(XmppSessionSockets.class);

	private long timer = -1;
	private String sessionId;
	private Map<String, MessageConsumer<Boolean>> sockets = new HashMap<>();

	private EventBus eventBus;

	private Vertx vertx;

	public XmppSessionSockets(Vertx vertx, String sessionId) {
		this.vertx = vertx;
		this.eventBus = vertx.eventBus();
		this.sessionId = sessionId;
	}

	public void unregisterAll() {
		// copy keySet before iterate on it
		for (String socketId : new HashSet<>(sockets.keySet())) {
			unregister(socketId);
		}
	}

	private void unregister(String socketId) {
		MessageConsumer<Boolean> cons = sockets.get(socketId);
		if (cons != null) {
			cons.unregister();
			sockets.remove(socketId);
		}

	}

	public void register(String socketId) {
		if (sockets.containsKey(socketId)) {
			logger.debug("socket {} already registred for session {}", socketId, sessionId);
			return;
		}

		if (timer != -1) {
			vertx.cancelTimer(timer);
			timer = -1;
		}
		Handler<Message<Boolean>> handler = closedHandler(socketId);
		MessageConsumer<Boolean> cons = eventBus.consumer("websocket." + socketId + ".closed", handler);
		sockets.put(socketId, cons);
	}

	private Handler<Message<Boolean>> closedHandler(final String socketId) {
		return new Handler<Message<Boolean>>() {

			@Override
			public void handle(Message<Boolean> event) {
				logger.debug("socket {} closed for session {}", socketId, sessionId);

				unregister(socketId);
				if (sockets.isEmpty()) {
					logger.debug("no more socket for session {}, wait one second and close xmpp session", sessionId);
					// after 1s without connection we close xmpp session
					timer = vertx.setTimer(1000, new Handler<Long>() {

						@Override
						public void handle(Long event) {
							timer = -1;
							closeSession();
						}
					});

				} else {
					logger.debug("active sockets {} for session {}", sockets.keySet(), sessionId);
				}
			}
		};
	}

	private void closeSession() {
		eventBus.send("xmpp/session/" + sessionId + ":close", new JsonObject());
	}
}
