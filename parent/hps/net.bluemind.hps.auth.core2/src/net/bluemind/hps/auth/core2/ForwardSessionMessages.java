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
package net.bluemind.hps.auth.core2;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.hornetq.client.vertx.IMessageForwarder;

public class ForwardSessionMessages implements IMessageForwarder {

	@Override
	public String getTopic() {
		return Topic.CORE_SESSIONS;
	}

	@Override
	public void forward(Vertx vertx, OOPMessage message) {
		JsonObject vmessage = new JsonObject();

		for (String name : message.getPropertyNames()) {
			String m = message.getStringProperty(name);
			if (m != null) {
				vmessage.put(name.toString(), m);
			}
		}
		vertx.eventBus().publish(Topic.CORE_SESSIONS, vmessage);
	}

}
