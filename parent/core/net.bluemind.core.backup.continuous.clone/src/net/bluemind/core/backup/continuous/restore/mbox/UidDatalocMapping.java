/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.restore.mbox;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.Mailbox;

public class UidDatalocMapping {

	public static class Replica {
		public final CyrusPartition part;
		public final ItemValue<MailboxReplica> folder;
		public final ItemValue<Mailbox> mbox;
		public final ItemValue<Domain> dom;

		public Replica(CyrusPartition part, ItemValue<Domain> dom, ItemValue<Mailbox> mbox,
				ItemValue<MailboxReplica> folder) {
			this.part = part;
			this.dom = dom;
			this.mbox = mbox;
			this.folder = folder;
		}
	}

	Map<String, Replica> mapping;
	private Map<String, CyrusPartition> knownPartitions;

	public UidDatalocMapping() {
		mapping = new HashMap<>();
		knownPartitions = new HashMap<>();
	}

	public Replica put(ItemValue<MailboxReplica> repl, ItemValue<Mailbox> mbox, ItemValue<Domain> dom,
			CyrusPartition cp) {
		CyrusPartition dedup = knownPartitions.computeIfAbsent(cp.name, k -> cp);
		Replica replica = new Replica(dedup, dom, mbox, repl);
		mapping.put(repl.uid, replica);
		return replica;
	}

	public int size() {
		return mapping.size();
	}

	public Replica get(String container) {
		return mapping.get(container);
	}

	public void dump() {
		for (Entry<String, Replica> e : mapping.entrySet()) {
			System.err.println("* " + e.getKey() + " v: " + e.getValue());
		}
	}

}
