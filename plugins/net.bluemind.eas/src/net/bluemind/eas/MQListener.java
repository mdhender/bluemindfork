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
package net.bluemind.eas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.command.provision.WipedDevices;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.OutOfProcessMessageHandler;

public class MQListener implements OutOfProcessMessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(MQListener.class);

	public MQListener() {

	}

	@Override
	public void handle(OOPMessage msg) {
		String op = msg.getStringProperty("operation");
		if (op == null) {
			logger.warn("operation attribute is not in message.");
			return;
		}
		if ("core.state.running".equals(op)) {
			// nothing
		} else if ("wipe".equals(op)) {
			String id = msg.getStringProperty("identifier");
			WipedDevices.wipe(id);
		} else if ("unwipe".equals(op)) {
			String id = msg.getStringProperty("identifier");
			WipedDevices.unwipe(id);
		} else {
			logger.warn("Unhandled operation: " + op);
		}
	}

}
