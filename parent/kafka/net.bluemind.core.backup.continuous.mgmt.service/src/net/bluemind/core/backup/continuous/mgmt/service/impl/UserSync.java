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

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarViewUids;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.events.ContinuousContenairization;
import net.bluemind.core.backup.continuous.mgmt.api.BackupSyncOptions;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.exchange.mapi.api.IMapiFoldersMgmt;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.exchange.mapi.api.MapiFolder;
import net.bluemind.exchange.mapi.api.MapiFolderContainer;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.notes.api.INoteUids;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;
import net.bluemind.user.api.UserSettings;

public class UserSync extends DirEntryWithMailboxSync<User> {

	private static final List<String> TYPE_ORDER = Lists.newArrayList(//
			IMailReplicaUids.REPLICATED_MBOXES, //
			IMailReplicaUids.MAILBOX_RECORDS, //
			ICalendarUids.TYPE, //
			ICalendarViewUids.TYPE, //
			ITodoUids.TYPE, //
			INoteUids.TYPE, //
			IAddressBookUids.TYPE //
	);

	private static final List<String> SKIPPED_TYPES = Lists.newArrayList(//
			MapiFolderContainer.TYPE, //
			IMailboxAclUids.TYPE, //
			IDeferredActionContainerUids.TYPE//
	);

	private final DomainKafkaState kafkaState;

	public class MapiReplicaContinuousBackup implements ContinuousContenairization<MapiReplica> {
		private IBackupStoreFactory target;

		public MapiReplicaContinuousBackup(IBackupStoreFactory tgt) {
			this.target = tgt;
		}

		@Override
		public String type() {
			return "mapi_artifacts";
		}

		@Override
		public IBackupStoreFactory targetStore() {
			return target;
		}
	}

	public class MapiFolderContinuousBackup implements ContinuousContenairization<MapiFolder> {

		private IBackupStoreFactory target;

		public MapiFolderContinuousBackup(IBackupStoreFactory tgt) {
			this.target = tgt;
		}

		@Override
		public String type() {
			return "mapi_artifacts";
		}

		@Override
		public IBackupStoreFactory targetStore() {
			return target;
		}
	}

	public UserSync(BmContext ctx, BackupSyncOptions opts, DomainKafkaState domKafkaState,
			IRestoreDirEntryWithMailboxSupport<User> getApi, DomainApis domainApis) {
		super(ctx, opts, getApi, domainApis);
		this.kafkaState = domKafkaState;
	}

	@Override
	public ItemValue<DirEntryAndValue<User>> syncEntry(ItemValue<DirEntry> ivDir, IServerTaskMonitor entryMon,
			IBackupStoreFactory target, BaseContainerDescriptor cont, Scope scope) {

		IContainersFlatHierarchy hierApi = ctx.provider().instance(IContainersFlatHierarchy.class, domainUid(),
				ivDir.uid);
		List<ItemValue<ContainerHierarchyNode>> nodes = hierApi.list();

		entryMon.begin(10 * nodes.size() + 3.0, "process " + nodes.size() + " container(s)");
		ItemValue<DirEntryAndValue<User>> stored = super.syncEntry(ivDir, entryMon.subWork(1), target, cont, scope);

		if (scope == Scope.Content) {
			processSettings(ivDir, target, cont);

			processMapiArtifacts(ivDir, target, nodes);

			processContainers(entryMon, target, nodes, stored);
		}
		return stored;
	}

	private void processMapiArtifacts(ItemValue<DirEntry> ivDir, IBackupStoreFactory target,
			List<ItemValue<ContainerHierarchyNode>> nodes) {
		IMapiMailbox mapiBox = ctx.provider().instance(IMapiMailbox.class, domainUid(), ivDir.uid);
		MapiReplica replica = mapiBox.get();
		if (replica != null) {
			new MapiReplicaContinuousBackup(target).save(domainUid(), ivDir.uid, "replica", replica, true);
			MapiFolderContinuousBackup folderBack = new MapiFolderContinuousBackup(target);
			nodes.stream().filter(iv -> iv.value.containerType.equals(MapiFolderContainer.TYPE))
					.forEach(mapiFolderNode -> {
						IMapiFoldersMgmt folders = ctx.provider().instance(IMapiFoldersMgmt.class, domainUid(),
								ivDir.uid);
						MapiFolder folder = folders.get(mapiFolderNode.value.containerUid);
						folderBack.save(domainUid(), ivDir.uid, folder.containerUid, folder, true);
					});
		}
	}

	private void processSettings(ItemValue<DirEntry> ivDir, IBackupStoreFactory target, BaseContainerDescriptor cont) {
		IUserSettings settingsApi = ctx.provider().instance(IUserSettings.class, domainUid());
		ItemValue<UserSettings> setsItem = ItemValue.create(ivDir, UserSettings.of(settingsApi.get(ivDir.uid)));
		target.<UserSettings>forContainer(cont).store(setsItem);
	}

	private void processContainers(IServerTaskMonitor entryMon, IBackupStoreFactory target,
			List<ItemValue<ContainerHierarchyNode>> nodes, ItemValue<DirEntryAndValue<User>> stored) {
		// group the nodes by types in a multimap
		ListMultimap<String, ItemValue<ContainerHierarchyNode>> mmap = MultimapBuilder.hashKeys().arrayListValues()
				.build();
		nodes.forEach(iv -> mmap.put(iv.value.containerType, iv));

		// warn for types we can't sync
		HashSet<String> notHandled = Sets.newHashSet(mmap.keySet());
		notHandled.removeAll(TYPE_ORDER);
		notHandled.removeAll(SKIPPED_TYPES);
		if (!notHandled.isEmpty()) {
			entryMon.log("WARN not handled types: " + notHandled.stream().collect(Collectors.joining(", ")));
		}

		for (String type : TYPE_ORDER) {
			if (opts.skipTypes.contains(type)) {
				entryMon.subWork(10).end(true, "Skipped type " + type + " as asked by user", "OK");
				continue;
			}
			ContainerMetadataBackup cmBack = new ContainerMetadataBackup(target);

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
