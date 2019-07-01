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
package net.bluemind.imip.parser.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VToDo;

public class IMIPParserHelper {

	public static List<CalendarComponent> fromICS(Reader reader) throws IOException, ParserException {
		CalendarBuilder builder = new CalendarBuilder();
		UnfoldingReader ur = new UnfoldingReader(reader, true);
		Calendar calendar = builder.build(ur);
		ComponentList clist = calendar.getComponents();

		@SuppressWarnings("unchecked")
		Iterator<CalendarComponent> it = clist.iterator();
		List<CalendarComponent> calendarComponents = new LinkedList<CalendarComponent>();
		while (it.hasNext()) {
			CalendarComponent component = it.next();
			if (component instanceof VEvent) {
				calendarComponents.add((VEvent) component);
			} else if (component instanceof VToDo) {
				calendarComponents.add((VToDo) component);
			}
		}

		return calendarComponents;
	}

}
