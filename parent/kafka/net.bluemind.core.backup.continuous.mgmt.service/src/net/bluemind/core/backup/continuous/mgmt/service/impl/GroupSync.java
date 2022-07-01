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

import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.dto.GroupMembership;
import net.bluemind.core.backup.continuous.events.ContinuousContenairization;
import net.bluemind.core.backup.continuous.mgmt.api.BackupSyncOptions;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.domain.api.IDomainUids;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.group.service.IInCoreGroup;

public class GroupSync extends DirEntryWithMailboxSync<Group> {

	private static class MembershipHook implements ContinuousContenairization<GroupMembership> {

		private final IBackupStoreFactory target;

		public MembershipHook(IBackupStoreFactory target) {
			this.target = target;
		}

		@Override
		public IBackupStoreFactory targetStore() {
			return target;
		}

		@Override
		public String type() {
			return "memberships";
		}

		private GroupMembership createGroupMembership(Group group, Member member, boolean added) {
			GroupMembership gm = new GroupMembership();
			gm.member = member;
			gm.added = added;
			gm.group = group;
			return gm;
		}

	}

	public GroupSync(BmContext ctx, BackupSyncOptions opts, IInCoreGroup getApi, DomainApis domainApis) {
		super(ctx, opts, getApi, domainApis);
	}

	private void saveMemberships(IServerTaskMonitor entryMon, IBackupStoreFactory target, String domain,
			ItemValue<DirEntryAndValue<Group>> entryAndValue) {
		IGroup grpApi = (IGroup) getApi;
		MembershipHook hook = new MembershipHook(target);
		grpApi.getMembers(entryAndValue.uid).forEach(m -> {
			ItemValue<DirEntryAndValue<Group>> updated = remap(entryMon, entryAndValue);
			GroupMembership gm = hook.createGroupMembership(updated.value.value, m, true);
			hook.save(domain, ContainerUidsMapping.alias(m.uid), updated.item(), gm);
		});
	}

	@Override
	public ItemValue<DirEntryAndValue<Group>> syncEntry(ItemValue<DirEntry> ivDir, IServerTaskMonitor entryMon,
			IBackupStoreFactory target, BaseContainerDescriptor cont, Scope scope) {
		ItemValue<DirEntryAndValue<Group>> stored = super.syncEntry(ivDir, entryMon, target, cont, scope);
		saveMemberships(entryMon, target, domainApis.domain.uid, stored);
		return stored;
	}

	@Override
	protected ItemValue<DirEntryAndValue<Group>> remap(@SuppressWarnings("unused") IServerTaskMonitor entryMon,
			ItemValue<DirEntryAndValue<Group>> orig) {
		ItemValue<DirEntryAndValue<Group>> ret = orig;
		if (orig.value.value.name.equals("user")) {
			ret = ItemValue.create(orig, orig.value);
			ret.uid = IDomainUids.userGroup(domainUid());
			ContainerUidsMapping.map(entryMon, orig.value.entry.entryUid, orig.uid);
			entryMon.log("Remap user group " + orig.value.entry.entryUid + " to " + orig.uid);
			orig.value.entry.entryUid = orig.uid;
		} else if (orig.value.value.name.equals("admin")) {
			ret = ItemValue.create(orig, orig.value);
			ret.uid = IDomainUids.adminGroup(domainUid());
			entryMon.log("Remap admin group " + orig.value.entry.entryUid + " to " + orig.uid);
			ContainerUidsMapping.map(entryMon, orig.value.entry.entryUid, orig.uid);
			orig.value.entry.entryUid = orig.uid;
		}
		return ret;
	}
}
