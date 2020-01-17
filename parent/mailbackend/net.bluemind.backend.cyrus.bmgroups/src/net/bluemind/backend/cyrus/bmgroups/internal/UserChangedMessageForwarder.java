/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.cyrus.bmgroups.internal;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.hornetq.client.vertx.IMessageForwarder;

public class UserChangedMessageForwarder implements IMessageForwarder {

	@Override
	public String getTopic() {
		return Topic.DIRECTORY_NOTIFICATIONS;
	}

	@Override
	public void forward(Vertx vertx, OOPMessage message) {
		JsonObject msg = new JsonObject();

		for (String name : message.getPropertyNames()) {
			String m = message.getStringProperty(name);
			if (m != null) {
				msg.put(name.toString(), m);
			}
		}
		if (!msg.containsKey("login")) {
			return;
		}
		vertx.eventBus().publish("invalidate.cache", msg);
	}

}
