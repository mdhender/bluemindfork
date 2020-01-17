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
package net.bluemind.milter.action.signature;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.hornetq.client.vertx.IMessageForwarder;

public class MilterMessageForwarder implements IMessageForwarder {

	public static String eventAddressChanged = "dir.changed";

	@Override
	public String getTopic() {
		return Topic.DIRECTORY_NOTIFICATIONS;
	}

	@Override
	public void forward(Vertx vertx, OOPMessage message) {

		String event = message.getStringProperty("event");
		String domain = message.getStringProperty("domain");

		JsonObject msg = null;
		switch (event) {
		case "dir.changed":
			msg = new JsonObject();
			msg.put("domain", domain);
			msg.put("uid", message.getStringProperty("uid"));
			vertx.eventBus().send(eventAddressChanged, msg);
			break;
		}
	}

}
