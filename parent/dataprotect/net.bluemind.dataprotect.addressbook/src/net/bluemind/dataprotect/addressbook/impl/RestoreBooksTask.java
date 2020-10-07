/* BEGIN LICENSE
* Copyright © Blue Mind SAS, 2012-2016
*
* This file is part of BlueMind. BlueMind is a messaging and collaborative
* solution.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of either the GNU Affero General Public License as
* published by the Free Software Foundation (version 3 of the License).
*
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCardChanges;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.service.BackupDataProvider;

public class RestoreBooksTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(RestoreBooksTask.class);

	private final DataProtectGeneration backup;
	private final Restorable item;

	public RestoreBooksTask(DataProtectGeneration backup, Restorable item) {
		this.backup = backup;
		this.item = item;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(10,
				String.format("Starting restore for UID: %s from old UID: %s ", item.liveEntryUid(), item.entryUid));
		try (BackupDataProvider bdp = new BackupDataProvider(null, SecurityContext.SYSTEM, monitor)) {
			BmContext back = bdp.createContextWithData(backup, item);
			BmContext live = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

			IContainers liveContApi = live.provider().instance(IContainers.class);
			List<ContainerDescriptor> liveABs = liveContApi
					.all(ContainerQuery.ownerAndType(item.liveEntryUid(), IAddressBookUids.TYPE));

			IContainers backContApi = back.provider().instance(IContainers.class);
			List<ContainerDescriptor> backABs = backContApi
					.all(ContainerQuery.ownerAndType(item.entryUid, IAddressBookUids.TYPE));

			monitor.begin(backABs.size(), String.format(
					"Starting restore for uid %s : Backup contains %d addressbook(s)", item.entryUid, backABs.size()));

			logger.info("Backup contains {} addressbook(s)", backABs.size());
			for (ContainerDescriptor backAB : backABs) {
				restore(back, live, backAB, liveABs, monitor.subWork(1));
			}
		} catch (Exception e) {
			logger.error("Error while restoring addressbooks", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		monitor.end(true, "finished.", "[]");

	}

	private void restore(BmContext back, BmContext live, ContainerDescriptor backAB, List<ContainerDescriptor> liveABs,
			IServerTaskMonitor monitor) {
		IContainers liveContApi = live.provider().instance(IContainers.class);
		IAddressBook backupABApi = back.provider().instance(IAddressBook.class, backAB.uid);

		List<String> allUids = backupABApi.allUids();
		monitor.begin(allUids.size() + 1d, "Restoring " + backAB.name + " [uid=" + backAB.uid + "]");

		String bookUid = mapBookUid(backAB.uid);

		if (liveABs.stream().anyMatch(c -> c.uid.equals(bookUid))) {
			IAddressBook liveABApi = live.provider().instance(IAddressBook.class, bookUid);
			liveABApi.reset();
			monitor.progress(1, "reset done");
		} else {
			backAB.owner = item.liveEntryUid();
			liveContApi.create(bookUid, backAB);
			monitor.progress(1, "addressbook recreated");
		}

		IAddressBook liveABApi = live.provider().instance(IAddressBook.class, bookUid);

		for (List<String> batch : Lists.partition(backupABApi.allUids(), 1000)) {
			List<ItemValue<VCard>> cards = backupABApi.multipleGet(batch);
			VCardChanges changes = VCardChanges.create(
					cards.stream().map(e -> VCardChanges.ItemAdd.create(e.uid, e.value)).collect(Collectors.toList()),
					Collections.emptyList(), Collections.emptyList());
			liveABApi.updates(changes);
			monitor.progress(batch.size(), null);
		}
	}

	private String mapBookUid(String uid) {
		if (!item.entryUid.equals(item.liveEntryUid()) && uid.endsWith(String.format("_%s", item.entryUid))) {
			return String.format("%s%s", uid.substring(0, uid.length() - item.entryUid.length()), item.liveEntryUid());
		}

		return uid;
	}
}
