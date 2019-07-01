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
package net.bluemind.cti.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.cti.service.internal.CTIPresenceHandler;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.OutOfProcessMessageHandler;
import net.bluemind.lib.vertx.VertxPlatform;

public class HornetQListener implements OutOfProcessMessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(HornetQListener.class);

	public HornetQListener() {

	}

	@Override
	public void handle(OOPMessage msg) {
		String op = msg.getStringProperty("operation");
		if (op == null || !op.startsWith("im.")) {
			logger.warn("operation attribute is not in message or unsupported '{}'", op);
			return;
		}
		VertxPlatform.eventBus().publish(CTIPresenceHandler.ADDR, msg.toJson());
	}
}
