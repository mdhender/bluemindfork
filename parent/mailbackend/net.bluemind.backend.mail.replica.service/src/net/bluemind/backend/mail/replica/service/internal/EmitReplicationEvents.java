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
package net.bluemind.backend.mail.replica.service.internal;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore.SubtreeLocation;
import net.bluemind.backend.mail.replica.service.ReplicationEvents;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.lib.vertx.VertxPlatform;

public class EmitReplicationEvents {

	private static final Logger logger = LoggerFactory.getLogger(EmitReplicationEvents.class);
	private static final EventBus eb = VertxPlatform.eventBus();

	public static void recordUpdated(String mboxUniqueId, ItemVersion upd, MailboxRecord mr) {
		JsonObject payload = new JsonObject();
		payload.put("modseq", mr.modSeq);
		payload.put("version", upd.version);
		payload.put("itemId", upd.id);
		eb.publish("mailreplica.record.updated." + mboxUniqueId + "." + mr.imapUid, payload);
		eb.publish("mailreplica.record.changed." + mboxUniqueId + "." + mr.imapUid, payload);
		eb.publish("mailreplica.record.idchanged." + mboxUniqueId + "." + upd.id, payload);
	}

	public static void recordCreated(String mboxUniqueId, long version, long internalId, long imapUid) {
		JsonObject payload = new JsonObject();
		payload.put("imapUid", imapUid);
		payload.put("version", version);
		payload.put("itemId", internalId);
		eb.publish("mailreplica.record.created." + mboxUniqueId, payload);
		eb.publish("mailreplica.record.changed." + mboxUniqueId + "." + imapUid, payload);
		eb.publish("mailreplica.record.idchanged." + mboxUniqueId + "." + internalId, payload);
		JsonObject copy = payload.copy();
		copy.put("mailbox", mboxUniqueId);
		copy.put("container", IMailReplicaUids.mboxRecords(mboxUniqueId));
		eb.publish("mailreplica.newmail", copy);
	}

	public static record ItemIdImapUid(long itemId, long imapUid, Set<String> flags) {

		public static ItemIdImapUid of(long itemId, MailboxRecord m) {
			Set<String> flags = new LinkedHashSet<>();
			m.flags.forEach(f -> flags.add(f.flag));
			return new ItemIdImapUid(itemId, m.imapUid, flags);
		}

		public static ItemIdImapUid[] arrayOf(long itemId, MailboxRecord m) {
			return new ItemIdImapUid[] { of(itemId, m) };
		}
	}

	public static void mailboxChanged(SubtreeLocation recordsLocation, Container c, String mboxUniqueId, long version,
			ItemIdImapUid[] allChangedIds, long... createdIds) {
		JsonObject payload = new JsonObject();
		payload.put("mailbox", mboxUniqueId);
		payload.put("container", IMailReplicaUids.mboxRecords(mboxUniqueId));
		payload.put("version", version);
		payload.put("owner", c.owner);
		payload.put("domain", c.domainUid);
		JsonArray changedIds = new JsonArray();
		JsonArray flaggedImapUids = new JsonArray();
		for (ItemIdImapUid l : allChangedIds) {
			changedIds.add(l.itemId());
			JsonArray flags = new JsonArray();
			l.flags.forEach(flags::add);
			JsonObject imapChange = new JsonObject().put("imap", l.imapUid).put("flags", flags).put("iid", l.itemId);
			flaggedImapUids.add(imapChange);
		}
		payload.put("itemIds", changedIds);
		payload.put("imapChanges", flaggedImapUids);
		JsonArray creates = new JsonArray();
		for (long l : createdIds) {
			creates.add(l);
		}
		payload.put("createdIds", creates);
		eb.publish(ReplicationEvents.MBOX_UPD_ADDR, payload);
		eb.publish(ReplicationEvents.MBOX_UPD_ADDR + "." + mboxUniqueId, payload);

		// Those events used to be sent from an ips/folderhierarchy combo.
		// Thanks to them, email numbers in 'le bandal' will be refreshed.
		if ("INBOX".equals(recordsLocation.boxName)) {
			eb.publish("bm.mailbox.hook." + c.owner + ".changed",
					new JsonObject().put("container", c.owner).put("type", "mailbox"));
		}
		eb.publish("bm.mailbox.hook.changed", new JsonObject().put("container", c.owner).put("type", "mailbox"));

	}

	public static void mailboxCreated(String subtreeContainerUid, String folderName, ItemIdentifier item) {
		logger.debug("****** mailboxCreated {}, folderName: {}", subtreeContainerUid, folderName);
		JsonObject js = new JsonObject().put("uid", subtreeContainerUid)//
				.put("itemUid", item.uid).put("itemId", item.id).put("version", item.version);

		eb.publish(ReplicationEvents.MBOX_CREATE_ADDR + "." + subtreeContainerUid + "." + folderName, js);
	}

	public static void subtreeUpdated(String subtreeContainerUid, String owner, ItemIdentifier item) {
		subtreeUpdated(subtreeContainerUid, owner, item, false);
	}

	public static void subtreeUpdated(String subtreeContainerUid, String owner, ItemIdentifier item,
			boolean minorChange) {
		logger.debug("****** Subtree updated {}, minorChange: {}", subtreeContainerUid, minorChange);
		JsonObject js = new JsonObject().put("uid", subtreeContainerUid)//
				.put("itemUid", item.uid).put("itemId", item.id).put("version", item.version)//
				.put("owner", owner).put("minor", minorChange);
		eb.publish(ReplicationEvents.HIER_UPD_ADDR + "." + subtreeContainerUid, js);
		eb.publish(ReplicationEvents.HIER_UPD_ADDR, js);
	}

	public static void recordDeleted(String mailboxUniqueId) {
		eb.publish(ReplicationEvents.REC_DEL_ADDR + mailboxUniqueId, new JsonObject());
	}

	public static void mailboxRootCreated(MailboxReplicaRootDescriptor desc) {
		JsonObject js = new JsonObject();
		js.put("ns", desc.ns.name()).put("name", desc.name);
		eb.publish(ReplicationEvents.ROOTS_CREATE_ADDR, js);
	}

}
