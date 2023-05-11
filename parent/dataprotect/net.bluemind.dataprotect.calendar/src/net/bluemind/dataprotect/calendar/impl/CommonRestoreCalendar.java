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

import com.google.common.collect.Lists;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventChanges.ItemAdd;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.dataprotect.common.restore.CommonRestoreEntities;
import net.bluemind.dataprotect.common.restore.RestoreRestorableItem;

public class CommonRestoreCalendar extends CommonRestoreEntities {

	protected CommonRestoreCalendar(RestoreRestorableItem item, BmContext back, BmContext live) {
		super(item, back, live);
	}

	@Override
	public void restoreEntities(List<String> allUids) {
		restoreEntities(allUids, item.entryUid(), item.entryUid());
	}

	@Override
	public void restoreEntities(List<String> allUids, String backUid, String liveUid) {
		ICalendar backCalApi = back.provider().instance(ICalendar.class, backUid);
		ICalendar liveCalApi = live.provider().instance(ICalendar.class, liveUid);

		for (List<String> batch : Lists.partition(backCalApi.all(), 1000)) {
			List<ItemValue<VEventSeries>> events = backCalApi.multipleGet(batch);
			VEventChanges changes = VEventChanges.create(
					events.stream().map(e -> ItemAdd.create(e.uid, e.value, false)).collect(Collectors.toList()),
					Collections.emptyList(), Collections.emptyList());
			ContainerUpdatesResult updatesResult = liveCalApi.updates(changes);
			if (updatesResult.errors != null && !updatesResult.errors.isEmpty()) {
				item.errors.addAll(updatesResult.errors);
			}
			item.monitor.progress(batch.size(), null);
		}
	}
}
