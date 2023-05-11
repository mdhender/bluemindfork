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
package net.bluemind.dataprotect.resource.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.CalendarSettingsData;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarSettings;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.container.model.ItemValue;
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
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;

public class RestoreResourceTask extends BlockingServerTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(RestoreResourceTask.class);

	private final DataProtectGeneration backup;
	private RestoreRestorableItem restorableItem;

	public RestoreResourceTask(DataProtectGeneration backup, Restorable item) {
		this.backup = backup;
		this.restorableItem = new RestoreRestorableItem(item);
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		restorableItem.setMonitor(monitor);
		monitor.begin(1, String.format("Starting restore for uid %s", restorableItem.entryUid()));
		logger.info("Starting restore for uid {}", restorableItem.entryUid());

		try (BackupDataProvider bdp = new BackupDataProvider(null, SecurityContext.SYSTEM, monitor)) {
			BmContext back = bdp.createContextWithData(backup, restorableItem.item);
			BmContext live = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

			ItemValue<ResourceDescriptor> backupResource = getResource(back, restorableItem.entryUid());
			ItemValue<ResourceDescriptor> currentResource = getResource(live, restorableItem.liveEntryUid());
			monitor.begin(1, String.format("Starting restore resource uid %s", restorableItem.entryUid()));

			live.provider().instance(IResources.class, restorableItem.domain()).restore(backupResource,
					currentResource == null);

			String resourceCal = ICalendarUids.resourceCalendar(backupResource.uid);

			ICalendarSettings backCalendarSettings = back.provider().instance(ICalendarSettings.class, resourceCal);
			CalendarSettingsData oldCalendarSettings = backCalendarSettings.get();

			if (oldCalendarSettings.present()) {
				ICalendarSettings liveCalendarSettings = live.provider().instance(ICalendarSettings.class, resourceCal);
				liveCalendarSettings.set(oldCalendarSettings);
			}

			monitor.log(String.format("Restore resource calendar uid %s", resourceCal));
			restorableItem.item.entryUid = resourceCal;

			List<String> allUids = back.provider().instance(ICalendar.class, restorableItem.entryUid()).all();
			monitor.begin(allUids.size() + 1d,
					"Restoring resource" + backupResource.displayName + "calendar [uid=" + resourceCal + "]");

			reset(live);

			new CommonRestoreResource(restorableItem, back, live).restoreEntities(allUids);
			monitor.progress(1, "restored...");
		} catch (Exception e) {
			logger.error("Error while restoring resource", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		restorableItem.endTask();
	}

	private ItemValue<ResourceDescriptor> getResource(BmContext ctx, String uid) {
		return ctx.provider().instance(IResources.class, restorableItem.domain()).getComplete(uid);
	}

	private void reset(BmContext live) {
		ICalendar liveCalendarApi = live.provider().instance(ICalendar.class, restorableItem.liveEntryUid());
		liveCalendarApi.reset();
		restorableItem.monitor.progress(1, "reset done");
	}

}
