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
package net.bluemind.exchange.mapi.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.IVerticleFactory;

public class ItemNotificationVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ItemNotificationVerticle.class);

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new ItemNotificationVerticle();
		}

	}

	@Override
	public void start() {
		EventBus eb = vertx.eventBus();
		MQ.init(() -> {
			final Producer producer = MQ.registerProducer(Topic.MAPI_ITEM_NOTIFICATIONS);
			eb.consumer(Topic.MAPI_ITEM_NOTIFICATIONS, (Message<JsonObject> msg) -> {
				JsonObject body = msg.body();
				OOPMessage mqMsg = MQ.newMessage();
				IContainers contApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IContainers.class);
				String contUid = body.getString("containerUid");
				ContainerDescriptor descriptor = contApi.get(contUid);
				mqMsg.putStringProperty("containerUid", contUid);
				mqMsg.putStringProperty("owner", descriptor.owner);
				mqMsg.putStringProperty("domain", descriptor.domainUid);
				mqMsg.putLongProperty("internalId", body.getLong("internalId"));
				mqMsg.putStringProperty("messageClass", body.getString("messageClass"));
				mqMsg.putStringProperty("operation", body.getString("operation"));
				producer.send(mqMsg);
				if (logger.isDebugEnabled()) {
					logger.debug("ItemNotification to MQ: {}", mqMsg.toJson().encode());
				}
			});

			final Producer hierProducer = MQ.registerProducer(Topic.MAPI_HIERARCHY_NOTIFICATIONS);
			eb.consumer(Topic.MAPI_HIERARCHY_NOTIFICATIONS, (Message<JsonObject> msg) -> {
				JsonObject body = msg.body();
				hierProducer.send(body);
				if (logger.isDebugEnabled()) {
					logger.debug("HierarchyNotification to MQ: {}", body.encode());
				}
			});

			final Producer dioProducer = MQ.registerProducer(Topic.MAPI_DELEGATION_NOTIFICATIONS);
			eb.consumer(Topic.MAPI_DELEGATION_NOTIFICATIONS, (Message<JsonObject> msg) -> {
				dioProducer.send(msg.body());
			});

			final Producer pfAclUpdateProducer = MQ.registerProducer(Topic.MAPI_PF_ACL_UPDATE);
			eb.consumer(Topic.MAPI_PF_ACL_UPDATE, (Message<JsonObject> msg) -> {
				pfAclUpdateProducer.send(msg.body());
			});
		});

	}

}
