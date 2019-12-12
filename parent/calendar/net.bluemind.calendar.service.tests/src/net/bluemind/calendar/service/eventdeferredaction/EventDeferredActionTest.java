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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.service.eventdeferredaction.EventDeferredAction;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;

public class EventDeferredActionTest {
	@Test
	public void isRecurringEventTest() {
		ZonedDateTime eventDate = ZonedDateTime.now();
		VEventSeries event = EventCreator.defaultVEvent(eventDate).withRecurrence(Frequency.DAILY).event;
		EventDeferredAction deferredAction = new EventDeferredAction(event.main, 0);
		assertTrue(deferredAction.isRecurringEvent());

		event = EventCreator.defaultVEvent(eventDate).event;
		deferredAction = new EventDeferredAction(event.main, 0);
		assertFalse(deferredAction.isRecurringEvent());
	}

	@Test
	public void getVAlarmTest() {
		ZonedDateTime eventDate = ZonedDateTime.now();
		VEventSeries event = EventCreator.defaultVEvent(eventDate).withAlarm(-600).withAlarm(-1200)
				.withAlarm(-1800).event;

		Integer trigger = -600;
		EventDeferredAction eventDeferredAction = new EventDeferredAction(event.main, trigger);
		assertEquals(trigger, eventDeferredAction.valarm.trigger);

		eventDeferredAction = new EventDeferredAction(event.main, 0);
		assertNull(eventDeferredAction.valarm);

		event = EventCreator.defaultVEvent(eventDate).event;
		eventDeferredAction = new EventDeferredAction(event.main, -600);
		assertNull(eventDeferredAction.valarm);
	}

	@Test
	public void alarmTriggerDateTest() {
		Integer trigger = -600;
		ZonedDateTime eventDate = ZonedDateTime.now();
		VEventSeries event = EventCreator.defaultVEvent(eventDate).withAlarm(trigger).event;
		EventDeferredAction eventDeferredAction = new EventDeferredAction(event.main, trigger);

		ZonedDateTime alarmTriggerDate = eventDeferredAction.getTriggerDate(eventDate);
		assertEquals(eventDate.plusSeconds(trigger), alarmTriggerDate);
	}

	@Test
	public void nextExecutionDateTest() {
		int trigger = -600;
		ZonedDateTime eventDate = ZonedDateTime.now();
		VEventSeries event = EventCreator.defaultVEvent(eventDate).withAlarm(trigger)
				.withRecurrence(Frequency.YEARLY).event;
		EventDeferredAction eventDeferredAction = new EventDeferredAction(event.main, trigger);
		eventDeferredAction.executionDate = new Date(eventDate.plusSeconds(trigger).toInstant().toEpochMilli());

		Optional<ZonedDateTime> nextExecutionDate = eventDeferredAction.nextExecutionDate();
		ZonedDateTime expected = eventDate.plusYears(1).plusSeconds(trigger).truncatedTo(ChronoUnit.SECONDS);
		assertEquals(expected, nextExecutionDate.get());
	}

	@Test
	public void isNotOccurrenceExceptionTest() {
		int trigger = -600;
		ZonedDateTime eventDate = ZonedDateTime.now();

		VEventSeries event = EventCreator.defaultVEvent(eventDate).withRecurrence(Frequency.DAILY)
				.withAlarm(trigger).event;

		EventDeferredAction deferredAction = new EventDeferredAction(event.main, trigger);

		deferredAction.configuration = Collections.emptyMap();
		assertTrue(deferredAction.isNotOccurrenceException());
	}

	@Test
	public void excludeKnownOccurrencesTest() {
		int trigger = -600;
		ZonedDateTime eventDate = ZonedDateTime.now();

		ZonedDateTime plannedDate = eventDate.plusDays(1);
		VEventSeries event = EventCreator.defaultVEvent(eventDate).withRecurrence(Frequency.DAILY).withAlarm(trigger)
				.withException(plannedDate, eventDate.plusDays(1).plusHours(3)).event;

		Set<BmDateTime> actual = EventDeferredAction.excludeKnownExceptions(event, null);

		BmDateTime recurid = BmDateTimeWrapper.create(plannedDate, Precision.DateTime);
		assertEquals(new HashSet<BmDateTime>(Arrays.asList(recurid)), actual);
	}
}
