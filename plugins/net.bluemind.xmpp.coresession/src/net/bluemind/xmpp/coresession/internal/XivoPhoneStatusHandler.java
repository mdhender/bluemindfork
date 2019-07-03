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
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.OutOfProcessMessageHandler;

public class XivoPhoneStatusHandler implements OutOfProcessMessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(XivoPhoneStatusHandler.class);

	private EventBus eventBus;
	private String busAddr;

	public XivoPhoneStatusHandler(EventBus eventBus) {
		this.eventBus = eventBus;
		this.busAddr = "xmpp/xivo/status";
	}

	@Override
	public void handle(OOPMessage message) {
		String operation = message.getStringProperty("operation");
		if ("xivo.updatePhoneStatus".equals(operation)) {
			String latd = message.getStringProperty("latd");
			String status = message.getStringProperty("status");

			JsonObject msg = new JsonObject();
			msg.putString("category", "xivo");
			msg.putString("latd", latd);
			msg.putString("status", status);

			logger.debug(msg.encodePrettily());

			eventBus.publish(busAddr, msg);
		}

	}
}
