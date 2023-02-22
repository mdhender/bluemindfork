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
package net.bluemind.calendar.helper.ical4j;

import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.api.VFreebusy;
import net.bluemind.calendar.api.VFreebusy.Type;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.Status;

public class VFreebusyServiceHelperTest {

	/**
	 * @return
	 */
	protected VEventSeries defaultVEvent(ZonedDateTime date) {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeWrapper.create(date, Precision.DateTime);
		event.dtstart = BmDateTimeWrapper.create(date.plusHours(1), Precision.DateTime);
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.organizer = new VEvent.Organizer("login@bm.lan");

		List<VEvent.Attendee> attendees = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef", null, null, null,
				"external@attendee.lan");
		attendees.add(me);

		event.attendees = attendees;

		series.main = event;
		return series;
	}

	@Test
	public void eventStatusToFreebusyType() {
		ItemValue<VEvent> event1 = ItemValue.create("uid", defaultVEvent(ZonedDateTime.now()).main);
		ItemValue<VEvent> event2 = ItemValue.create("uid", defaultVEvent(ZonedDateTime.now().plusHours(2)).main);
		event2.value.status = Status.Tentative;
		ItemValue<VEvent> event3 = ItemValue.create("uid", defaultVEvent(ZonedDateTime.now().plusHours(4)).main);
		event3.value.status = Status.NeedsAction;
		ItemValue<VEvent> event4 = ItemValue.create("uid", defaultVEvent(ZonedDateTime.now().plusHours(6)).main);
		event4.value.status = Status.Cancelled;

		BmDateTime start = BmDateTimeWrapper.create(ZonedDateTime.now().minusDays(2), Precision.DateTime);
		BmDateTime end = BmDateTimeWrapper.create(ZonedDateTime.now().plusDays(2), Precision.DateTime);

		VFreebusy freebusy = VFreebusyServiceHelper.convertToFreebusy("bm.lan", "login@bm.lan", start, end,
				Arrays.asList(event1, event2, event3, event4), Collections.emptyList());
		assertEquals("Confirmed event must be converted to BUSY slot", Type.BUSY, freebusy.slots.get(0).type);
		assertEquals("Tentative event must be converted to BUSYTENTATIVE slott", Type.BUSYTENTATIVE,
				freebusy.slots.get(1).type);
		assertEquals("Confirmed event must be converted to BUSYTENTATIVE slot", Type.BUSYTENTATIVE,
				freebusy.slots.get(2).type);
		assertEquals("Confirmed event must be converted to FREE slot", Type.FREE, freebusy.slots.get(3).type);

	}

	@Test
	public void attendeeStatusToFreebusyType() {
		ItemValue<VEvent> event1 = ItemValue.create("uid", defaultVEvent(ZonedDateTime.now()).main);
		ItemValue<VEvent> event2 = ItemValue.create("uid", defaultVEvent(ZonedDateTime.now().plusHours(2)).main);
		event2.value.status = Status.Tentative;
		ItemValue<VEvent> event3 = ItemValue.create("uid", defaultVEvent(ZonedDateTime.now().plusHours(6)).main);
		event3.value.status = Status.Cancelled;

		BmDateTime start = BmDateTimeWrapper.create(ZonedDateTime.now().minusDays(2), Precision.DateTime);
		BmDateTime end = BmDateTimeWrapper.create(ZonedDateTime.now().plusDays(2), Precision.DateTime);

		// Attendee partstatus is declined
		ParticipationStatus participation = ParticipationStatus.Declined;
		List<VEvent.Attendee> attendees = Arrays
				.asList(VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair, participation, true, "",
						"", "", "osef", "bm://bm.lan/user/freebusyOwnerUid", null, null, "external@attendee.lan"));
		event1.value.attendees = attendees;
		event2.value.attendees = attendees;
		event3.value.attendees = attendees;

		VFreebusy freebusy = VFreebusyServiceHelper.convertToFreebusy("bm.lan", "freebusyOwnerUid", start, end,
				Arrays.asList(event1, event2, event3), Collections.emptyList());
		assertEquals(Type.FREE, freebusy.slots.get(0).type);
		assertEquals(Type.FREE, freebusy.slots.get(1).type);
		assertEquals(Type.FREE, freebusy.slots.get(2).type);

		// Attendee partstatus is tentative
		participation = ParticipationStatus.Tentative;
		attendees = Arrays.asList(VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair, participation,
				true, "", "", "", "osef", "bm://bm.lan/user/freebusyOwnerUid", null, null, "external@attendee.lan"));
		event1.value.attendees = attendees;
		event2.value.attendees = attendees;
		event3.value.attendees = attendees;
		freebusy = VFreebusyServiceHelper.convertToFreebusy("bm.lan", "freebusyOwnerUid", start, end,
				Arrays.asList(event1, event2, event3), Collections.emptyList());
		assertEquals(Type.BUSYTENTATIVE, freebusy.slots.get(0).type);
		assertEquals(Type.BUSYTENTATIVE, freebusy.slots.get(1).type);
		assertEquals(Type.FREE, freebusy.slots.get(2).type);

		// Attendee partstatus is needs-action
		participation = ParticipationStatus.NeedsAction;
		attendees = Arrays.asList(VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair, participation,
				true, "", "", "", "osef", "bm://bm.lan/user/freebusyOwnerUid", null, null, "external@attendee.lan"));
		event1.value.attendees = attendees;
		event2.value.attendees = attendees;
		event3.value.attendees = attendees;
		freebusy = VFreebusyServiceHelper.convertToFreebusy("bm.lan", "freebusyOwnerUid", start, end,
				Arrays.asList(event1, event2, event3), Collections.emptyList());
		assertEquals(Type.BUSYTENTATIVE, freebusy.slots.get(0).type);
		assertEquals(Type.BUSYTENTATIVE, freebusy.slots.get(1).type);
		assertEquals(Type.FREE, freebusy.slots.get(2).type);

		// Attendee partstatus is accepted
		participation = ParticipationStatus.Accepted;
		attendees = Arrays.asList(VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair, participation,
				true, "", "", "", "osef", "bm://bm.lan/user/freebusyOwnerUid", null, null, "external@attendee.lan"));
		event1.value.attendees = attendees;
		event2.value.attendees = attendees;
		event3.value.attendees = attendees;
		freebusy = VFreebusyServiceHelper.convertToFreebusy("bm.lan", "freebusyOwnerUid", start, end,
				Arrays.asList(event1, event2, event3), Collections.emptyList());
		assertEquals(Type.BUSY, freebusy.slots.get(0).type);
		assertEquals(Type.BUSYTENTATIVE, freebusy.slots.get(1).type);
		assertEquals(Type.FREE, freebusy.slots.get(2).type);
	}

}
