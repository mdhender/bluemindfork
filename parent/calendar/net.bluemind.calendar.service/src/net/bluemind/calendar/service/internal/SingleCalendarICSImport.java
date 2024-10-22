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

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.api.internal.IInternalCalendar;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.icalendar.parser.CalendarOwner;
import net.bluemind.tag.api.TagRef;

public class SingleCalendarICSImport extends ICSImportTask {

	private final InputStream ics;
	private final List<TagRef> allTags;

	public SingleCalendarICSImport(IInternalCalendar calendar, InputStream ics, Optional<CalendarOwner> owner,
			List<TagRef> allTags, Mode mode) {
		super(calendar, owner, mode);
		this.ics = ics;
		this.allTags = allTags;
	}

	@Override
	protected void convertToVEventList(Consumer<ItemValue<VEventSeries>> consumer) {
		VEventServiceHelper.parseCalendar(ics, owner, allTags, consumer);
	}

}
