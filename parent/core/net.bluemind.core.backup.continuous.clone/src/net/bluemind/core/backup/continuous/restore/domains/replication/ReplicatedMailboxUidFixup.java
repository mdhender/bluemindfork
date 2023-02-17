/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.core.backup.continuous.restore.domains.replication;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.bluemind.backend.cyrus.partitions.CyrusUniqueIds;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.IDtoPreProcessor;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.backup.continuous.restore.domains.RestoreState;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.service.common.DefaultFolder;

public class ReplicatedMailboxUidFixup implements IDtoPreProcessor<MailboxReplica> {

	private ItemValue<Domain> dom;
	private ConcurrentHashMap<String, String> uniqueIdMapOldNew;
	private RestoreState state;

	public ReplicatedMailboxUidFixup(RestoreState state, ItemValue<Domain> dom) {
		this.dom = dom;
		this.state = state;
		this.uniqueIdMapOldNew = new ConcurrentHashMap<>();
	}

	private static final Set<String> DEFAULT_USER_FOLDERS = userFolders();

	private static Set<String> userFolders() {
		Set<String> defaultFolders = new HashSet<>();
		defaultFolders.add("INBOX");
		for (String child : DefaultFolder.USER_FOLDERS_NAME) {
			defaultFolders.add(child);
		}
		return defaultFolders;
	}

	private static Set<String> defaultFolders(ItemValue<Mailbox> owner) {
		if (!owner.value.type.sharedNs) {
			return DEFAULT_USER_FOLDERS;
		} else {
			Set<String> defaultFolders = new HashSet<>();
			defaultFolders.add(owner.value.name);
			for (String child : DefaultFolder.MAILSHARE_FOLDERS_NAME) {
				defaultFolders.add(owner.value.name + "/" + child);
			}
			return defaultFolders;
		}
	}

	@Override
	public VersionnedItem<MailboxReplica> fixup(RestoreLogger log, IServiceProvider target, RecordKey k,
			VersionnedItem<MailboxReplica> v) {
		String subtree = k.uid;
		String pref = IMailReplicaUids.subtreePrefix(dom.uid);
		String mboxUid = subtree.substring(pref.length()).replaceAll("^user.", "");
		IMailboxes mboxApi = target.instance(IMailboxes.class, dom.uid);
		ItemValue<Mailbox> owner = mboxApi.getComplete(mboxUid);

		Set<String> defaultFolders = defaultFolders(owner);

		if (!defaultFolders.contains(v.value.fullName)) {
			fixupParent(log, v);
			return v;
		}

		String rightUniqueId = CyrusUniqueIds.forMailbox(dom.uid, owner, v.value.fullName).toString();
		if (v.uid.equals(rightUniqueId)) {
			fixupParent(log, v);
			return v;
		}

		log.monitor().log("FIXUP ReplicatedMailbox {} -> {}", v.uid, rightUniqueId);
		uniqueIdMapOldNew.put(v.uid, rightUniqueId);
		v.uid = rightUniqueId;

		fixupParent(log, v);
		state.mapUid(IMailReplicaUids.mboxRecords(v.uid), IMailReplicaUids.mboxRecords(rightUniqueId));

		return v;
	}

	private void fixupParent(RestoreLogger log, VersionnedItem<MailboxReplica> v) {
		if (v.value.parentUid != null && uniqueIdMapOldNew.containsKey(v.value.parentUid)) {
			log.monitor().log("FIXUP parent ReplicatedMailbox {} -> {}", v.value.parentUid,
					uniqueIdMapOldNew.get(v.value.parentUid));
			v.value.parentUid = uniqueIdMapOldNew.get(v.value.parentUid);
		}
	}

}
