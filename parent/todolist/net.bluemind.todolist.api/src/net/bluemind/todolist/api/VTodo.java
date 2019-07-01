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
package net.bluemind.todolist.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.icalendar.api.ICalendarElement;

@BMApi(version = "3")
public class VTodo extends ICalendarElement {

	public BmDateTime due;
	public Integer percent;
	public BmDateTime completed;
	public String uid;

	public VTodo copy() {
		VTodo copy = new VTodo();
		copy.dtstart = dtstart;
		copy.summary = summary;
		copy.classification = classification;
		copy.description = description;
		copy.location = location;
		copy.priority = priority;
		copy.alarm = alarm;
		copy.status = status;
		copy.attendees = attendees;
		copy.organizer = organizer;
		copy.exdate = exdate;
		copy.categories = categories;
		copy.rrule = rrule;

		copy.due = due;
		copy.percent = percent;
		copy.completed = completed;
		copy.uid = uid;
		return copy;
	}

}
