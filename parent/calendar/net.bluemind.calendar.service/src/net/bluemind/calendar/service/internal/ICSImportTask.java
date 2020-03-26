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
import net.bluemind.calendar.api.VEventChanges.ItemDelete;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.ImportStats;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.icalendar.parser.CalendarOwner;

public abstract class ICSImportTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(ICSImportTask.class);

	protected final ICalendar service;
	protected final Optional<CalendarOwner> owner;
	protected final int STEP = 50;
	protected final Mode mode;

	public ICSImportTask(ICalendar calendar, Optional<CalendarOwner> owner, Mode mode) {
		this.service = calendar;
		this.owner = owner;
		this.mode = mode;
	}

	protected abstract List<ItemValue<VEventSeries>> convertToVEventList();

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(3, "Begin import");

		List<ItemValue<VEventSeries>> events = convertToVEventList();
		monitor.progress(1, "ICS parsed ( " + events.size() + " events )");
		ContainerUpdatesResult ret = importEvents(events, monitor.subWork("", 2));

		if (mode == Mode.IMPORT) {
			ImportStats asStats = new ImportStats();
			asStats.uids = new ArrayList<String>();
			asStats.uids.addAll(ret.added);
			asStats.uids.addAll(ret.updated);
			asStats.total = events.size();
			monitor.end(true, ret.total() + " events synchronized", JsonUtils.asString(asStats));
		} else {
			monitor.end(true, ret.total() + " events synchronized", JsonUtils.asString(ret));
		}
	}

	private ContainerUpdatesResult importEvents(List<ItemValue<VEventSeries>> events, IServerTaskMonitor monitor)
			throws ServerFault {
		monitor.begin(events.size(), "Import " + events.size() + " events");
		ContainerUpdatesResult ret = new ContainerUpdatesResult();

		ArrayList<String> icsUids = new ArrayList<String>(events.size());
		VEventChanges changes = new VEventChanges();
		changes.add = new ArrayList<>();
		changes.modify = new ArrayList<>();
		changes.delete = new ArrayList<>();

		int index = 0;
		for (ItemValue<VEventSeries> itemValue : events) {
			VEventSeries event = itemValue.value;
			icsUids.add(itemValue.uid);
			List<ItemValue<VEventSeries>> byIcsUid = service.getByIcsUid(itemValue.uid);
			if (itemValue.updated == null || //
					byIcsUid.isEmpty() || //
					itemValue.updated.after(byIcsUid.get(0).updated)) {
				VEventChanges eventChanges = EventChangesMerge.getStrategy(byIcsUid, event).merge(byIcsUid, event);
				changes.addAll(eventChanges);
			}
			if (index++ % STEP == 0) {
				ContainerUpdatesResult result = service.updates(changes);
				monitor.progress(index, null);
				ret.added.addAll(result.added);
				ret.updated.addAll(result.updated);
				changes.add = new ArrayList<>();
				changes.modify = new ArrayList<>();
				changes.delete = new ArrayList<>();
			}
		}

		if (mode == Mode.SYNC) {
			List<String> uids = service.all();
			uids.removeAll(icsUids);
			for (String uid : uids) {
				changes.delete.add(ItemDelete.create(uid, false));
			}
		}

		ContainerUpdatesResult result = service.updates(changes);
		monitor.progress(index, null);

		ret.added.addAll(result.added);
		ret.updated.addAll(result.updated);
		ret.removed.addAll(result.removed);
		return ret;
	}

	public static enum Mode {
		SYNC, IMPORT
	}

}
