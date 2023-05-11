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
package net.bluemind.dataprotect.calendar.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarsMgmt;
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

public class RestoreDomainCalendarsTask extends BlockingServerTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(RestoreDomainCalendarsTask.class);

	private final DataProtectGeneration backup;
	private RestoreRestorableItem restorableItem;

	public RestoreDomainCalendarsTask(DataProtectGeneration backup, Restorable item) {
		this.backup = backup;
		this.restorableItem = new RestoreRestorableItem(item);
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		restorableItem.setMonitor(monitor);
		monitor.log(String.format("Starting restore for uid %s", restorableItem.entryUid()));
		logger.info("Starting restore for uid {}", restorableItem.entryUid());

		try (BackupDataProvider bdp = new BackupDataProvider(null, SecurityContext.SYSTEM, monitor)) {
			BmContext back = bdp.createContextWithData(backup, restorableItem.item);
			BmContext live = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

			CalendarDescriptor backCalendarDescriptor = back.provider().instance(ICalendarsMgmt.class)
					.get(restorableItem.entryUid());

			monitor.begin(1, String.format("Starting restore domain calendar uid %s", restorableItem.entryUid()));

			List<String> allUids = back.provider().instance(ICalendar.class, restorableItem.entryUid()).all();
			monitor.begin(allUids.size() + 1d,
					"Restoring " + backCalendarDescriptor.name + " [uid=" + restorableItem.entryUid() + "]");

			reset(live, backCalendarDescriptor);

			new CommonRestoreCalendar(restorableItem, back, live).restoreEntities(allUids);

		} catch (Exception e) {
			logger.error("Error while restoring domain calendars", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		restorableItem.endTask();
	}

	private void reset(BmContext live, CalendarDescriptor backDomainCalendar) {
		CalendarDescriptor liveCalendarDescriptor = live.provider().instance(ICalendarsMgmt.class)
				.get(restorableItem.liveEntryUid());

		if (liveCalendarDescriptor == null) {
			backDomainCalendar.owner = restorableItem.domain();
			live.provider().instance(ICalendarsMgmt.class).create(restorableItem.entryUid(), backDomainCalendar);
			restorableItem.monitor.progress(1, "domain calendar recreated");
		} else {
			ICalendar liveCalendarApi = live.provider().instance(ICalendar.class, restorableItem.entryUid());
			liveCalendarApi.reset();
			restorableItem.monitor.progress(1, "reset done");
		}

	}
}
