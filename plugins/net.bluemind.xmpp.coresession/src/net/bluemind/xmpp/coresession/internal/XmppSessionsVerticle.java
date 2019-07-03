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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;

public class XmppSessionsVerticle extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(XmppSessionsVerticle.class);

	private Map<String, XmppSessionSockets> activeSessions = new HashMap<>();
	private EventBus eventBus;

	public XmppSessionsVerticle() {
	}

	@Override
	public void start() {
		eventBus = getVertx().eventBus();

		eventBus.registerHandler("xmpp/sessions-manager:open", openSessionRegisterHandler);

		eventBus.registerHandler("xmpp/sessions-manager:internal-close", closeSessionRegisterHandler);

		eventBus.registerHandler("core.user.sessionLogout", new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject value = event.body();
				if (!value.getString("origin").equals("bm-hps")) {
					return;
				}
				logger.debug("received logged out notification {}", event.body());
				eventBus.send("xmpp/session/" + event.body().getString("sessionId") + ":close", new JsonObject());

			}
		});

		MQ.init(new MQ.IMQConnectHandler() {

			@Override
			public void connected() {
				MQ.registerConsumer(Topic.XIVO_PHONE_STATUS, new XivoPhoneStatusHandler(eventBus));
			}
		});
	}

	private Handler<Message<JsonObject>> closeSessionRegisterHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(final Message<JsonObject> clientMessage) {
			final String sessionId = clientMessage.body().getString("sessionId");
			final XmppSessionSockets sockets = activeSessions.get(sessionId);
			if (sockets == null) {
				clientMessage.reply();
				return;
			}

			activeSessions.remove(sessionId);
			sockets.unregisterAll();
		}
	};

	private Handler<Message<JsonObject>> openSessionRegisterHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(final Message<JsonObject> clientMessage) {

			logger.debug("message body {}", clientMessage.body());

			final String sessionId = clientMessage.body().getString("sessionId");
			String socketId = clientMessage.body().getString("sockId");

			logger.debug("receive session:open from {} (websocket: {})", sessionId, socketId);

			if (activeSessions.get(sessionId) != null) {
				XmppSessionSockets sockets = activeSessions.get(sessionId);

				logger.debug("session {} is already active", sessionId);
				sockets.register(socketId);
				clientMessage.reply();
				return;
			}

			final XmppSessionSockets sockets = new XmppSessionSockets(vertx, sessionId);
			sockets.register(socketId);
			activeSessions.put(sessionId, sockets);

			getVertx().eventBus().send("xmpp/session:initiate", clientMessage.body(),
					new Handler<Message<JsonObject>>() {

				@Override
				public void handle(Message<JsonObject> event) {

					if (event.body().getNumber("status").intValue() == 0) {
						clientMessage.reply();
					} else {
						logger.error("initialization failed, remove sessionid {} from active sessions", sessionId);
						sockets.unregisterAll();
						activeSessions.remove(sessionId);
						clientMessage.reply(event.body());
					}

				}
			});
		}

	};

}
