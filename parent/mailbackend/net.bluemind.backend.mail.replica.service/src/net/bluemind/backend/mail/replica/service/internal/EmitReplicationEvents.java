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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

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
		payload.putNumber("modseq", mr.modSeq);
		payload.putNumber("version", upd.version);
		payload.putNumber("itemId", upd.id);
		eb.publish("mailreplica.record.updated." + mboxUniqueId + "." + mr.imapUid, payload);
	}

	public static void recordCreated(String mboxUniqueId, long version, long internalId, long imapUid) {
		JsonObject payload = new JsonObject();
		payload.putNumber("imapUid", imapUid);
		payload.putNumber("version", version);
		payload.putNumber("itemId", internalId);
		eb.publish("mailreplica.record.created." + mboxUniqueId, payload);
		JsonObject copy = payload.copy();
		copy.putString("mailbox", mboxUniqueId);
		copy.putString("container", IMailReplicaUids.mboxRecords(mboxUniqueId));
		eb.publish("mailreplica.newmail", copy);
	}

	public static void mailboxChanged(SubtreeLocation recordsLocation, Container c, String mboxUniqueId, long version,
			long[] allChangedIds, long... createdIds) {
		JsonObject payload = new JsonObject();
		payload.putString("mailbox", mboxUniqueId);
		payload.putString("container", IMailReplicaUids.mboxRecords(mboxUniqueId));
		payload.putNumber("version", version);
		JsonArray changedIds = new JsonArray();
		for (long l : allChangedIds) {
			changedIds.addNumber(l);
		}
		payload.putArray("itemIds", changedIds);
		JsonArray creates = new JsonArray();
		for (long l : createdIds) {
			creates.addNumber(l);
		}
		payload.putArray("createdIds", creates);
		eb.publish(ReplicationEvents.MBOX_UPD_ADDR, payload);
		eb.publish(ReplicationEvents.MBOX_UPD_ADDR + "." + mboxUniqueId, payload);

		// Those events used to be sent from an ips/folderhierarchy combo.
		// Thanks to them, email numbers in 'le bandal' will be refreshed.
		if ("INBOX".equals(recordsLocation.boxName)) {
			eb.publish("bm.mailbox.hook." + c.owner + ".changed",
					new JsonObject().putString("container", c.owner).putString("type", "mailbox"));
		}
		eb.publish("bm.mailbox.hook.changed",
				new JsonObject().putString("container", c.owner).putString("type", "mailbox"));

	}

	public static void subtreeUpdated(String subtreeContainerUid, ItemIdentifier item) {
		subtreeUpdated(subtreeContainerUid, item, false);
	}

	public static void subtreeUpdated(String subtreeContainerUid, ItemIdentifier item, boolean minorChange) {
		logger.debug("****** Subtree updated {}, minorChange: {}", subtreeContainerUid, minorChange);
		JsonObject js = new JsonObject().putString("uid", subtreeContainerUid)//
				.putString("itemUid", item.uid).putNumber("itemId", item.id).putNumber("version", item.version)//
				.putBoolean("minor", minorChange);
		eb.publish(ReplicationEvents.HIER_UPD_ADDR + "." + subtreeContainerUid, js);
		eb.publish(ReplicationEvents.HIER_UPD_ADDR, js);
	}

	public static void recordDeleted(String mailboxUniqueId) {
		eb.publish(ReplicationEvents.REC_DEL_ADDR + mailboxUniqueId, new JsonObject());
	}

	public static void mailboxRootCreated(MailboxReplicaRootDescriptor desc) {
		JsonObject js = new JsonObject();
		js.putString("ns", desc.ns.name()).putString("name", desc.name);
		eb.publish(ReplicationEvents.ROOTS_CREATE_ADDR, js);
	}

}
