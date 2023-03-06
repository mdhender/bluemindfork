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

import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.mgmt.service.impl.DirEntryWithMailboxSync.Scope;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.directory.service.DirEntryAndValue;

public class OrgUnitSync {

	private IOrgUnits getApi;

	public OrgUnitSync(IOrgUnits getApi) {
		this.getApi = getApi;
	}

	public ItemValue<DirEntryAndValue<OrgUnit>> syncEntry(ItemValue<DirEntry> ivDir, IServerTaskMonitor entryMon,
			IBackupStoreFactory target, BaseContainerDescriptor cont, Scope scope) {
		entryMon.begin(1, null);
		OrgUnit value = getApi.get(ivDir.uid);
		ItemValue<DirEntryAndValue<OrgUnit>> entryAndValue = ItemValue.create(ivDir,
				new DirEntryAndValue<OrgUnit>(ivDir.value, value, null, null));
		if (scope == Scope.Entry) {
			IBackupStore<DirEntryAndValue<OrgUnit>> topicUser = target.forContainer(cont);
			topicUser.store(entryAndValue);

		}

		entryMon.end(true, "processed", "OK");

		return entryAndValue;
	}

}
