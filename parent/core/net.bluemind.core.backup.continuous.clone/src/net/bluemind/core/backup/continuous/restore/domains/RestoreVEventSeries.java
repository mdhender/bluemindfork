/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.restore.domains;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;

public class RestoreVEventSeries implements RestoreDomainType {

	private static final ValueReader<ItemValue<VEventSeries>> mrReader = JsonUtils
			.reader(new TypeReference<ItemValue<VEventSeries>>() {
			});
	private final IServerTaskMonitor monitor;
	private IServiceProvider target;

	public RestoreVEventSeries(IServerTaskMonitor monitor, IServiceProvider target) {
		this.monitor = monitor;
		this.target = target;
	}

	@Override
	public String type() {
		return ICalendarUids.TYPE;
	}

	@Override
	public void restore(DataElement de) {
		ICalendar calApi = target.instance(ICalendar.class, de.key.uid);
		if (de.payload.length > 0) {
			createOrUpdate(new String(de.payload), calApi);
		} else {
			delete(de.key, calApi);
		}
	}

	private void createOrUpdate(String payload, ICalendar calApi) {
		ItemValue<VEventSeries> item = mrReader.read(payload);
		ItemValue<VEventSeries> existing = calApi.getCompleteById(item.internalId);
		if (existing != null) {
			calApi.updateWithItem(item);
			monitor.log("Update VEventSeries '" + item.displayName + "' " + item.value.icsUid);
		} else {
			calApi.createWithItem(item);
			monitor.log("Create VEventSeries '" + item.displayName + "' " + item.value.icsUid);
		}
	}

	private void delete(RecordKey key, ICalendar calApi) {
		calApi.deleteById(key.id);
	}

}
