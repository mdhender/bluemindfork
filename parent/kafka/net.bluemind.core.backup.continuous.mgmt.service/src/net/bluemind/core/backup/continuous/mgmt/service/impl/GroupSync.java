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

import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.dto.GroupMembership;
import net.bluemind.core.backup.continuous.mgmt.api.BackupSyncOptions;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.IGroupMember;
import net.bluemind.group.api.Member;
import net.bluemind.group.service.IInCoreGroup;

public class GroupSync extends DirEntryWithMailboxSync<Group> {

	public GroupSync(BmContext ctx, BackupSyncOptions opts, IInCoreGroup getApi, DomainApis domainApis) {
		super(ctx, opts, getApi, domainApis);
	}

	@Override
	public ItemValue<DirEntryAndValue<Group>> syncEntry(ItemValue<DirEntry> ivDir, IServerTaskMonitor entryMon,
			IBackupStoreFactory target, BaseContainerDescriptor cont, Scope scope) {
		ItemValue<DirEntryAndValue<Group>> stored = super.syncEntry(ivDir, entryMon, target, cont, scope);
		storeMemberships(target, stored);
		return stored;
	}

	/**
	 * Store the memberships if this group is part of another group
	 * 
	 * @param target
	 * @param stored
	 */
	private void storeMemberships(IBackupStoreFactory target, ItemValue<DirEntryAndValue<Group>> stored) {
		IGroupMember memberOfApi = ctx.provider().instance(IGroup.class, domainUid());
		List<ItemValue<Group>> groups = memberOfApi.memberOf(stored.uid);
		MembershipHook hook = new MembershipHook(target);
		Member asMember = Member.group(stored.uid);
		for (ItemValue<Group> g : groups) {
			GroupMembership gm = hook.createGroupMembership(g.value, asMember, true);
			hook.save(domainUid(), asMember.uid, g.item(), gm);
		}
	}

}
