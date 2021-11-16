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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class MailboxEventsConsumer extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(MailboxEventsConsumer.class);

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new MailboxEventsConsumer();
		}

	}

	@Override
	public void start() {
		EventBus eb = vertx.eventBus();
		eb.consumer("mailreplica.mailbox.updated", (Message<JsonObject> msg) -> vertx.executeBlocking(prom -> {
			JsonObject replicaNotif = msg.body();
			String cont = replicaNotif.getString("container");
			Set<Long> changed = new HashSet<>();
			Set<Long> created = new HashSet<>();
			JsonArray changedIds = replicaNotif.getJsonArray("itemIds");
			JsonArray createdIds = replicaNotif.getJsonArray("createdIds");
			long messageId = 1L;
			int len = changedIds.size();
			for (int i = 0; i < len; i++) {
				changed.add(changedIds.getLong(i));
			}
			for (int i = 0; i < createdIds.size(); i++) {
				created.add(createdIds.getLong(i));
			}
			changed.removeAll(created);
			len = changed.size();
			if (len > 0) {
				messageId = changedIds.getLong(0);
			} else {
				logger.debug("Using fake messageId 1L on {}", cont);
			}

			if (len > 0 || created.isEmpty()) {
				JsonObject asMapiNotif = new JsonObject();
				asMapiNotif.put("containerUid", cont);
				asMapiNotif.put("itemIds", changedIds);
				asMapiNotif.put("messageClass", "IPM.Note");
				asMapiNotif.put("internalId", messageId);
				asMapiNotif.put("operation", CrudOperation.Update.name());
				if (logger.isDebugEnabled()) {
					logger.debug("Re-publish notification {} for mapi on container {}", replicaNotif.encode(), cont);
				}
				eb.publish(Topic.MAPI_ITEM_NOTIFICATIONS, asMapiNotif);
			}

			if (!created.isEmpty()) {
				JsonObject asCreateNotif = new JsonObject();
				asCreateNotif.put("containerUid", cont);
				asCreateNotif.put("messageClass", "IPM.Note");
				asCreateNotif.put("internalId", created.iterator().next());
				asCreateNotif.put("itemIds", createdIds);
				asCreateNotif.put("operation", CrudOperation.Create.name());
				eb.publish(Topic.MAPI_ITEM_NOTIFICATIONS, asCreateNotif);
			}

		}, false));

	}

}
