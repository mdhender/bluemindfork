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
package net.bluemind.calendar.pdf.internal;

import java.util.Calendar;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;

public class PrintedEvent {

	public String id;
	public VEvent event;
	public int position;
	public int unit;
	public int end;
	public int size;
	public Calendar dtstart;
	public Calendar dtend;
	public String calendarId;
	public ParticipationStatus part;

	public PrintedEvent() {
		position = 0;
		unit = 1;
	}

}
