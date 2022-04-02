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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;

import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.mgmt.api.BackupSyncOptions;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.mailshare.api.Mailshare;

public class MailshareSync extends DirEntryWithMailboxSync<Mailshare> {

	private static final List<String> TYPE_ORDER = Lists.newArrayList(//
			IMailReplicaUids.REPLICATED_MBOXES, //
			IMailReplicaUids.MAILBOX_RECORDS //
	);

	private final DomainKafkaState kafkaState;

	public MailshareSync(BmContext ctx, BackupSyncOptions opts, DomainKafkaState domKafkaState,
			IRestoreDirEntryWithMailboxSupport<Mailshare> getApi, DomainApis domainApis) {
		super(ctx, opts, getApi, domainApis);
		this.kafkaState = domKafkaState;
	}

	@Override
	public ItemValue<DirEntryAndValue<Mailshare>> syncEntry(ItemValue<DirEntry> ivDir, IServerTaskMonitor entryMon,
			IBackupStoreFactory target, BaseContainerDescriptor cont, Scope scope) {

		IContainersFlatHierarchy hierApi = ctx.provider().instance(IContainersFlatHierarchy.class, domainUid(),
				ivDir.uid);
		List<ItemValue<ContainerHierarchyNode>> nodes = hierApi.list();

		entryMon.begin(10 * nodes.size() + 1.0, "process " + nodes.size() + " container(s)");
		ItemValue<DirEntryAndValue<Mailshare>> stored = super.syncEntry(ivDir, entryMon.subWork(1), target, cont,
				scope);

		if (scope == Scope.Content) {
			processContainers(entryMon, target, nodes, stored);
		}

		return stored;
	}

	private void processContainers(IServerTaskMonitor entryMon, IBackupStoreFactory target,
			List<ItemValue<ContainerHierarchyNode>> nodes, ItemValue<DirEntryAndValue<Mailshare>> stored) {
		// group the nodes by types in a multimap
		ListMultimap<String, ItemValue<ContainerHierarchyNode>> mmap = MultimapBuilder.hashKeys().arrayListValues()
				.build();
		nodes.forEach(iv -> mmap.put(iv.value.containerType, iv));

		// warn for types we can't sync
		HashSet<String> notHandled = Sets.newHashSet(mmap.keySet());
		notHandled.removeAll(TYPE_ORDER);
		if (!notHandled.isEmpty()) {
			entryMon.log("WARN not handled types: " + notHandled.stream().collect(Collectors.joining(", ")));
		}
		ContainerMetadataBackup cmBack = new ContainerMetadataBackup(target);
		for (String type : TYPE_ORDER) {
			for (ItemValue<ContainerHierarchyNode> node : Optional.ofNullable(mmap.get(type))
					.orElseGet(Collections::emptyList)) {
				ContainerState state = kafkaState.containerState(node.value.containerUid);
				ContainerSync syncSupport = ContainerSyncRegistry.forNode(ctx, node, stored, domainApis.domain);
				syncSupport.sync(state, target, entryMon.subWork(10));

				aclsAndSettings(entryMon, stored, cmBack, node);
			}
		}
	}

}
