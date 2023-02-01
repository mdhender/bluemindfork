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
package net.bluemind.core.backup.continuous.mgmt.service.impl;

import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.dto.GroupMembership;
import net.bluemind.core.backup.continuous.events.ContinuousContenairization;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.Member;

class MembershipHook implements ContinuousContenairization<GroupMembership> {

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

	GroupMembership createGroupMembership(Group group, Member member, boolean added) {
		GroupMembership gm = new GroupMembership();
		gm.member = member;
		gm.added = added;
		gm.group = group;
		return gm;
	}

}