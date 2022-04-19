/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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

package net.bluemind.directory.service.xfer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;

@RunWith(Parameterized.class)
public class CalendarXferTests extends AbstractMultibackendTests {
	@Parameterized.Parameters
	public static Object[][] data() {
		return new Object[10][0];
	}

	@Test
	public void testXferCalendar() {
		String container = ICalendarUids.defaultUserCalendar(userUid);

		ICalendar service = ServerSideServiceProvider.getProvider(context).instance(ICalendar.class, container);

		VEventSeries new1 = defaultVEvent();
		VEventSeries new2 = defaultVEvent();
		String new1Id = "test_1" + System.nanoTime();
		String new2Id = "test_2" + System.nanoTime();

		VEventSeries update = defaultVEvent();
		String updateUID = "test_" + System.nanoTime();
		service.create(updateUID, update, false); // v1
		update.main.summary = "update" + System.currentTimeMillis();

		VEventSeries delete = defaultVEvent();
		String deleteUID = "test_" + System.nanoTime();
		service.create(deleteUID, delete, false); // v2

		VEventChanges.ItemAdd add1 = VEventChanges.ItemAdd.create(new1Id, new1, false); // v2
		VEventChanges.ItemAdd add2 = VEventChanges.ItemAdd.create(new2Id, new2, false); // v4

		VEventChanges.ItemModify modify = VEventChanges.ItemModify.create(updateUID, update, false); // v5

		VEventChanges.ItemDelete itemDelete = VEventChanges.ItemDelete.create(deleteUID, false); // v6

		VEventChanges changes = VEventChanges.create(Arrays.asList(add1, add2), Arrays.asList(modify),
				Arrays.asList(itemDelete));

		service.updates(changes);

		// initial container state
		int nbItems = service.all().size();
		assertEquals(3, nbItems);
		long version = service.getVersion();
		assertEquals(6, version);

		System.err.println("Starting transfer....");
		TaskRef tr = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, domainUid)
				.xfer(userUid, shardIp);
		waitTaskEnd(tr);

		// current service should return nothing
		assertTrue(service.all().isEmpty());

		// new ICalendar instance
		service = ServerSideServiceProvider.getProvider(context).instance(ICalendar.class, container);

		assertEquals(nbItems, service.all().size());
		assertEquals(3L, service.getVersion());

		service.create("new-one", defaultVEvent(), false);

		ContainerChangeset<String> changeset = service.changeset(3L);
		System.err.println("changeset: " + changeset);
		assertEquals(1, changeset.created.size());
		assertEquals("new-one", changeset.created.get(0));
		assertTrue(changeset.updated.isEmpty());
		assertTrue(changeset.deleted.isEmpty());

	}

	protected VEventSeries defaultVEvent() {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		ZoneId tz = ZoneId.of("Asia/Ho_Chi_Minh");
		event.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, tz));
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		event.organizer = new VEvent.Organizer(userUid + "@bm.lan");

		List<VEvent.Attendee> attendees = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef", null, null, null,
				"external@attendee.lan");
		attendees.add(me);

		event.attendees = attendees;

		series.main = event;
		return series;
	}
}
