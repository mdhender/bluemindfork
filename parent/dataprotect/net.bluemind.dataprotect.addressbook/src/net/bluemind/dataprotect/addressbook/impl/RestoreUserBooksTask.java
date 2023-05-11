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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.common.restore.RestoreRestorableItem;
import net.bluemind.dataprotect.service.BackupDataProvider;

public class RestoreUserBooksTask extends BlockingServerTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(RestoreUserBooksTask.class);

	private final DataProtectGeneration backup;
	private RestoreRestorableItem restorableItem;

	public RestoreUserBooksTask(DataProtectGeneration backup, Restorable item) {
		this.backup = backup;
		this.restorableItem = new RestoreRestorableItem(item);
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		restorableItem.setMonitor(monitor);
		monitor.begin(10, String.format("Starting restore for UID: %s from old UID: %s ", restorableItem.liveEntryUid(),
				restorableItem.entryUid()));
		logger.info("Starting restore for uid {}", restorableItem.entryUid());

		try (BackupDataProvider bdp = new BackupDataProvider(null, SecurityContext.SYSTEM, monitor)) {
			BmContext back = bdp.createContextWithData(backup, restorableItem.item);
			BmContext live = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

			List<ContainerDescriptor> backABs = back.provider().instance(IContainers.class)
					.all(ContainerQuery.ownerAndType(restorableItem.entryUid(), IAddressBookUids.TYPE));

			monitor.begin(backABs.size(),
					String.format("Starting restore for uid %s : Backup contains %d addressbook(s)",
							restorableItem.entryUid(), backABs.size()));
			logger.info("Backup contains {} addressbook(s)", backABs.size());

			CommonRestoreBooks restoreBooks = new CommonRestoreBooks(restorableItem, back, live);
			for (ContainerDescriptor backAB : backABs) {
				monitor.subWork(1);
				restore(restoreBooks, backAB);
			}
		} catch (Exception e) {
			logger.error("Error while restoring addressbooks", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		restorableItem.endTask();
	}

	private void restore(CommonRestoreBooks restoreBooks, ContainerDescriptor backAB) {
		IAddressBook backupABApi = restoreBooks.back.provider().instance(IAddressBook.class, backAB.uid);

		List<String> allUids = backupABApi.allUids();
		restorableItem.monitor.begin(allUids.size() + 1d, "Restoring " + backAB.name + " [uid=" + backAB.uid + "]");

		String bookUid = mapBookUid(backAB.uid);
		reset(restoreBooks, backAB, bookUid);
		restoreBooks.restoreEntities(allUids, backAB.uid, bookUid);
	}

	private String mapBookUid(String uid) {
		if (!restorableItem.entryUid().equals(restorableItem.liveEntryUid())
				&& uid.endsWith(String.format("_%s", restorableItem.entryUid()))) {
			return String.format("%s%s", uid.substring(0, uid.length() - restorableItem.entryUid().length()),
					restorableItem.liveEntryUid());
		}

		return uid;
	}

	private void reset(CommonRestoreBooks restoreBooks, ContainerDescriptor backAB, String bookUid) {
		List<ContainerDescriptor> liveABs = restoreBooks.live.provider().instance(IContainers.class)
				.all(ContainerQuery.ownerAndType(restorableItem.liveEntryUid(), IAddressBookUids.TYPE));

		if (liveABs.stream().anyMatch(c -> c.uid.equals(bookUid))) {
			IAddressBook liveABApi = restoreBooks.live.provider().instance(IAddressBook.class, bookUid);
			liveABApi.reset();
			restorableItem.monitor.progress(1, "reset done");
		} else {
			backAB.owner = restorableItem.liveEntryUid();
			restoreBooks.live.provider().instance(IContainers.class).create(bookUid, backAB);
			restorableItem.monitor.progress(1, "addressbook recreated");
		}

	}
}
