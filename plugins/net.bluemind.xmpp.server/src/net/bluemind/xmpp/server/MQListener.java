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
package net.bluemind.xmpp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.OutOfProcessMessageHandler;
import net.bluemind.lib.vertx.VertxPlatform;

public class MQListener implements OutOfProcessMessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(MQListener.class);
	private BMSessionManager bmSessionManager;

	public MQListener() {

	}

	@Override
	public void handle(OOPMessage msg) {
		logger.debug("FROM MQ: " + msg);
		String op = msg.getStringProperty("operation");
		if (op == null) {
			logger.warn("operation attribute is not in message.");
			return;
		}

		if (op.startsWith("domain.")) {
			VertxPlatform.eventBus().publish("refreshDomains", Boolean.TRUE);
		} else if (op.startsWith("presence.")) {
			bmSessionManager.updatePresence(msg);
		} else if (op.startsWith("xivo.")) {
			bmSessionManager.updatePhoneStatus(msg);
		}
	}

	public void setSessionManager(BMSessionManager bmSessionManager) {
		this.bmSessionManager = bmSessionManager;
	}

	public BMSessionManager getSessionManager() {
		return bmSessionManager;
	}

}
