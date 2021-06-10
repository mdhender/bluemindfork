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
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.mailbox.api.Mailbox;

public class UidDatalocMapping {

	public static class Replica {
		public final CyrusPartition part;
		public final ItemValue<MailboxReplica> folder;
		public final ItemValue<Mailbox> mbox;
		public final ItemValue<Domain> dom;
		public final String cmdPrefix;

		public Replica(CyrusPartition part, ItemValue<Domain> dom, ItemValue<Mailbox> mbox,
				ItemValue<MailboxReplica> folder) {
			this.part = part;
			this.dom = dom;
			this.mbox = mbox;
			this.folder = folder;
			this.cmdPrefix = applyMailboxPrefixImpl();
		}

		public String cyrusMbox() {
			String fn = folder.value.fullName;
			if (mbox.value.type.sharedNs) {
				if (fn.equals(mbox.value.name)) {
					fn = "";
				} else {
					fn = "." + UTF7Converter.encode(fn.replace('.', '^').replace('/', '.'));
				}
			} else {
				if (fn.equals("INBOX")) {
					fn = "";
				} else {
					fn = "." + UTF7Converter.encode(fn.replace('.', '^').replace('/', '.'));
				}
			}
			return dom.uid + "!" + mbox.value.type.nsPrefix + mbox.value.name.replace('.', '^') + fn;
		}

		private String applyMailboxPrefixImpl() {
			String cmd = "APPLY MAILBOX %(UNIQUEID " + folder.uid + " MBOXNAME \"" + cyrusMbox() + "\" ";
			cmd += "SYNC_CRC 0 SYNC_CRC_ANNOT 0 LAST_UID " + folder.value.lastUid + " HIGHESTMODSEQ "
					+ folder.value.highestModSeq + " RECENTUID 0 ";
			cmd += "RECENTTIME 0 LAST_APPENDDATE " + (folder.value.lastAppendDate.getTime() / 1000)
					+ " POP3_LAST_LOGIN 0 POP3_SHOW_AFTER 0 UIDVALIDITY " + folder.value.uidValidity;
			cmd += " PARTITION " + part.name + " ";
			cmd += "ACL \"" + mbox.value.name + "@" + dom.uid + " lrswipkxtecdan \" ";
			cmd += "OPTIONS PS RECORD (";
			return cmd;
		}
	}

	Map<String, Replica> mapping;
	private Map<String, CyrusPartition> knownPartitions;

	public UidDatalocMapping() {
		mapping = new HashMap<>();
		knownPartitions = new HashMap<>();
	}

	public void put(ItemValue<MailboxReplica> repl, ItemValue<Mailbox> mbox, ItemValue<Domain> dom, CyrusPartition cp) {
		CyrusPartition dedup = knownPartitions.computeIfAbsent(cp.name, k -> cp);
		Replica r = new Replica(dedup, dom, mbox, repl);
		mapping.put(repl.uid, r);
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
