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
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import net.bluemind.calendar.api.IPublicFreebusy;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.api.VFreebusy;
import net.bluemind.calendar.api.VFreebusy.Slot;
import net.bluemind.calendar.api.VFreebusy.Type;
import net.bluemind.calendar.api.VFreebusyQuery;
import net.bluemind.calendar.service.AbstractCalendarTests;
import net.bluemind.calendar.service.internal.PublicFreebusyService;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeHelper;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.ical4j.data.CalendarBuilder;
import net.bluemind.lib.ical4j.model.PropertyFactoryRegistry;
import net.fortuna.ical4j.data.CalendarParser;
import net.fortuna.ical4j.data.CalendarParserFactory;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterFactoryRegistry;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VFreeBusy;

public class PublicFreebusyServiceTests extends AbstractCalendarTests {

	@Test
	public void testSimple() throws ServerFault, IOException, ParserException {
		VEventSeries vevent = defaultVEvent();
		vevent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.now().minusMonths(3).minusDays(2));
		vevent.main.dtend = BmDateTimeHelper.time(ZonedDateTime.now().minusMonths(3).minusDays(2).plusHours(1));

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		rrule.until = BmDateTimeHelper.time(ZonedDateTime.now().plusYears(10));
		vevent.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, vevent, sendNotifications);

		String ics = getPublicFreebusyService(SecurityContext.ANONYMOUS).simple(testUser.value.defaultEmail().address,
				userSecurityContext.getSubject(), userSecurityContext.getContainerUid());

		CalendarParser parser = CalendarParserFactory.getInstance().createParser();
		PropertyFactoryRegistry propertyFactory = new PropertyFactoryRegistry();
		ParameterFactoryRegistry parameterFactory = new ParameterFactoryRegistry();

		CalendarBuilder builder = new CalendarBuilder(parser, propertyFactory, parameterFactory,
				TimeZoneRegistryFactory.getInstance().createRegistry());

		net.fortuna.ical4j.model.Calendar calendar = builder.build(new ByteArrayInputStream(ics.getBytes()));

		VFreeBusy vfreebusy = (VFreeBusy) calendar.getComponent("VFREEBUSY");
		assertNotNull(vfreebusy);
		PropertyList slots = vfreebusy.getProperties("FREEBUSY");

		int count = 0;
		for (int i = 0; i < slots.size(); i++) {
			Property slot = (Property) slots.get(i);
			Parameter type = slot.getParameter("FBTYPE");
			if (type.getValue().equals("BUSY")) {
				count++;
			}
		}

		assertEquals(3, count);

	}

	@Test
	public void testGet() throws ServerFault {
		VEventSeries vevent = defaultVEvent();

		vevent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2017, 2, 13, 8, 0, 0, 0, tz));
		vevent.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2017, 2, 13, 10, 0, 0, 0, tz));

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.DAILY;
		rrule.interval = 1;
		rrule.until = BmDateTimeHelper.time(ZonedDateTime.now().plusYears(10));
		vevent.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, vevent, sendNotifications);

		ZonedDateTime start = ZonedDateTime.of(2017, 2, 13, 0, 0, 0, 0, tz);
		ZonedDateTime end = ZonedDateTime.of(2017, 2, 18, 0, 0, 0, 0, tz);

		int days = (int) ChronoUnit.DAYS.between(start, end);

		VFreebusyQuery query = VFreebusyQuery.create(BmDateTimeWrapper.create(start, Precision.DateTime),
				BmDateTimeWrapper.create(end, Precision.DateTime));
		VFreebusy fb = getPublicFreebusyService(SecurityContext.ANONYMOUS).get(testUser.value.defaultEmail().address,
				userSecurityContext.getSubject(), userSecurityContext.getContainerUid(), query);

		// 2 BUSY slots per day (out of office) + 5 BUSY slot for each event
		// occurrences
		assertEquals(days * 2 + 5, fb.slots.size());

		int i = 0;
		for (Slot slot : fb.slots) {
			if (slot.type == Type.BUSY) {
				i++;
			}
		}

		// one event per day
		assertEquals(days, i);
	}

	@Test
	public void testGetAstString() throws ServerFault, IOException, ParserException {
		VEventSeries vevent = defaultVEvent();
		vevent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.now().minusMonths(3).minusDays(2));
		vevent.main.dtend = BmDateTimeHelper.time(ZonedDateTime.now().minusMonths(3).minusDays(2).plusHours(1));

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		rrule.until = BmDateTimeHelper.time(ZonedDateTime.now().plusYears(10));
		vevent.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, vevent, sendNotifications);

		ZonedDateTime start = ZonedDateTime.now().minusMonths(3);
		ZonedDateTime end = ZonedDateTime.now().plusMonths(3);

		VFreebusyQuery query = VFreebusyQuery.create(BmDateTimeWrapper.create(start, Precision.DateTime),
				BmDateTimeWrapper.create(end, Precision.DateTime));
		String ics = getPublicFreebusyService(SecurityContext.ANONYMOUS).getAsString(
				testUser.value.defaultEmail().address, userSecurityContext.getSubject(),
				userSecurityContext.getContainerUid(), query);

		CalendarParser parser = CalendarParserFactory.getInstance().createParser();
		PropertyFactoryRegistry propertyFactory = new PropertyFactoryRegistry();
		ParameterFactoryRegistry parameterFactory = new ParameterFactoryRegistry();

		CalendarBuilder builder = new CalendarBuilder(parser, propertyFactory, parameterFactory,
				TimeZoneRegistryFactory.getInstance().createRegistry());

		net.fortuna.ical4j.model.Calendar calendar = builder.build(new ByteArrayInputStream(ics.getBytes()));

		VFreeBusy vfreebusy = (VFreeBusy) calendar.getComponent("VFREEBUSY");
		assertNotNull(vfreebusy);
		PropertyList slots = vfreebusy.getProperties("FREEBUSY");

		int count = 0;
		for (int i = 0; i < slots.size(); i++) {
			Property slot = (Property) slots.get(i);
			Parameter type = slot.getParameter("FBTYPE");
			if (type.getValue().equals("BUSY")) {
				count++;
			}
		}

		assertEquals(6, count);

	}

	private IPublicFreebusy getPublicFreebusyService(SecurityContext sc) {
		return new PublicFreebusyService(new BmTestContext(sc));
	}

}
