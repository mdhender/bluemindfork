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
package net.bluemind.calendar.service.tests;

import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import net.bluemind.calendar.api.CalendarsVEventQuery;
import net.bluemind.calendar.api.ICalendars;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.service.AbstractCalendarTests;
import net.bluemind.calendar.service.internal.CalendarsService;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.tests.BmTestContext;

public class CalendarsServiceTests extends AbstractCalendarTests {

	ZoneId parisTz = ZoneId.of("Europe/Paris");

	protected ICalendars getCalendarsService(SecurityContext context) {
		return new CalendarsService(new BmTestContext(context));
	}

	@Test
	public void testSearch() {
		VEventSeries event = defaultVEvent();
		event.main.summary = "toto";

		String uid = "test_" + System.nanoTime();
		VEventQuery eventQuery = VEventQuery.create("value.summary:toto");

		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, event, sendNotifications);

		CalendarsVEventQuery query = CalendarsVEventQuery.create(eventQuery, Arrays.asList(userCalendarContainer.uid));

		List<ItemContainerValue<VEventSeries>> res = getCalendarsService(userSecurityContext).search(query);

		assertEquals(1, res.size());
		VEvent found = res.get(0).value.main;
		assertEquals(event.main.summary, found.summary);

		query = CalendarsVEventQuery.create(eventQuery, testUser.uid);

		res = getCalendarsService(userSecurityContext).search(query);

		assertEquals(1, res.size());
		found = res.get(0).value.main;
		assertEquals(event.main.summary, found.summary);

	}

}
