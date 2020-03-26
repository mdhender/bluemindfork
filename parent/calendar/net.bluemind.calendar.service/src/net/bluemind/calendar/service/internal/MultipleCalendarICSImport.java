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

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.icalendar.parser.CalendarOwner;

public class MultipleCalendarICSImport extends ICSImportTask {

	private static final Logger logger = LoggerFactory.getLogger(MultipleCalendarICSImport.class);

	private final String ics;

	public MultipleCalendarICSImport(ICalendar calendar, String ics, Optional<CalendarOwner> owner, Mode mode) {
		super(calendar, owner, mode);
		this.ics = ics;
	}

	@Override
	protected List<ItemValue<VEventSeries>> convertToVEventList() {
		return VEventServiceHelper.convertToVEventList(ics, owner);
	}

}
