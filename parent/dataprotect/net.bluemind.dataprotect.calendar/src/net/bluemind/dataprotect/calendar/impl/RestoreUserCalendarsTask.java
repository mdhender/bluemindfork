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
package net.bluemind.dataprotect.calendar.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.common.restore.RestoreRestorableItem;
import net.bluemind.dataprotect.service.BackupDataProvider;

public class RestoreUserCalendarsTask extends BlockingServerTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(RestoreUserCalendarsTask.class);

	private final DataProtectGeneration backup;
	private RestoreRestorableItem restorableItem;

	public RestoreUserCalendarsTask(DataProtectGeneration backup, Restorable item) {
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

			List<ContainerDescriptor> backCalendars = back.provider().instance(IContainers.class)
					.all(ContainerQuery.ownerAndType(restorableItem.entryUid(), ICalendarUids.TYPE));

			monitor.begin(backCalendars.size(), String.format("Starting restore for uid %s: Backup contains %s",
					restorableItem.entryUid(), backCalendars.size()));
			logger.info("Backup contains {} calendar(s)", backCalendars.size());

			CommonRestoreCalendar restoreCal = new CommonRestoreCalendar(restorableItem, back, live);
			for (ContainerDescriptor backCalendar : backCalendars) {
				monitor.subWork(1);
				restore(restoreCal, backCalendar);
			}
		} catch (Exception e) {
			logger.error("Error while restoring calendars", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		restorableItem.endTask();
	}

	private void restore(CommonRestoreCalendar restoreCal, ContainerDescriptor backCalendar) {
		ICalendar backCalApi = restoreCal.back.provider().instance(ICalendar.class, backCalendar.uid);

		List<String> allUids = backCalApi.all();
		restorableItem.monitor.begin(allUids.size() + 1d,
				"Restoring " + backCalendar.name + " [uid=" + backCalendar.uid + "]");

		String calendarUid = mapCalendarUid(backCalendar.uid);
		reset(restoreCal, backCalendar, calendarUid);
		restoreCal.restoreEntities(allUids, backCalendar.uid, calendarUid);
	}

	private void reset(CommonRestoreCalendar restoreCal, ContainerDescriptor backCalendar, String calendarUid) {
		List<ContainerDescriptor> liveCalendars = restoreCal.live.provider().instance(IContainers.class)
				.all(ContainerQuery.ownerAndType(restorableItem.liveEntryUid(), ICalendarUids.TYPE));

		if (liveCalendars.stream().anyMatch(c -> c.uid.equals(calendarUid))) {
			ICalendar lCalApi = restoreCal.live.provider().instance(ICalendar.class, calendarUid);
			TaskRef tr = lCalApi.reset();
			TaskUtils.wait(restoreCal.live.provider(), tr);
			restorableItem.monitor.progress(1, "reset done");
		} else {
			backCalendar.owner = restorableItem.liveEntryUid();
			restoreCal.live.provider().instance(IContainers.class).create(calendarUid, backCalendar);
			restorableItem.monitor.progress(1, "calendar recreated");
		}
	}

	private String mapCalendarUid(String uid) {
		if (!restorableItem.entryUid().equals(restorableItem.liveEntryUid())
				&& uid.endsWith(String.format(":%s", restorableItem.entryUid()))) {
			return String.format("%s%s", uid.substring(0, uid.length() - restorableItem.entryUid().length()),
					restorableItem.liveEntryUid());
		}

		return uid;
	}

}
