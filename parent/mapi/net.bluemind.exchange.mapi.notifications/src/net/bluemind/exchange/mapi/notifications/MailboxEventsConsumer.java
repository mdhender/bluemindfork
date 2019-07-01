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

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.IVerticleFactory;

public class MailboxEventsConsumer extends BusModBase {

	private static final Logger logger = LoggerFactory.getLogger(MailboxEventsConsumer.class);

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new MailboxEventsConsumer();
		}

	}

	public void start() {
		super.start();

		eb.registerHandler("mailreplica.mailbox.updated", (Message<JsonObject> msg) -> {
			JsonObject replicaNotif = msg.body();
			String cont = replicaNotif.getString("container");
			Set<Long> changed = new HashSet<>();
			Set<Long> created = new HashSet<>();
			JsonArray changedIds = replicaNotif.getArray("itemIds");
			JsonArray createdIds = replicaNotif.getArray("createdIds");
			long messageId = 1L;
			int len = changedIds.size();
			for (int i = 0; i < len; i++) {
				Number n = changedIds.get(i);
				changed.add(n.longValue());
			}
			for (int i = 0; i < createdIds.size(); i++) {
				Number n = createdIds.get(i);
				created.add(n.longValue());
			}
			changed.removeAll(created);
			len = changed.size();
			if (len > 0) {
				Number n = changedIds.get(0);
				messageId = n.longValue();
			} else {
				logger.debug("Using fake messageId 1L on {}", cont);
			}

			if (len > 0 || created.isEmpty()) {
				JsonObject asMapiNotif = new JsonObject();
				asMapiNotif.putString("containerUid", cont);
				asMapiNotif.putArray("itemIds", changedIds);
				asMapiNotif.putString("messageClass", "IPM.Note");
				asMapiNotif.putNumber("internalId", messageId);
				asMapiNotif.putString("operation", CrudOperation.Update.name());
				if (logger.isDebugEnabled()) {
					logger.debug("Re-publish notification {} for mapi on container {}", replicaNotif.encode(), cont);
				}
				eb.publish(Topic.MAPI_ITEM_NOTIFICATIONS, asMapiNotif);
			}

			if (!created.isEmpty()) {
				JsonObject asCreateNotif = new JsonObject();
				asCreateNotif.putString("containerUid", cont);
				asCreateNotif.putString("messageClass", "IPM.Note");
				asCreateNotif.putNumber("internalId", created.iterator().next());
				asCreateNotif.putArray("itemIds", createdIds);
				asCreateNotif.putString("operation", CrudOperation.Create.name());
				eb.publish(Topic.MAPI_ITEM_NOTIFICATIONS, asCreateNotif);
			}

		});

	}

}
