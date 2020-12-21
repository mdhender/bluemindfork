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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.bluemind.calendar.EventChangesMerge;
import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventChanges.ItemDelete;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.api.internal.IInternalCalendar;
import net.bluemind.core.api.ImportStats;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.icalendar.parser.CalendarOwner;

public abstract class ICSImportTask implements IServerTask {

	protected final IInternalCalendar service;
	protected final Optional<CalendarOwner> owner;
	protected final int STEP = 50;
	protected final Mode mode;

	public ICSImportTask(IInternalCalendar calendar, Optional<CalendarOwner> owner, Mode mode) {
		this.service = calendar;
		this.owner = owner;
		this.mode = mode;
	}

	protected abstract void convertToVEventList(Consumer<ItemValue<VEventSeries>> consumer);

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(3, "Begin import");
		ContainerUpdatesResult ret = new ContainerUpdatesResult();
		try {
			Consumer<ItemValue<VEventSeries>> consumer = (series -> {
				ContainerUpdatesResult importEventResult = importEvent(series);
				ret.added.addAll(importEventResult.added);
				ret.updated.addAll(importEventResult.updated);
				ret.unhandled.addAll(importEventResult.unhandled);
			});
			convertToVEventList(consumer);
			monitor.progress(1, "ICS parsed ( " + ret.total() + " events )");

			if (mode == Mode.IMPORT) {
				ImportStats asStats = new ImportStats();
				asStats.uids = new ArrayList<String>();
				asStats.uids.addAll(ret.added);
				asStats.uids.addAll(ret.updated);
				asStats.total = ret.total();
				monitor.end(true, ret.total() + " events synchronized", JsonUtils.asString(asStats));
			} else if (mode == Mode.SYNC) {
				List<String> uids = service.all();
				uids.removeAll(ret.added);
				uids.removeAll(ret.updated);
				uids.removeAll(ret.unhandled);
				VEventChanges changes = new VEventChanges();
				changes.add = new ArrayList<>();
				changes.modify = new ArrayList<>();
				changes.delete = new ArrayList<>();
				for (String uid : uids) {
					changes.delete.add(ItemDelete.create(uid, false));
					ret.removed.add(uid);
				}
				service.updates(changes, false);
			}
			monitor.end(true, ret.total() + " events synchronized", JsonUtils.asString(ret));

		} finally {
			if (ret.synced() > 0) {
				service.emitNotification();
			}
		}
	}

	private ContainerUpdatesResult importEvent(ItemValue<VEventSeries> itemValue) {
		ContainerUpdatesResult ret = new ContainerUpdatesResult();

		VEventChanges changes = new VEventChanges();
		changes.add = new ArrayList<>();
		changes.modify = new ArrayList<>();
		changes.delete = new ArrayList<>();

		VEventSeries event = itemValue.value;
		List<ItemValue<VEventSeries>> byIcsUid = service.getByIcsUid(itemValue.uid);
		if (itemValue.updated == null || //
				byIcsUid.isEmpty() || //
				eventHasAddedExDates(event, byIcsUid.get(0).value) || // BM-16128 Google does not update last-modified
																		// if only the exdates have been modified
				itemValue.updated.after(byIcsUid.get(0).updated)) {
			VEventChanges eventChanges = EventChangesMerge.getStrategy(byIcsUid, event).merge(byIcsUid, event);
			changes.addAll(eventChanges);
		} else {
			ret.unhandled.add(itemValue.uid);
		}
		ContainerUpdatesResult result = service.updates(changes, false);
		ret.added.addAll(result.added);
		ret.updated.addAll(result.updated);
		ret.unhandled.addAll(result.errors.stream().map(e -> e.uid).collect(Collectors.toList()));

		return ret;
	}

	private boolean eventHasAddedExDates(VEventSeries event, VEventSeries localEvent) {
		return exdateCount(event) != exdateCount(localEvent);
	}

	private int exdateCount(VEventSeries event) {
		if (event.main != null && event.main.exdate != null) {
			return event.main.exdate.size();
		}
		return 0;
	}

	public static enum Mode {
		SYNC, IMPORT
	}

}
