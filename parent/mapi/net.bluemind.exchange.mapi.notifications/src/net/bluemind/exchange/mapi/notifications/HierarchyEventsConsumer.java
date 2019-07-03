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
package net.bluemind.exchange.mapi.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.core.container.api.ContainersFlatHierarchyBusAddresses;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.IVerticleFactory;

public class HierarchyEventsConsumer extends BusModBase {

	private static final Logger logger = LoggerFactory.getLogger(HierarchyEventsConsumer.class);

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new HierarchyEventsConsumer();
		}

	}

	public void start() {
		super.start();

		eb.registerHandler(ContainersFlatHierarchyBusAddresses.ALL_HIERARCHY_CHANGES, (Message<JsonObject> msg) -> {
			JsonObject flatNotification = msg.body();
			String owner = flatNotification.getString("owner");
			long version = flatNotification.getNumber("version").longValue();
			JsonObject forMapi = new JsonObject();
			forMapi.putString("owner", owner).putNumber("version", version);
			logger.info("MAPI hierarchy notification {}", forMapi.encode());
			eb.publish(Topic.MAPI_HIERARCHY_NOTIFICATIONS, forMapi);
		});

	}

}
