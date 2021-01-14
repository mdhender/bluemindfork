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

import java.util.Collections;
import java.util.stream.Collectors;

import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.icalendar.api.ICalendarElement;

@BMApi(version = "3")
public class VEvent extends ICalendarElement {

	public BmDateTime dtend;
	public Transparency transparency;

	@BMApi(version = "3")
	public enum Transparency {
		Opaque, //
		Transparent;
	}

	/**
	 * Check if {@link VEvent} is an all day event. An all day event is
	 * <p>
	 * <ul>
	 * <li>an event without dtend
	 * <li>an event with dtstart == dtend
	 * <li>...
	 * </ul>
	 * </p>
	 * 
	 * @return isAllday a boolean to specify if {@link VEvent} is en all day event
	 */
	public boolean allDay() {
		return dtstart != null && dtstart.precision.equals(BmDateTime.Precision.Date);
	}

	public VEvent copy() {
		VEvent copy = new VEvent();
		if (null != this.dtstart) {
			copy.dtstart = new BmDateTime(this.dtstart.iso8601, this.dtstart.timezone, this.dtstart.precision);
		}
		copy.summary = this.summary;
		copy.classification = this.classification;
		copy.description = this.description;
		copy.url = this.url;
		copy.location = this.location;
		copy.priority = this.priority;
		if (null != this.alarm) {
			copy.alarm = this.alarm.stream().map(al -> al.copy()).collect(Collectors.toList());
		}
		copy.status = this.status;
		if (null != this.attendees) {
			copy.attendees = this.attendees.stream().map(at -> at.copy()).collect(Collectors.toList());
		}
		if (null != this.organizer) {
			copy.organizer = this.organizer.copy();
		}
		if (null != this.exdate) {
			copy.exdate = this.exdate.stream().map(ex -> new BmDateTime(ex.iso8601, ex.timezone, ex.precision))
					.collect(Collectors.toSet());
		}
		if (null != this.rdate) {
			copy.rdate = this.rdate.stream().map(rd -> new BmDateTime(rd.iso8601, rd.timezone, rd.precision))
					.collect(Collectors.toSet());
		}
		if (null != this.categories) {
			copy.categories = categories.stream().map(cat -> cat.copy()).collect(Collectors.toList());
		}
		if (null != this.rrule) {
			copy.rrule = this.rrule.copy();
		}
		if (null != this.dtend) {
			copy.dtend = new BmDateTime(this.dtend.iso8601, this.dtend.timezone, this.dtend.precision);
		}
		copy.attachments = this.attachments.stream().map(att -> {
			AttachedFile file = new AttachedFile();
			file.name = att.name;
			file.expirationDate = att.expirationDate;
			file.publicUrl = att.publicUrl;
			return file;
		}).collect(Collectors.toList());

		copy.transparency = transparency;
		copy.sequence = sequence;
		copy.draft = draft;
		copy.conference = conference;
		return copy;
	}

	public VEvent filtered() {
		VEvent f = copy();
		f.summary = "Private";
		f.description = null;
		f.location = null;
		f.url = null;
		f.attendees = Collections.emptyList();
		f.attachments = Collections.emptyList();
		f.organizer = null;
		f.conference = null;
		return f;
	}

	public boolean exception() {
		return false;
	}

}
