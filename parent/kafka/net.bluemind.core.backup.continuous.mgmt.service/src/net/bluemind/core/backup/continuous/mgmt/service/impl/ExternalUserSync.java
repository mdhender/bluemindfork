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

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.dto.GroupMembership;
import net.bluemind.core.backup.continuous.mgmt.service.impl.DirEntryWithMailboxSync.Scope;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.externaluser.api.ExternalUser;
import net.bluemind.externaluser.api.IExternalUser;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.Member;

public class ExternalUserSync {

	private IExternalUser getApi;
	private DomainApis domainApis;

	public ExternalUserSync(IExternalUser getApi, DomainApis domainApis) {
		this.getApi = getApi;
		this.domainApis = domainApis;
	}

	public ItemValue<DirEntryAndValue<ExternalUser>> syncEntry(ItemValue<DirEntry> ivDir, IServerTaskMonitor entryMon,
			IBackupStoreFactory target, BaseContainerDescriptor cont, Scope scope) {
		entryMon.begin(1, null);
		ExternalUser value = getApi.get(ivDir.uid);
		ItemValue<VCard> vcardUser = domainApis.dirApi.getVCard(ivDir.uid);
		ItemValue<DirEntryAndValue<ExternalUser>> entryAndValue = ItemValue.create(ivDir,
				new DirEntryAndValue<ExternalUser>(ivDir.value, value, vcardUser.value, null));
		if (scope == Scope.Entry) {
			IBackupStore<DirEntryAndValue<ExternalUser>> topicUser = target.forContainer(cont);
			topicUser.store(entryAndValue);

			storeMemberships(target, entryAndValue);
		}

		entryMon.end(true, "processed", "OK");

		return entryAndValue;
	}

	private void storeMemberships(IBackupStoreFactory target, ItemValue<DirEntryAndValue<ExternalUser>> stored) {
		List<ItemValue<Group>> groups = getApi.memberOf(stored.uid);
		MembershipHook hook = new MembershipHook(target);
		Member asMember = Member.externalUser(stored.uid);
		for (ItemValue<Group> g : groups) {

			GroupMembership gm = hook.createGroupMembership(g.value, asMember, true);
			hook.save(domainUid(), asMember.uid, g.item(), gm);
		}
	}

	private String domainUid() {
		return domainApis.domain.uid;
	}

}
