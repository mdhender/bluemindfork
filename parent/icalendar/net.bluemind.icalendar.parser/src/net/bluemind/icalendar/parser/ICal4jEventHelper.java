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
package net.bluemind.icalendar.parser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.tag.api.TagRef;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.property.DateProperty;

public class ICal4jEventHelper<T extends VEvent> extends ICal4jHelper<T> {

	@Override
	@SuppressWarnings("unchecked")
	public ItemValue<T> parseIcs(T iCalendarElement, CalendarComponent cc, Optional<String> globalTZ,
			Map<String, String> tzMapping, Optional<CalendarOwner> owner, List<TagRef> allTags) {

		ItemValue<T> parseIcs = super.parseIcs(iCalendarElement, cc, globalTZ, tzMapping, owner, allTags);

		// RECCURID
		if (cc.getProperty(Property.RECURRENCE_ID) != null) {
			BmDateTime recurid = parseIcsDate((DateProperty) cc.getProperty(Property.RECURRENCE_ID), globalTZ,
					tzMapping);
			VEventOccurrence evt = VEventOccurrence.fromEvent(iCalendarElement, recurid);

			// FIXME should instantiate iCalendarElement in this class
			parseIcs.value = (T) evt;
		}

		return parseIcs;
	}

}
