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
package net.bluemind.mailflow.service.internal;

import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;

public class EmitMailflowEvent {

	public static void registerHandler() {

		MQ.init(new MQ.IMQConnectHandler() {

			@Override
			public void connected() {
				MQ.registerProducer(Topic.MAILFLOW_NOTIFICATIONS);
			}
		});

		VertxPlatform.eventBus().consumer("core.mailflow.context.changed", (message) -> {
			OOPMessage cm = new OOPMessage((JsonObject) message.body());
			MQ.getProducer(Topic.MAILFLOW_NOTIFICATIONS).send(cm);
		});
	}

	public static void invalidateConfig(String domainUid) {
		JsonObject message = new JsonObject();
		message.put("domainUid", domainUid);
		message.put("event", "assignments.changed");
		VertxPlatform.eventBus().publish("core.mailflow.context.changed", message);
	}

	public static void invalidateDomainAliasCache(String domainUid) {
		JsonObject message = new JsonObject();
		message.put("domainUid", domainUid);
		message.put("event", "domain.config.changed");
		VertxPlatform.eventBus().publish("core.mailflow.context.changed", message);
	}

}
