/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.hornetq.client;

import org.vertx.java.core.json.JsonObject;

import com.hazelcast.core.ITopic;

public class Producer {

	private final ITopic<String> hzTopic;

	public Producer(ITopic<String> hzTopic) {
		this.hzTopic = hzTopic;
	}

	public void send(OOPMessage m) {
		hzTopic.publish(m.toString());
	}

	public void send(JsonObject m) {
		hzTopic.publish(m.encode());
	}

	public long sent() {
		return hzTopic.getLocalTopicStats().getPublishOperationCount();
	}

	public void close() {
	}

	public boolean isClosed() {
		return false;
	}

}
