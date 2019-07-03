package net.bluemind.serialization.client;
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


import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.hornetq.client.vertx.IMessageForwarder;

public class HollowMessageForwarder implements IMessageForwarder {

	public static final String dataSetChanged = "hollow.dataset.version.announcement";

	@Override
	public String getTopic() {
		return Topic.DATA_SERIALIZATION_NOTIFICATIONS;
	}

	@Override
	public void forward(Vertx vertx, OOPMessage message) {

		String action = message.getStringProperty("action");
		String dataset = message.getStringProperty("dataset");
		long version = message.getLongProperty("version");

		JsonObject msg = null;
		switch (action) {
		case "version_announcement":
			msg = new JsonObject();
			msg.putString("dataset", dataset);
			msg.putNumber("version", version);
			vertx.eventBus().publish(dataSetChanged, msg);
			break;
		}

	}

}
