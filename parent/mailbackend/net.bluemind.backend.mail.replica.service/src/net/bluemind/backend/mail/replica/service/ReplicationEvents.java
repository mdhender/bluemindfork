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
package net.bluemind.backend.mail.replica.service;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.google.common.annotations.VisibleForTesting;

import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.lib.vertx.VertxPlatform;

@VisibleForTesting
public class ReplicationEvents {

	private static final Logger logger = LoggerFactory.getLogger(ReplicationEvents.class);
	private static final EventBus eb = VertxPlatform.eventBus();
	public static final String HIER_UPD_ADDR = "mailreplica.hierarchy.updated";
	public static final String MBOX_UPD_ADDR = "mailreplica.mailbox.updated";
	public static final String REC_DEL_ADDR = "mailreplica.record.deleted.";
	public static final String ROOTS_CREATE_ADDR = "mailreplica.roots.create";

	public static class ItemChange {
		public long version;
		public long internalId;
		public long latencyMs;

		public static ItemChange create(long version, long internalId, long latency) {
			ItemChange ic = new ItemChange();
			ic.version = version;
			ic.internalId = internalId;
			ic.latencyMs = latency;
			return ic;
		}
	}

	public static CompletableFuture<ItemChange> onRecordUpdate(String mboxUniqueId, long imapUid) {
		long time = System.currentTimeMillis();
		CompletableFuture<ItemChange> done = new CompletableFuture<>();
		String addr = "mailreplica.record.updated." + mboxUniqueId + "." + imapUid;
		Handler<Message<JsonObject>> handler = new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> msg) {
				long latency = System.currentTimeMillis() - time;
				JsonObject change = msg.body();
				done.complete(ItemChange.create(change.getNumber("version").longValue(),
						change.getNumber("itemId").longValue(), latency));
				eb.unregisterHandler(addr, this);
			}
		};
		eb.registerLocalHandler(addr, handler);
		return done;
	}

	public static CompletableFuture<ItemChange> onRecordCreate(String mboxUniqueId, long expectedId) {
		long time = System.currentTimeMillis();
		CompletableFuture<ItemChange> done = new CompletableFuture<>();
		String addr = "mailreplica.record.created." + mboxUniqueId;
		Handler<Message<JsonObject>> handler = new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> msg) {
				long latency = System.currentTimeMillis() - time;
				JsonObject change = msg.body();
				long version = change.getNumber("version").longValue();
				long itemId = change.getNumber("itemId").longValue();
				if (itemId == expectedId) {
					logger.info("itemCreated id {}, version {}", itemId, version);
					done.complete(ItemChange.create(version, itemId, latency));
					eb.unregisterHandler(addr, this);
				} else {
					logger.info("We got a create, but it is for {} instead of {}", itemId, expectedId);
				}
			}
		};
		eb.registerLocalHandler(addr, handler);
		return done;
	}

	public static CompletableFuture<ItemIdentifier> onSubtreeUpdate(String subtreeContainerUid) {
		CompletableFuture<ItemIdentifier> ret = new CompletableFuture<>();
		String addr = HIER_UPD_ADDR + "." + subtreeContainerUid;
		Handler<Message<JsonObject>> handler = new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject idJs = event.body();
				ItemIdentifier iid = ItemIdentifier.of(idJs.getString("itemUid"), idJs.getNumber("itemId").longValue(),
						idJs.getNumber("version").longValue());
				ret.complete(iid);
				eb.unregisterHandler(addr, this);
			}
		};
		eb.registerLocalHandler(addr, handler);
		return ret;
	}

	public static CompletableFuture<Long> onMailboxChanged(String mboxUniqueId) {
		CompletableFuture<Long> ret = new CompletableFuture<>();
		String addr = MBOX_UPD_ADDR + "." + mboxUniqueId;
		Handler<Message<JsonObject>> handler = new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				ret.complete(event.body().getNumber("version").longValue());
				eb.unregisterHandler(addr, this);
			}
		};
		eb.registerLocalHandler(addr, handler);
		return ret;
	}

	public static CompletableFuture<Void> onRecordDeleted(String mailboxUniqueId) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		String addr = REC_DEL_ADDR + mailboxUniqueId;
		Handler<Message<JsonObject>> handler = new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				ret.complete(null);
				eb.unregisterHandler(addr, this);
			}
		};
		eb.registerLocalHandler(addr, handler);
		return ret;
	}

	public static CompletableFuture<MailboxReplicaRootDescriptor> onMailboxRootCreated() {
		CompletableFuture<MailboxReplicaRootDescriptor> ret = new CompletableFuture<>();
		String addr = ROOTS_CREATE_ADDR;
		Handler<Message<JsonObject>> handler = new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject js = event.body();
				MailboxReplicaRootDescriptor root = MailboxReplicaRootDescriptor
						.create(Namespace.valueOf(js.getString("ns")), js.getString("name"));
				ret.complete(root);
				eb.unregisterHandler(addr, this);
			}
		};
		eb.registerLocalHandler(addr, handler);
		return ret;
	}

}
