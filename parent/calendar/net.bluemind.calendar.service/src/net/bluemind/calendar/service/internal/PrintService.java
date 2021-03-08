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
import java.util.stream.Collectors;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.IPrint;
import net.bluemind.calendar.api.PrintData;
import net.bluemind.calendar.api.PrintOptions;
import net.bluemind.calendar.api.PrintOptions.CalendarMetadata;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.occurrence.OccurrenceHelper;
import net.bluemind.calendar.pdf.PrintCalendarHelper;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;

public class PrintService implements IPrint {

	private BmContext context;

	public PrintService(BmContext context) {
		this.context = context;
	}

	@Override
	public PrintData print(PrintOptions options) throws ServerFault {

		VEventQuery eventQuery = VEventQuery.create(options.dateBegin, options.dateEnd);

		List<ItemContainerValue<VEvent>> ret = new ArrayList<>();

		List<CalendarMetadata> containers = options.calendars;

		for (CalendarMetadata cal : containers) {

			ICalendar calendar = context.provider().instance(ICalendar.class, cal.uid);
			ListResult<ItemValue<VEventSeries>> search = null;
			try {
				search = calendar.search(eventQuery);
			} catch (ServerFault e) {
				if (e.getCode() == ErrorCode.PERMISSION_DENIED) {
					continue;
				}
			}
			ret.addAll(search.values.stream().flatMap(series -> {
				return OccurrenceHelper.list(series, options.dateBegin, options.dateEnd).stream().map(evt -> {
					return ItemContainerValue.create(cal.uid, series, evt);
				});
			}).collect(Collectors.toList()));

		}

		if (options.tagsFilter != null && !options.tagsFilter.isEmpty()) {
			ret = ret.stream().filter(
					ev -> ev.value.categories.stream().anyMatch(tag -> options.tagsFilter.contains(tag.itemUid)))
					.collect(Collectors.toList());
		}
		return PrintCalendarHelper.printCalendar(context, options, ret);
	}
}
