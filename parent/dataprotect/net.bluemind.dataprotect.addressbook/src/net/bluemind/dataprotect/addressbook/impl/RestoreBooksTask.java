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

	private DataProtectGeneration backup;
	private Restorable item;
	private static final Logger logger = LoggerFactory.getLogger(RestoreBooksTask.class);

	public RestoreBooksTask(DataProtectGeneration backup, Restorable item) {
		this.backup = backup;
		this.item = item;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(10, "starting restore for uid " + item.entryUid);
		try (BackupDataProvider bdp = new BackupDataProvider(null, SecurityContext.SYSTEM, monitor)) {
			BmContext back = bdp.createContextWithData(backup, item);
			BmContext live = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

			ContainerQuery cq = ContainerQuery.ownerAndType(item.entryUid, IAddressBookUids.TYPE);

			IContainers lContApi = live.provider().instance(IContainers.class);
			List<ContainerDescriptor> liveABs = lContApi.all(cq);

			IContainers bContApi = back.provider().instance(IContainers.class);
			List<ContainerDescriptor> dataProtectedBooks = bContApi.all(cq);

			monitor.begin(dataProtectedBooks.size(), "starting restore for uid " + item.entryUid + " : Backup contains "
					+ dataProtectedBooks.size() + " addressbook(s)");

			logger.info("Backup contains " + dataProtectedBooks.size() + " addressbook(s)");
			for (ContainerDescriptor cd : dataProtectedBooks) {

				restore(back, live, cd, liveABs, monitor.subWork(1));

			}
		} catch (Exception e) {
			logger.warn("Error while restoring addressbooks", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		monitor.end(true, "finished.", "[]");

	}

	private void restore(BmContext back, BmContext live, ContainerDescriptor cd, List<ContainerDescriptor> liveBooks,
			IServerTaskMonitor monitor) {
		IContainers lContApi = live.provider().instance(IContainers.class);
		IAddressBook backupABApi = back.provider().instance(IAddressBook.class, cd.uid);

		List<String> allUids = backupABApi.allUids();
		monitor.begin(allUids.size() + 1d, "Restoring " + cd.name + " [uid=" + cd.uid + "]");

		if (liveBooks.stream().filter(c -> c.uid.equals(cd.uid)).findFirst().isPresent()) {
			IAddressBook liveABApi = live.provider().instance(IAddressBook.class, cd.uid);
			liveABApi.reset();
			monitor.progress(1, "reset done");
		} else {
			lContApi.create(cd.uid, cd);
			monitor.progress(1, "addressbook recreated");
		}

		IAddressBook liveABApi = live.provider().instance(IAddressBook.class, cd.uid);

		for (List<String> batch : Lists.partition(backupABApi.allUids(), 1000)) {
			List<ItemValue<VCard>> cards = backupABApi.multipleGet(batch);
			VCardChanges changes = VCardChanges.create(
					cards.stream().map(e -> VCardChanges.ItemAdd.create(e.uid, e.value)).collect(Collectors.toList()),
					Collections.emptyList(), Collections.emptyList());
			liveABApi.updates(changes);
			monitor.progress(batch.size(), null);
		}
	}
}
