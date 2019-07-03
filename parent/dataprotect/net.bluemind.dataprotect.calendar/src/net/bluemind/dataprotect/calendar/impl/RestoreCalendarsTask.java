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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventChanges.ItemAdd;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.service.BackupDataProvider;

public class RestoreCalendarsTask implements IServerTask {

	private DataProtectGeneration backup;
	private Restorable item;
	private static final Logger logger = LoggerFactory.getLogger(RestoreCalendarsTask.class);

	public RestoreCalendarsTask(DataProtectGeneration backup, Restorable item) {
		this.backup = backup;
		this.item = item;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.log("starting restore for uid " + item.entryUid);

		try (BackupDataProvider bdp = new BackupDataProvider(null, SecurityContext.SYSTEM, monitor)) {
			BmContext back = bdp.createContextWithData(backup, item);
			BmContext live = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

			ContainerQuery cq = ContainerQuery.ownerAndType(item.entryUid, ICalendarUids.TYPE);

			IContainers lContApi = live.provider().instance(IContainers.class);
			List<ContainerDescriptor> liveCalendars = lContApi.all(cq);

			IContainers bContApi = back.provider().instance(IContainers.class);
			List<ContainerDescriptor> dataProtectedCalendars = bContApi.all(cq);

			monitor.begin(dataProtectedCalendars.size(), "starting restore for uid " + item.entryUid
					+ " : Backup contains " + dataProtectedCalendars.size() + " calendar(s)");

			logger.info("Backup contains " + dataProtectedCalendars.size() + " calendar(s)");
			for (ContainerDescriptor cd : dataProtectedCalendars) {

				restore(back, live, cd, liveCalendars, monitor.subWork(1));

			}
		} catch (Exception e) {
			logger.warn("Error while restoring calendars", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		monitor.end(true, "finished.", "[]");

	}

	private void restore(BmContext back, BmContext live, ContainerDescriptor cd,
			List<ContainerDescriptor> liveCalendars, IServerTaskMonitor monitor) {
		IContainers lContApi = live.provider().instance(IContainers.class);
		ICalendar bCalApi = back.provider().instance(ICalendar.class, cd.uid);

		List<String> allUids = bCalApi.all();
		monitor.begin(allUids.size() + 1d, "Restoring " + cd.name + " [uid=" + cd.uid + "]");

		if (liveCalendars.stream().filter(c -> c.uid.equals(cd.uid)).findFirst().isPresent()) {
			ICalendar lCalApi = live.provider().instance(ICalendar.class, cd.uid);
			TaskRef tr = lCalApi.reset();
			TaskUtils.wait(live.provider(), tr);

			monitor.progress(1, "reset done");
		} else {
			lContApi.create(cd.uid, cd);
			monitor.progress(1, "calendar recreated");
		}

		ICalendar lCalApi = live.provider().instance(ICalendar.class, cd.uid);

		for (List<String> batch : Lists.partition(bCalApi.all(), 1000)) {
			List<ItemValue<VEventSeries>> events = bCalApi.multipleGet(batch);
			VEventChanges changes = VEventChanges.create(
					events.stream().map(e -> ItemAdd.create(e.uid, e.value, false)).collect(Collectors.toList()),
					Collections.emptyList(), Collections.emptyList());
			lCalApi.updates(changes);
			monitor.progress(batch.size(), null);
		}
	}

}
