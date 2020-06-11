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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.calendar.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;

public class AttendeesTest {

	@Test
	public void testEqualsNPE() {
		VEvent.Attendee a1 = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", null, null, null, null, null);
		VEvent.Attendee a2 = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "Attendee 2", "dir://attendee1", null, null,
				"a1@bm.lan");

		assertFalse(a1.equals(a2));
	}

	@Test
	public void testEqualsAndHashCodeAndSameDirOrMailtoAs() {
		VEvent.Attendee a1 = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "Attendee 1", "dir://attendee1", null, null,
				"a1@bm.lan");

		VEvent.Attendee a2 = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "Attendee 2", "dir://attendee1", null, null,
				"a1@bm.lan");

		assertTrue(a1.equals(a2));
		assertEquals(a1.hashCode(), a2.hashCode());
		assertTrue(a1.sameDirOrMailtoAs(a2));
		a2.mailto = "a2@bm.lan";
		// a1.dir == a2.dir
		assertTrue(a1.equals(a2));
		assertEquals(a1.hashCode(), a2.hashCode());
		assertTrue(a1.sameDirOrMailtoAs(a2));

		a2.dir = null;
		a2.mailto = "a1@bm.lan";
		assertFalse(a1.equals(a2));
		assertFalse(a1.sameDirOrMailtoAs(a2));
		a2.dir = null;
		a1.dir = null;
		// a1.mailto == a2.mailto
		assertTrue(a1.equals(a2));
		assertEquals(a1.hashCode(), a2.hashCode());
		assertTrue(a1.sameDirOrMailtoAs(a2));
	}

	@Test
	public void compareAttendees() {
		VEvent.Attendee a1 = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "Attendee 1", "dir://attendee1", null, null,
				"a1@bm.lan");

		VEvent.Attendee a2 = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "Attendee 2", "dir://attendee2", null, null,
				"a2@bm.lan");

		List<Attendee> currentAttendeeList = new ArrayList<Attendee>();
		currentAttendeeList.add(a1);
		currentAttendeeList.add(a2);

		List<Attendee> newAttendeeList = new ArrayList<Attendee>();
		newAttendeeList.addAll(currentAttendeeList);

		List<VEvent.Attendee> addedAttendees = VEvent.diff(newAttendeeList, currentAttendeeList);
		assertEquals(0, addedAttendees.size());

		List<Attendee> removedAttendees = VEvent.diff(currentAttendeeList, newAttendeeList);
		assertEquals(0, removedAttendees.size());

		a2 = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "Attendee 2", "dir://attendee2", null, null,
				"a2.update@bm.lan");
		newAttendeeList = new ArrayList<Attendee>();
		newAttendeeList.add(a1);
		newAttendeeList.add(a2);

		addedAttendees = VEvent.diff(newAttendeeList, currentAttendeeList);
		assertEquals(0, addedAttendees.size());
		removedAttendees = VEvent.diff(currentAttendeeList, newAttendeeList);
		assertEquals(0, removedAttendees.size());

		a2 = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "Attendee 2", "dir://attendee2-updated", null,
				null, "a2.update@bm.lan");
		newAttendeeList = new ArrayList<Attendee>();
		newAttendeeList.add(a1);
		newAttendeeList.add(a2);

		addedAttendees = VEvent.diff(newAttendeeList, currentAttendeeList);
		assertEquals(1, addedAttendees.size());
		assertEquals("dir://attendee2-updated", addedAttendees.get(0).dir);

		removedAttendees = VEvent.diff(currentAttendeeList, newAttendeeList);
		assertEquals(1, removedAttendees.size());
		assertEquals("dir://attendee2", removedAttendees.get(0).dir);

		a2 = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "Attendee 2", null, null, null,
				"a2.update@bm.lan");
		newAttendeeList = new ArrayList<Attendee>();
		newAttendeeList.add(a1);
		newAttendeeList.add(a2);

		addedAttendees = VEvent.diff(newAttendeeList, currentAttendeeList);
		assertEquals(1, addedAttendees.size());
		assertEquals("a2.update@bm.lan", addedAttendees.get(0).mailto);

		removedAttendees = VEvent.diff(currentAttendeeList, newAttendeeList);
		assertEquals(1, removedAttendees.size());
		assertEquals("a2@bm.lan", removedAttendees.get(0).mailto);

	}
}
