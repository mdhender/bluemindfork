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

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
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

public class RestoreDomainBooksTask extends BlockingServerTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(RestoreDomainBooksTask.class);

	private final DataProtectGeneration backup;
	private RestoreRestorableItem restorableItem;

	public RestoreDomainBooksTask(DataProtectGeneration backup, Restorable item) {
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

			AddressBookDescriptor backDomainAddressBook = back.provider().instance(IAddressBooksMgmt.class)
					.get(restorableItem.entryUid());

			monitor.begin(1, String.format("Starting restore domain addressbook uid %s", restorableItem.entryUid()));

			List<String> allUids = back.provider().instance(IAddressBook.class, restorableItem.entryUid()).allUids();
			monitor.begin(allUids.size() + 1d,
					"Restoring " + backDomainAddressBook.name + " [uid=" + restorableItem.entryUid() + "]");

			reset(live, backDomainAddressBook);

			new CommonRestoreBooks(restorableItem, back, live).restoreEntities(allUids);

		} catch (Exception e) {
			logger.error("Error while restoring domain addressbook", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		restorableItem.endTask();
	}

	private void reset(BmContext live, AddressBookDescriptor backDomainAddressBook) {
		AddressBookDescriptor liveDomainAddressBook = live.provider().instance(IAddressBooksMgmt.class)
				.get(restorableItem.liveEntryUid());

		if (liveDomainAddressBook == null) {
			backDomainAddressBook.owner = restorableItem.domain();
			IAddressBooksMgmt liveAddressBooksMgmtApi = live.provider().instance(IAddressBooksMgmt.class);
			liveAddressBooksMgmtApi.create(restorableItem.entryUid(), backDomainAddressBook, false);
			restorableItem.monitor.progress(1, "domain addressbook recreated");
		} else {
			IAddressBook liveAddressBookApi = live.provider().instance(IAddressBook.class, restorableItem.entryUid());
			liveAddressBookApi.reset();
			restorableItem.monitor.progress(1, "reset done");
		}
	}

}
