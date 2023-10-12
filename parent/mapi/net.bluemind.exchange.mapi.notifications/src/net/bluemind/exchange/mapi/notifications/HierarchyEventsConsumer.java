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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
				(Message<JsonObject> msg) -> vertx.executeBlocking(() -> {
					JsonObject flatNotification = msg.body();
					String owner = flatNotification.getString("owner");

					if (FreshOwnerListener.isFreshOwner(owner)) {
						return null;
					}

					long version = flatNotification.getLong("version");
					String domain = flatNotification.getString("domain");

					JsonObject forMapi = new JsonObject();
					forMapi.put("owner", owner).put("domain", domain).put("version", version);
					logger.info("MAPI hierarchy notification owner: {}, version {}", owner, version);
					eb.publish(Topic.MAPI_HIERARCHY_NOTIFICATIONS, forMapi);
					return null;
				}, false));

		vertx.eventBus().consumer(ContainersFlatHierarchyBusAddresses.ALL_HIERARCHY_CHANGES_OPS,
				(Message<JsonObject> msg) -> vertx.executeBlocking(() -> {
					JsonObject flatNotification = msg.body();
					Notification notif = Notification.fromJson(flatNotification);
					if (!notif.container.startsWith(IMailReplicaUids.MAILBOX_RECORDS_PREFIX) //
							|| FreshOwnerListener.isFreshOwner(notif.owner) //
							|| notif.owner.equals(PublicFolders.mailboxGuid(notif.domain))) {
						return null;
					}

					CompletableFuture<Optional<JsonObject>> futureMapiNotif;
					switch (notif.op) {
					case "CREATE":
					case "UPDATE":
						futureMapiNotif = notifyUpdateOnParent(notif);
						break;
					case "DELETE":
						// the thing is deleted, we cannot figure out what was the parent folder anymore
						futureMapiNotif = CompletableFuture.completedFuture(Optional.of(notif.forContainer()));
						break;
					default:
						futureMapiNotif = CompletableFuture.completedFuture(Optional.empty());
						logger.warn("Unknown op: {}", notif.op);
					}

					futureMapiNotif.thenAccept(maybeMapiNotif -> maybeMapiNotif
							.ifPresent(mapiNotif -> eb.publish(Topic.MAPI_HIERARCHY_NOTIFICATIONS, mapiNotif)));
					return null;
				}, false));

	}

	private CompletableFuture<Optional<JsonObject>> notifyUpdateOnParent(Notification notif) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailboxes mboxApi = prov.instance(IMailboxes.class, notif.domain);
		ItemValue<Mailbox> ownerBox = mboxApi.getComplete(notif.owner);
		String subtree = IMailReplicaUids.subtreeUid(notif.domain, ownerBox);

		IDbByContainerReplicatedMailboxes treeApi = prov.instance(IDbByContainerReplicatedMailboxes.class, subtree);
		CompletableFuture<Optional<MailboxFolder>> futureFolder = notifiedFolder(treeApi, notif);
		return futureFolder.thenApply(freshFolder -> freshFolder.map(folder -> {
			String parentUid;
			if (folder.parentUid != null && !isSharedMailboxRoot(ownerBox, treeApi.getComplete(folder.parentUid))) {
				parentUid = IMailReplicaUids.mboxRecords(folder.parentUid);
			} else {
				// assume it is the root, try to locate IPM subtree
				MapiReplica replica = prov.instance(IMapiMailbox.class, notif.domain, notif.owner).get();
				parentUid = (replica != null)
						? MapiFolderContainer.getIdentifier("IPM_SUBTREE", replica.localReplicaGuid)
						: null;
			}
			return (parentUid != null) ? notif.forParentContainer(parentUid) : null;
		}));
	}

	private CompletableFuture<Optional<MailboxFolder>> notifiedFolder(IDbByContainerReplicatedMailboxes treeApi,
			Notification notif) {
		CompletableFuture<Optional<MailboxFolder>> futureReplica = new CompletableFuture<>();
		ItemValue<MailboxFolder> replica = treeApi.getComplete(IMailReplicaUids.uniqueId(notif.container));
		if (replica == null || replica.value == null) {
			return retryNotifiedFolder(treeApi, notif);
		} else {
			futureReplica.complete(Optional.ofNullable(replica.value));
			return futureReplica;
		}
	}

	private CompletableFuture<Optional<MailboxFolder>> retryNotifiedFolder(IDbByContainerReplicatedMailboxes treeApi,
			Notification notif) {
		CompletableFuture<Optional<MailboxFolder>> futureReplica = new CompletableFuture<>();
		vertx.setTimer(100, t -> {
			ItemValue<MailboxFolder> replica = treeApi.getComplete(IMailReplicaUids.uniqueId(notif.container));
			futureReplica.complete(replica != null ? Optional.ofNullable(replica.value) : Optional.empty());
		});
		return futureReplica;
	}

	private boolean isSharedMailboxRoot(ItemValue<Mailbox> ownerBox, ItemValue<MailboxFolder> maybeRoot) {
		return maybeRoot.value.parentUid == null
				&& (ownerBox.value.type.sharedNs && maybeRoot.value.name.equals(ownerBox.value.name));
	}

	private static class Notification {
		private final String domain;
		private final String owner;
		private final String container;
		private final Long id;
		private final String op;
		private final long version;

		public Notification(String domain, String owner, String container, Long id, String op, long version) {
			this.domain = domain;
			this.owner = owner;
			this.container = container;
			this.id = id;
			this.op = op;
			this.version = version;
		}

		public static Notification fromJson(JsonObject json) {
			return new Notification(json.getString("domain"), json.getString("owner"), json.getString("container"),
					json.getLong("id"), json.getString("op"), json.getLong("version"));
		}

		public JsonObject forParentContainer(String parentContainer) {
			return toJson() //
					.put("container", parentContainer) //
					.put("op", "UPDATE") //
					.put("details", new JsonObject().put(op.toLowerCase(), new JsonArray().add(String.valueOf(id))));
		}

		public JsonObject forContainer() {
			return toJson().put("id", id);
		}

		private JsonObject toJson() {
			return new JsonObject() //
					.put("domain", domain) //
					.put("owner", owner) //
					.put("container", container) //
					.put("op", op) //
					.put("version", version);
		}
	}
}
