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
package net.bluemind.calendar.service.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.EventChangesMerge;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.core.api.ImportStats;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.icalendar.parser.CalendarOwner;

public class ICSImportTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(ICSImportTask.class);

	private final String ics;
	private final ICalendar calendarService;
	private final Optional<CalendarOwner> owner;

	public ICSImportTask(ICalendar calendar, String ics, Optional<CalendarOwner> owner) {
		this.calendarService = calendar;
		this.ics = ics;
		this.owner = owner;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(3, "Begin import");

		List<ItemValue<VEventSeries>> events = VEventServiceHelper.convertToVEventList(ics, owner);
		monitor.progress(1, "ICS parsed ( " + events.size() + " events )");
		ImportStats ret = importEvents(events, monitor.subWork("", 2));
		// FIXME ret should be returned as ImportStats
		monitor.end(true, ret.total + " events imported", JsonUtils.asString(ret));

	}

	private ImportStats importEvents(List<ItemValue<VEventSeries>> events, IServerTaskMonitor monitor)
			throws ServerFault {
		monitor.begin(events.size(), "Import " + events.size() + " events");

		VEventChanges changes = new VEventChanges();
		changes.add = new ArrayList<>();
		changes.modify = new ArrayList<>();
		changes.delete = new ArrayList<>();

		for (ItemValue<VEventSeries> itemValue : events) {
			VEventSeries event = itemValue.value;
			List<ItemValue<VEventSeries>> byIcsUid = calendarService.getByIcsUid(itemValue.uid);
			if (itemValue.updated == null || //
					byIcsUid.isEmpty() || //
					itemValue.updated.after(byIcsUid.get(0).updated)) {
				VEventChanges eventChanges = EventChangesMerge.getStrategy(byIcsUid, event).merge(byIcsUid, event);
				changes.addAll(eventChanges);
			}
			monitor.progress(1, "in progress");
		}
		ContainerUpdatesResult result = calendarService.updates(changes);
		ImportStats ret = new ImportStats();
		ret.uids = new ArrayList<String>();
		ret.uids.addAll(result.added);
		ret.uids.addAll(result.updated);
		ret.total = ret.uids.size();
		return ret;
	}

}
