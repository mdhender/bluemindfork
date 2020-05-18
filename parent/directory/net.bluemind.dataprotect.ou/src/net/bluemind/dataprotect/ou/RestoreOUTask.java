/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.dataprotect.ou;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.service.BackupDataProvider;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;

public class RestoreOUTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(RestoreOUTask.class);

	private final DataProtectGeneration backup;
	private final Restorable item;

	public RestoreOUTask(DataProtectGeneration backup, Restorable item) {
		this.backup = backup;
		this.item = item;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(1, String.format("Starting restore for uid %s", item.liveEntryUid()));
		logger.info("Starting restore for uid {}", item.liveEntryUid());
		try (BackupDataProvider bdp = new BackupDataProvider(null, SecurityContext.SYSTEM, monitor)) {
			BmContext back = bdp.createContextWithData(backup, item);
			BmContext live = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

			IOrgUnits ouBackup = back.provider().instance(IOrgUnits.class, item.domainUid);
			IOrgUnits ouLive = live.provider().instance(IOrgUnits.class, item.domainUid);

			ItemValue<OrgUnit> currentOu = ouLive.getComplete(item.liveEntryUid());
			ItemValue<OrgUnit> backupOu = ouBackup.getComplete(item.entryUid);
			if (currentOu != null) {
				currentOu.value.name = backupOu.value.name;
				ouLive.update(item.liveEntryUid(), currentOu.value);
			} else {
				createHierarchy(ouBackup, ouLive, backupOu);
			}
			monitor.progress(1, "restored...");
		} catch (Exception e) {
			logger.warn("Error while restoring ou", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}
		monitor.end(true, "finished.", "[]");

	}

	private void createHierarchy(IOrgUnits ouBackup, IOrgUnits ouLive, ItemValue<OrgUnit> backupOu) {
		logger.info("restore of uid {}", backupOu.uid);
		if (backupOu.value.parentUid != null && ouLive.getComplete(backupOu.value.parentUid) == null) {
			ItemValue<OrgUnit> parent = ouBackup.getComplete(backupOu.value.parentUid);
			createHierarchy(ouBackup, ouLive, parent);
		}
		ouLive.create(backupOu.uid, backupOu.value);
	}

}
