/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.calendar.service.eventdeferredaction;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.service.internal.WaitForCalendarHook;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;

class EventCreator {
	VEventSeries event;
	private static int count = 0;

	public static EventCreator defaultVEvent(ZonedDateTime eventDate) {
		VEventSeries event = new VEventSeries();
		VEvent main = new VEvent();
		main.dtstart = BmDateTimeWrapper.create(eventDate, Precision.DateTime);
		main.dtend = BmDateTimeWrapper.create(eventDate.plusHours(2), Precision.DateTime);
		main.summary = "event " + System.currentTimeMillis();
		main.location = "Toulouse";
		main.description = "Lorem ipsum";

		event.main = main;
		EventCreator eventCreator = new EventCreator();
		eventCreator.event = event;
		return eventCreator;
	}

	public EventCreator withAlarm(int trigger) {
		VAlarm alarm = ICalendarElement.VAlarm.create(trigger);
		alarm.action = VAlarm.Action.Email;
		if (this.event.main.alarm == null) {
			this.event.main.alarm = new ArrayList<VAlarm>();
		}
		this.event.main.alarm.add(alarm);
		return this;
	}

	public EventCreator withRecurrence(Frequency frequency) {
		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = frequency;
		rrule.count = 2;
		rrule.interval = 1;
		this.event.main.rrule = rrule;
		return this;
	}

	public void saveOnCalendar(ICalendar calendar) throws InterruptedException, ExecutionException, TimeoutException {
		saveOnCalendar(calendar, nextUid());
	}

	public void saveOnCalendar(ICalendar calendar, String uid)
			throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Void> wait = WaitForCalendarHook.register(uid);
		calendar.create(uid, this.event, false);
		wait.get(5, TimeUnit.SECONDS);
	}

	private String nextUid() {
		return "uid" + count++;
	}

	public EventCreator withException(ZonedDateTime plannedDate, ZonedDateTime eventDate) {
		VEvent exception = new VEvent();
		exception.dtstart = BmDateTimeWrapper.create(eventDate, Precision.DateTime);
		exception.dtend = BmDateTimeWrapper.create(eventDate.plusHours(1), Precision.DateTime);
		BmDateTime recurid = BmDateTimeWrapper.create(plannedDate, Precision.DateTime);
		this.event.occurrences = Stream
				.concat(this.event.occurrences.stream(),
						Arrays.asList(VEventOccurrence.fromEvent(exception, recurid)).stream())
				.collect(Collectors.toList());
		return this;
	}
}