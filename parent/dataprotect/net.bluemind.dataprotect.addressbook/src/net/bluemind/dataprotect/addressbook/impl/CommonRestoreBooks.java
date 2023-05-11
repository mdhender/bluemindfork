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
package net.bluemind.dataprotect.addressbook.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCardChanges;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.dataprotect.common.restore.CommonRestoreEntities;
import net.bluemind.dataprotect.common.restore.RestoreRestorableItem;

public class CommonRestoreBooks extends CommonRestoreEntities {

	protected CommonRestoreBooks(RestoreRestorableItem item, BmContext back, BmContext live) {
		super(item, back, live);
	}

	@Override
	public void restoreEntities(List<String> allUids) {
		restoreEntities(allUids, item.entryUid(), item.entryUid());
	}

	@Override
	public void restoreEntities(List<String> allUids, String backUid, String liveUid) {
		IAddressBook backAddressBookApi = back.provider().instance(IAddressBook.class, backUid);
		IAddressBook liveAddressBookApi = live.provider().instance(IAddressBook.class, liveUid);

		for (List<String> batch : Lists.partition(allUids, 1000)) {
			List<ItemValue<VCard>> cards = backAddressBookApi.multipleGet(batch);
			VCardChanges changes = VCardChanges.create(
					cards.stream().map(e -> VCardChanges.ItemAdd.create(e.uid, e.value)).collect(Collectors.toList()),
					Collections.emptyList(), Collections.emptyList());
			ContainerUpdatesResult updatesResult = liveAddressBookApi.updates(changes);
			if (updatesResult.errors != null && !updatesResult.errors.isEmpty()) {
				item.errors.addAll(updatesResult.errors);
			}
			item.monitor.progress(batch.size(), null);
		}
	}

}
