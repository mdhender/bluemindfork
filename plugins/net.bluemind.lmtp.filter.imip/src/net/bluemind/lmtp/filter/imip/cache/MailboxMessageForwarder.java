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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lmtp.filter.imip.cache;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.hornetq.client.vertx.IMessageForwarder;

public class MailboxMessageForwarder implements IMessageForwarder {

	public static String mailboxChanged = "bm.mailbox.changed";

	@Override
	public String getTopic() {
		return Topic.MAILBOX_NOTIFICATIONS;
	}

	@Override
	public void forward(Vertx vertx, OOPMessage message) {
		String mailbox = message.getStringProperty("mailbox");
		String domain = message.getStringProperty("domain");

		JsonObject msg = new JsonObject();
		msg.put("mailbox", mailbox);
		msg.put("domain", domain);

		vertx.eventBus().send(mailboxChanged, msg);

	}

}
