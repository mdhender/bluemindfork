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
package net.bluemind.dataprotect.webappdata.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.service.BackupDataProvider;
import net.bluemind.webappdata.api.IWebAppData;
import net.bluemind.webappdata.api.IWebAppDataUids;

public class RestoreWebAppDataTask extends BlockingServerTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(RestoreWebAppDataTask.class);

	private final DataProtectGeneration backup;
	private final Restorable item;

	public RestoreWebAppDataTask(DataProtectGeneration backup, Restorable item) {
		this.backup = backup;
		this.item = item;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(10, String.format("Starting webappdata restore for uid %s", item.entryUid));

		try (BackupDataProvider bdp = new BackupDataProvider(null, SecurityContext.SYSTEM, monitor)) {
			BmContext back = bdp.createContextWithData(backup, item);
			BmContext live = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

			IWebAppData backupApi = back.provider().instance(IWebAppData.class,
					IWebAppDataUids.containerUid(item.entryUid));
			IWebAppData liveApi = live.provider().instance(IWebAppData.class,
					IWebAppDataUids.containerUid(item.liveEntryUid()));

			liveApi.deleteAll();
			List<String> allUids = backupApi.allUids();
			logger.info("Backup contains " + allUids.size() + " webappdata");

			monitor.begin(allUids.size() + 1, "Restoring webappdata container for user " + item.displayName);

			backupApi.multipleGet(allUids).forEach(item -> {
				liveApi.create(item.uid, item.value);
				monitor.progress(1, "Restoring webappdata " + item.uid);
			});
		} catch (Exception e) {
			logger.error("Error while restoring webappdata", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}
		String successLog = "webappdata container and items recreated";
		monitor.end(true, successLog, successLog);
	}
}
