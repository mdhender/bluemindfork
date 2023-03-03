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
import java.util.Set;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarViewUids;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.dto.GroupMembership;
import net.bluemind.core.backup.continuous.events.ContinuousContenairization;
import net.bluemind.core.backup.continuous.events.RolesContinuousHook.DirEntryRoleContinuousBackup;
import net.bluemind.core.backup.continuous.mgmt.api.BackupSyncOptions;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IOwnerSubscriptionUids;
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.device.api.IDeviceUids;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.exchange.mapi.api.IMapiFoldersMgmt;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.exchange.mapi.api.MapiFolder;
import net.bluemind.exchange.mapi.api.MapiFolderContainer;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroupMember;
import net.bluemind.group.api.Member;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.notes.api.INoteUids;
import net.bluemind.role.hook.RoleEvent;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;
import net.bluemind.user.api.UserSettings;
import net.bluemind.user.service.IInCoreUser;
import net.bluemind.webappdata.api.IWebAppDataUids;

public class UserSync extends DirEntryWithMailboxSync<User> {

	private static final List<String> TYPE_ORDER = Lists.newArrayList(//
			IMailboxAclUids.TYPE, //
			IMailReplicaUids.REPLICATED_MBOXES, //
			IMailReplicaUids.MAILBOX_RECORDS, //
			ICalendarUids.TYPE, //
			ICalendarViewUids.TYPE, //
			ITodoUids.TYPE, //
			INoteUids.TYPE, //
			IAddressBookUids.TYPE, //
			IDeviceUids.TYPE, //
			IWebAppDataUids.TYPE, //
			IOwnerSubscriptionUids.TYPE//
	);

	private static final List<String> SKIPPED_TYPES = Lists.newArrayList(//
			MapiFolderContainer.TYPE, //
			IDeferredActionContainerUids.TYPE//
	);

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
		super(ctx, opts, getApi, domainApis, domKafkaState);
	}

	@Override
	protected ItemValue<DirEntryAndValue<User>> remap(IServerTaskMonitor entryMon,
			ItemValue<DirEntryAndValue<User>> orig) {

		IInCoreUser inCore = ctx.provider().instance(IInCoreUser.class, domainUid());
		ItemValue<User> fullWithPasswordHash = inCore.getFull(orig.uid);
		orig.value.value = fullWithPasswordHash.value;
		return orig;
	}

	@Override
	protected List<String> containerTypeOrder() {
		return TYPE_ORDER;
	}

	@Override
	protected List<String> containerTypeToSkip() {
		return SKIPPED_TYPES;
	}

	@Override
	protected void entrySync(IBackupStoreFactory target, ItemValue<DirEntryAndValue<User>> stored) {
		storeMemberships(target, stored);
		pushRoles(target, stored);
	}

	@Override
	protected void contentSync(ItemValue<DirEntry> ivDir, IBackupStoreFactory target, BaseContainerDescriptor cont,
			List<ItemValue<ContainerHierarchyNode>> nodes) {

		processSettings(ivDir, target, cont);

		processMapiArtifacts(ivDir, target, nodes);

	}

	private void pushRoles(IBackupStoreFactory target, ItemValue<DirEntryAndValue<User>> fixed) {
		IUser roleApi = ctx.provider().instance(IUser.class, domainUid());
		Set<String> roles = roleApi.getRoles(fixed.uid);
		DirEntryRoleContinuousBackup rolesBackup = new DirEntryRoleContinuousBackup(target);
		RoleEvent re = new RoleEvent(domainApis.domain.uid, fixed.uid, fixed.value.entry.kind, roles);
		rolesBackup.onRolesSet(re);
	}

	private void storeMemberships(IBackupStoreFactory target, ItemValue<DirEntryAndValue<User>> stored) {
		IGroupMember memberOfApi = ctx.provider().instance(IUser.class, domainUid());
		List<ItemValue<Group>> groups = memberOfApi.memberOf(stored.uid);
		MembershipHook hook = new MembershipHook(target);
		Member asMember = Member.user(stored.uid);
		for (ItemValue<Group> g : groups) {

			GroupMembership gm = hook.createGroupMembership(g.value, asMember, true);
			hook.save(domainUid(), asMember.uid, g.item(), gm);
		}
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

}
