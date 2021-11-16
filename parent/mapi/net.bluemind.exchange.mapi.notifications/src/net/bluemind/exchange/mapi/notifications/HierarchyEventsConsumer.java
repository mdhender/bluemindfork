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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.container.api.ContainersFlatHierarchyBusAddresses;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.exchange.mapi.api.MapiFolderContainer;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.exchange.publicfolders.common.PublicFolders;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class HierarchyEventsConsumer extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(HierarchyEventsConsumer.class);

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new HierarchyEventsConsumer();
		}

	}

	@Override
	public void start() {

		EventBus eb = vertx.eventBus();
		vertx.eventBus().consumer(ContainersFlatHierarchyBusAddresses.ALL_HIERARCHY_CHANGES,
				(Message<JsonObject> msg) -> vertx.executeBlocking(prom -> {
					JsonObject flatNotification = msg.body();
					String owner = flatNotification.getString("owner");
					long version = flatNotification.getLong("version");
					String domain = flatNotification.getString("domain");

					JsonObject forMapi = new JsonObject();
					forMapi.put("owner", owner).put("domain", domain).put("version", version);
					logger.info("MAPI hierarchy notification owner: {}, version {}", owner, version);
					eb.publish(Topic.MAPI_HIERARCHY_NOTIFICATIONS, forMapi);
				}, false)

		);

		vertx.eventBus().consumer(ContainersFlatHierarchyBusAddresses.ALL_HIERARCHY_CHANGES_OPS,
				(Message<JsonObject> msg) -> vertx.executeBlocking(prom -> {
					JsonObject flatNotification = msg.body();
					String container = flatNotification.getString("container");

					if (!container.startsWith(IMailReplicaUids.MAILBOX_RECORDS_PREFIX)) {
						return;
					}

					String owner = flatNotification.getString("owner");
					String domain = flatNotification.getString("domain");

					if (owner.equals(PublicFolders.mailboxGuid(domain))) {
						return;
					}

					long version = flatNotification.getLong("version");
					String op = flatNotification.getString("op");

					JsonObject forMapi = new JsonObject();
					forMapi.put("owner", owner).put("domain", domain).put("version", version);

					switch (op) {
					case "CREATE":
					case "UPDATE":
						// transform create/update notifications into an update on the parent uniqueId
						ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
						IMailboxes mboxApi = prov.instance(IMailboxes.class, domain);
						ItemValue<Mailbox> ownerBox = mboxApi.getComplete(owner);
						String subtree = IMailReplicaUids.subtreeUid(domain, ownerBox);
						IDbByContainerReplicatedMailboxes treeApi = prov
								.instance(IDbByContainerReplicatedMailboxes.class, subtree);
						ItemValue<MailboxFolder> freshFolder = treeApi
								.getComplete(IMailReplicaUids.uniqueId(container));
						if (freshFolder.value.parentUid != null
								&& !isSharedMailboxRoot(ownerBox, treeApi.getComplete(freshFolder.value.parentUid))) {

							forMapi.put("details",
									new JsonObject().put(op.toLowerCase(), new JsonArray().add(container)));
							container = IMailReplicaUids.mboxRecords(freshFolder.value.parentUid);
							op = "UPDATE";
						} else {
							// assume it is the root, try to locate IPM subtree
							IMapiMailbox mapiApi = prov.instance(IMapiMailbox.class, domain, owner);
							MapiReplica replica = mapiApi.get();
							if (replica == null) {
								logger.warn("parentless create of {} in {}, null replica, drop MAPI notification.",
										container, ownerBox);
								return;
							} else {
								forMapi.put("details",
										new JsonObject().put(op.toLowerCase(), new JsonArray().add(container)));
								container = MapiFolderContainer.getIdentifier("IPM_SUBTREE", replica.localReplicaGuid);
								op = "UPDATE";
							}
						}
						break;
					case "DELETE":
						// the thing is deleted, we cannot figure out what was the parent folder anymore
						break;
					default:
						logger.warn("Unknown op: {}", op);
					}
					forMapi.put("container", container).put("op", op);

					logger.info("MAPI hierarchy container {} ({}) notification owner: {}, version {}", container, op,
							owner, version);
					eb.publish(Topic.MAPI_HIERARCHY_NOTIFICATIONS, forMapi);

				}, false));

	}

	private boolean isSharedMailboxRoot(ItemValue<Mailbox> ownerBox, ItemValue<MailboxFolder> maybeRoot) {
		return maybeRoot.value.parentUid == null && (//
		(ownerBox.value.type.sharedNs && maybeRoot.value.name.equals(ownerBox.value.name)));
	}

}
