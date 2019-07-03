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
package net.bluemind.device.service.internal;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.hornetq.client.Topic;

public class DeviceEventProducer {

	public void deleted(String uid) {
		sendMessage(uid, "unwipe");
	}

	public void wipe(String uid) {
		sendMessage(uid, "wipe");
	}

	public void unwipe(String uid) {
		sendMessage(uid, "unwipe");
	}

	private void sendMessage(String identifier, String operation) {
		Producer producer = MQ.getProducer(Topic.HOOKS_DEVICE);
		if (producer != null) {
			OOPMessage m = MQ.newMessage();
			m.putStringProperty("identifier", identifier);
			m.putStringProperty("operation", operation);
			producer.send(m);
		}
	}
}
