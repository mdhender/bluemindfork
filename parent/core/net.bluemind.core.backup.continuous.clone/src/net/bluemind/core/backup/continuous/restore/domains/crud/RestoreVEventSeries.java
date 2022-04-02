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
package net.bluemind.core.backup.continuous.restore.domains.crud;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;

public class RestoreVEventSeries extends CrudRestore<VEventSeries> {
	private static final ValueReader<ItemValue<VEventSeries>> reader = JsonUtils
			.reader(new TypeReference<ItemValue<VEventSeries>>() {
			});
	private final IServiceProvider target;

	Set<String> validatedCalendars = ConcurrentHashMap.newKeySet();

	public RestoreVEventSeries(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		super(log, domain);
		this.target = target;
	}

	@Override
	public String type() {
		return ICalendarUids.TYPE;
	}

	@Override
	protected ValueReader<ItemValue<VEventSeries>> reader() {
		return reader;
	}

	@Override
	protected ICalendar api(ItemValue<Domain> domain, RecordKey key) {
		if (!validatedCalendars.contains(key.uid)) {
			IContainers contApi = target.instance(IContainers.class);
			if (contApi.getIfPresent(key.uid) == null) {
				ICalendarsMgmt mgmtApi = target.instance(ICalendarsMgmt.class);
				CalendarDescriptor cd = CalendarDescriptor.create("usercal-" + key.uid, key.owner, domain.uid);
				mgmtApi.create(key.uid, cd);
				validatedCalendars.add(key.uid);
			}
		}

		return target.instance(ICalendar.class, key.uid);
	}

}
