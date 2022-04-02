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

import java.util.stream.Collectors;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.dto.ContainerMetadata;
import net.bluemind.core.backup.continuous.mgmt.api.BackupSyncOptions;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.mailbox.api.Mailbox;

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
		System.err.println("*** SYNC " + scope + " " + ivDir.uid);
		T value = getApi.get(ivDir.uid);
		ItemValue<VCard> vcardUser = domainApis.dirApi.getVCard(ivDir.uid);
		ItemValue<Mailbox> mboxUser = domainApis.mailboxesApi.getComplete(ivDir.uid);
		ItemValue<DirEntryAndValue<T>> entryAndValue = ItemValue.create(ivDir,
				new DirEntryAndValue<T>(ivDir.value, value, vcardUser.value, mboxUser.value));
		preSync(target, domainUid(), entryAndValue);
		if (scope == Scope.Entry) {
			IBackupStore<DirEntryAndValue<T>> topicUser = target.forContainer(cont);
			topicUser.store(remap(entryMon, entryAndValue));
		}

		entryMon.end(true, "processed", "OK");

		return entryAndValue;
	}

	protected void aclsAndSettings(IServerTaskMonitor entryMon, ItemValue<DirEntryAndValue<T>> stored,
			ContainerMetadataBackup cmBack, ItemValue<ContainerHierarchyNode> node) {
		try {
			IContainerManagement mgmt = ctx.provider().instance(IContainerManagement.class, node.value.containerUid);
			String uidAlias = ContainerUidsMapping.alias(node.value.containerUid);
			ContainerMetadata aclMeta = ContainerMetadata.forAcls(uidAlias,
					mgmt.getAccessControlList().stream().map(ace -> {
						ace.subject = ContainerUidsMapping.alias(ace.subject);
						return ace;
					}).collect(Collectors.toList()));
			cmBack.save(domainUid(), stored.uid, uidAlias, aclMeta, true);
			ContainerMetadata setMeta = ContainerMetadata.forSettings(uidAlias, mgmt.getSettings());
			cmBack.save(domainUid(), stored.uid, uidAlias, setMeta, true);
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

}
