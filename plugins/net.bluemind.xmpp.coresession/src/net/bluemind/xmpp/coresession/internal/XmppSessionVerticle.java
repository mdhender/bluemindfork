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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.network.topology.Topology;

public class XmppSessionVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(XmppSessionVerticle.class);

	private static final int PORT = 5222;

	@Override
	public void start() {
		getVertx().eventBus().consumer("xmpp/session:initiate", initiateHandler());
	}

	private Handler<Message<JsonObject>> initiateHandler() {

		return (Message<JsonObject> event) -> {

			String latd = event.body().getString("latd");
			if (Strings.isNullOrEmpty(latd)) {
				logger.error("latd is null or empty {}", event.body());
				event.reply(XmppSessionMessage.sessionConnectionFailed());
				return;
			}

			String[] landD = latd.split("@");
			if (landD.length != 2) {
				logger.error("latd not valid : {}", latd);
				event.reply(XmppSessionMessage.sessionConnectionFailed());
				return;
			}

			String host = getXmppHost();
			if (host == null) {
				event.reply(XmppSessionMessage.sessionAuthenticationFailed());
			}

			logger.debug("open xmpp session for {} on {}", latd, host);

			String login = landD[0];
			String domain = landD[1];

			XmppSession session = null;
			try {
				session = XmppSession.create(host, PORT, domain, event.body().getString("sessionId"), getVertx());

			} catch (Exception e) {
				// session init failed...
				logger.error("error during connection initialization ", e);

				event.reply(XmppSessionMessage.sessionConnectionFailed());

				return;
			}

			try {
				session.authenticate(login, event.body().getString("sessionId"));

				event.reply(XmppSessionMessage.sessionOk());

			} catch (Exception e) {
				session.close();
				// authentication failed
				logger.error("authentication failed ", e);
				event.reply(XmppSessionMessage.sessionAuthenticationFailed());

			}

		};
	}

	private String getXmppHost() {
		return Topology.getIfAvailable()
				.map(t -> t.anyIfPresent("bm/xmpp").map(s -> s.value.address()).orElse("127.0.0.1"))
				.orElse("127.0.0.1");
	}
}
