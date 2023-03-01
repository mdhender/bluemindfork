/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.core.backup.continuous.mgmt.service.impl;

import java.util.List;

import org.slf4j.event.Level;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.dto.ContainerMetadata;
import net.bluemind.core.backup.continuous.mgmt.api.BackupSyncOptions;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.ReservedIds;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.service.common.DefaultFolder;

public class DirEntryWithMailboxSync<T> {

	protected final IRestoreDirEntryWithMailboxSupport<T> getApi;
	protected final BmContext ctx;
	protected final DomainApis domainApis;
	protected final BackupSyncOptions opts;

	public enum Scope {
		Entry, Content
	}

	public DirEntryWithMailboxSync(BmContext ctx, BackupSyncOptions opts, IRestoreDirEntryWithMailboxSupport<T> getApi,
			DomainApis domainApis) {
		this.ctx = ctx;
		this.opts = opts;
		this.getApi = getApi;
		this.domainApis = domainApis;
	}

	public ItemValue<DirEntryAndValue<T>> syncEntry(ItemValue<DirEntry> ivDir, IServerTaskMonitor entryMon,
			IBackupStoreFactory target, BaseContainerDescriptor cont, Scope scope) {
		entryMon.begin(1, null);
		T value = getApi.get(ivDir.uid);
		ItemValue<VCard> vcardUser = domainApis.dirApi.getVCard(ivDir.uid);
		ItemValue<Mailbox> mboxUser = domainApis.mailboxesApi.getComplete(ivDir.uid);
		ItemValue<DirEntryAndValue<T>> entryAndValue = ItemValue.create(ivDir,
				new DirEntryAndValue<T>(ivDir.value, value, vcardUser.value, mboxUser.value));
		preSync(target, domainUid(), entryAndValue);
		if (scope == Scope.Entry) {
			IBackupStore<DirEntryAndValue<T>> topicUser = target.forContainer(cont);
			ReservedIds reserved = reserveBoxes(entryMon, mboxUser);
			ItemValue<DirEntryAndValue<T>> fixed = remap(entryMon, entryAndValue);
			topicUser.store(fixed, reserved);
		}

		entryMon.end(true, "processed", "OK");

		return entryAndValue;
	}

	private ReservedIds reserveBoxes(IServerTaskMonitor mon, ItemValue<Mailbox> mboxUser) {
		ReservedIds reserved = new ReservedIds();
		String subtree = IMailReplicaUids.subtreeUid(domainApis.domain.uid, mboxUser);
		IContainers contApi = ctx.provider().instance(IContainers.class);
		ContainerDescriptor existing = contApi.getIfPresent(subtree);
		if (existing == null) {
			mon.log("Subtree is missing for {}", Level.WARN, mboxUser);
		} else {
			IDbReplicatedMailboxes boxes = ctx.provider().instance(IDbByContainerReplicatedMailboxes.class, subtree);
			if (mboxUser.value.type.sharedNs) {
				String rn = mboxUser.value.name;
				allocBox(mon, reserved, subtree, boxes, rn);
				for (String defFolder : DefaultFolder.MAILSHARE_FOLDERS_NAME) {
					allocBox(mon, reserved, subtree, boxes, rn + "/" + defFolder);
				}
			} else {
				allocBox(mon, reserved, subtree, boxes, "INBOX");
				for (String defFolder : DefaultFolder.USER_FOLDERS_NAME) {
					allocBox(mon, reserved, subtree, boxes, defFolder);
				}
			}
		}
		return reserved;
	}

	private void allocBox(IServerTaskMonitor mon, ReservedIds reserved, String subtree, IDbReplicatedMailboxes boxes,
			String rn) {
		ItemValue<MailboxFolder> box = boxes.byName(rn);
		if (box != null) {
			reserved.add(subtree + ":" + rn, box.internalId);
		} else {
			mon.log("[{}] IDRES Missing {}", Level.WARN, subtree, rn);
		}
	}

	protected void aclsAndSettings(IServerTaskMonitor entryMon, ItemValue<DirEntryAndValue<T>> stored,
			ContainerMetadataBackup cmBack, ItemValue<ContainerHierarchyNode> node) {
		try {
			IContainerManagement mgmt = ctx.provider().instance(IContainerManagement.class, node.value.containerUid);
			String contUid = node.value.containerUid;
			IContainers contApi = ctx.provider().instance(IContainers.class);
			BaseContainerDescriptor bd = contApi.getLight(contUid);
			ContainerMetadata aclMeta = ContainerMetadata.forAcls(bd, mgmt.getAccessControlList());
			cmBack.save(domainUid(), stored.uid, contUid, aclMeta, true);
			ContainerMetadata setMeta = ContainerMetadata.forSettings(bd, mgmt.getSettings());
			cmBack.save(domainUid(), stored.uid, contUid, setMeta, true);
		} catch (ServerFault sf) {
			entryMon.log("WARN error processing " + node.value + ": " + sf.getMessage());
		}
	}

	protected ItemValue<DirEntryAndValue<T>> remap(@SuppressWarnings("unused") IServerTaskMonitor entryMon,
			ItemValue<DirEntryAndValue<T>> orig) {
		return orig;
	}

	public final String domainUid() {
		return domainApis.domain.uid;
	}

	protected void preSync(@SuppressWarnings("unused") IBackupStoreFactory target,
			@SuppressWarnings("unused") String domain,
			@SuppressWarnings("unused") ItemValue<DirEntryAndValue<T>> entryAndValue) {
		// override to create related objects first
	}

	private static record NodeWithFullname(String fullName, ItemValue<ContainerHierarchyNode> node) {

	}

	/**
	 * Make smaller containerId come first in topic
	 * 
	 * @param type
	 * @param mailbox
	 * @param entry
	 * 
	 * @param nodes
	 * @return
	 */
	protected List<ItemValue<ContainerHierarchyNode>> containerIdSort(String type, DirEntry entry,
			List<ItemValue<ContainerHierarchyNode>> nodes) {

		if (IMailReplicaUids.MAILBOX_RECORDS.equals(type)) {
			String subtree = IMailReplicaUids.subtreeUid(domainApis.domain.uid, entry);
			IDbReplicatedMailboxes tree = ctx.provider().instance(IDbByContainerReplicatedMailboxes.class, subtree);

			return nodes.stream().map(ivn -> {
				ItemValue<MailboxFolder> mrFull = tree.getComplete(IMailReplicaUids.uniqueId(ivn.value.containerUid));
				return new NodeWithFullname(mrFull.value.fullName, ivn);
			}).sorted((nd1, nd2) -> nd1.fullName().compareTo(nd2.fullName())).map(NodeWithFullname::node).toList();

		}

		return nodes;

	}

}
