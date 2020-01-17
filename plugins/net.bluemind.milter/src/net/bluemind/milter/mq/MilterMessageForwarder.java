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
package net.bluemind.milter.mq;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.hornetq.client.vertx.IMessageForwarder;

public class MilterMessageForwarder implements IMessageForwarder {

	private static String eventAddressBase = "bm.milter.mailflow.event.";
	public static String eventAddressChanged = eventAddressBase + "changed";
	public static String domainChanged = "bm.domain.config.changed";

	@Override
	public String getTopic() {
		return Topic.MAILFLOW_NOTIFICATIONS;
	}

	@Override
	public void forward(Vertx vertx, OOPMessage message) {

		String event = message.getStringProperty("event");
		String domain = message.getStringProperty("domainUid");

		JsonObject msg = null;
		switch (event) {
		case "assignments.changed":
			msg = new JsonObject();
			msg.put("domainUid", domain);
			vertx.eventBus().send(eventAddressChanged, msg);
			break;
		case "domain.config.changed":
			msg = new JsonObject();
			msg.put("domainUid", domain);
			vertx.eventBus().send(domainChanged, msg);
			break;
		}
	}

}
