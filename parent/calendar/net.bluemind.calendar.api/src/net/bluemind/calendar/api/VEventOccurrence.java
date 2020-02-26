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
package net.bluemind.calendar.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.date.BmDateTime;

@BMApi(version = "3")
public class VEventOccurrence extends VEvent {

	public BmDateTime recurid;

	public static <T extends VEvent> VEventOccurrence fromEvent(T event, BmDateTime recurid) {
		VEvent evt = event.copy();
		VEventOccurrence occurrence = new VEventOccurrence();
		occurrence.dtstart = evt.dtstart;
		occurrence.summary = evt.summary;
		occurrence.classification = evt.classification;
		occurrence.description = evt.description;
		occurrence.location = evt.location;
		occurrence.priority = evt.priority;
		occurrence.alarm = evt.alarm;
		occurrence.status = evt.status;
		occurrence.attendees = evt.attendees;
		occurrence.organizer = evt.organizer;
		occurrence.exdate = evt.exdate;
		occurrence.rdate = evt.rdate;
		occurrence.categories = evt.categories;
		occurrence.dtend = evt.dtend;
		occurrence.transparency = evt.transparency;
		occurrence.recurid = recurid;
		occurrence.rrule = evt.rrule;
		occurrence.attachments = evt.attachments;
		occurrence.sequence = evt.sequence;
		occurrence.draft = evt.draft;
		return occurrence;
	}

	@Override
	public VEventOccurrence copy() {
		VEvent evt = super.copy();
		return fromEvent(evt, this.recurid);
	}

	@Override
	public VEventOccurrence filtered() {
		VEvent f = super.filtered();
		return fromEvent(f, this.recurid);
	}

	@Override
	public boolean exception() {
		return null != recurid;
	}

}
