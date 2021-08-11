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
package net.bluemind.core.backup.continuous.events;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.dto.GroupMembership;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.group.hook.GroupMessage;
import net.bluemind.group.hook.IGroupHook;

public class MembershipsContinuousHook implements IGroupHook {

	private static final Logger logger = LoggerFactory.getLogger(MembershipsContinuousHook.class);

	@Override
	public void onGroupCreated(GroupMessage created) throws ServerFault {
	}

	@Override
	public void onGroupUpdated(GroupMessage previous, GroupMessage current) throws ServerFault {
	}

	@Override
	public void onGroupDeleted(GroupMessage deleted) throws ServerFault {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IGroup grpApi = prov.instance(IGroup.class, deleted.container.domainUid);
		List<Member> fullList = grpApi.getMembers(deleted.group.uid);
		saveMembers(deleted, fullList, false);
	}

	@Override
	public void onAddMembers(GroupMessage group) throws ServerFault {
		saveMembers(group, group.members, true);
	}

	@Override
	public void onRemoveMembers(GroupMessage group) throws ServerFault {
		saveMembers(group, group.members, false);
	}

	private void saveMembers(GroupMessage group, List<Member> members, boolean added) {
		members.forEach(member -> {
			ContainerDescriptor metaDesc = ContainerDescriptor.create(group.container.domainUid + "_membership",
					group.container.domainUid + " membership", member.uid, "memberships", group.container.domainUid,
					true);
			GroupMembership gm = new GroupMembership();
			gm.member = member;
			gm.added = added;
			ItemValue<GroupMembership> iv = ItemValue.create(group.group.uid, gm);
			iv.internalId = iv.uid.hashCode();
			IBackupStore<GroupMembership> store = DefaultBackupStore.store().<GroupMembership>forContainer(metaDesc);
			store.store(iv);
			logger.info("Saved memberships for {}", group.group.uid);
		});
	}

}
