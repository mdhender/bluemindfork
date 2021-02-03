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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.CalendarSettingsData;
import net.bluemind.calendar.api.CalendarSettingsData.Day;
import net.bluemind.calendar.api.ICalendarSettings;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.calendar.api.IFreebusyMgmt;
import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.calendar.api.IVFreebusy;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.api.VFreebusy;
import net.bluemind.calendar.api.VFreebusy.Slot;
import net.bluemind.calendar.api.VFreebusy.Type;
import net.bluemind.calendar.api.VFreebusyQuery;
import net.bluemind.calendar.service.AbstractCalendarTests;
import net.bluemind.calendar.service.internal.VFreebusyService;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;
import net.bluemind.user.api.IUserSettings;

public class VFreebusyServiceTests extends AbstractCalendarTests {

	ZoneId tz = ZoneId.of("UTC");
	ZoneId defaultTz = ZoneId.systemDefault();

	@Test
	public void testGetAsString() throws ServerFault {
		VEventSeries vevent = defaultVEvent();
		vevent.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 10, 0, 0, 0, tz),
				Precision.DateTime);
		vevent.main.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 12, 0, 0, 0, tz), Precision.DateTime);

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		rrule.until = BmDateTimeHelper.time(ZonedDateTime.of(2022, 12, 31, 12, 0, 0, 0, defaultTz));
		vevent.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, vevent, sendNotifications);

		ZonedDateTime dtstart = ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, tz);
		ZonedDateTime dtend = ZonedDateTime.of(2014, 12, 31, 0, 0, 0, 0, tz);

		String vfreebusy = getVFreebusyService(userSecurityContext, userFreebusyContainer).getAsString(
				VFreebusyQuery.create(BmDateTimeHelper.time(dtstart, false), BmDateTimeHelper.time(dtend, false)));

		System.err.println(vfreebusy);

		assertTrue(vfreebusy.contains("BEGIN:VFREEBUSY"));
		assertTrue(vfreebusy.contains("DTSTAMP:"));
		assertTrue(vfreebusy.contains("DTSTART:20140101T000000"));
		assertTrue(vfreebusy.contains("DTEND:20141231T000000"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140101T100000Z/20140101T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140201T100000Z/20140201T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140301T100000Z/20140301T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140401T100000Z/20140401T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140501T100000Z/20140501T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140601T100000Z/20140601T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140701T100000Z/20140701T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140801T100000Z/20140801T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140901T100000Z/20140901T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20141001T100000Z/20141001T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20141101T100000Z/20141101T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20141201T100000Z/20141201T120000Z"));
		assertTrue(vfreebusy.contains("END:VFREEBUSY"));
	}

	@Test
	public void testOutOfOffice() throws ServerFault, IOException {
		ZonedDateTime dtstart = ZonedDateTime.of(2016, 6, 6, 0, 0, 0, 0, tz);
		ZonedDateTime dtend = ZonedDateTime.of(2016, 6, 13, 0, 0, 0, 0, tz);

		String vfreebusy = getVFreebusyService(userSecurityContext, userFreebusyContainer).getAsString(
				VFreebusyQuery.create(BmDateTimeHelper.time(dtstart, false), BmDateTimeHelper.time(dtend, false)));

		assertTrue(vfreebusy.contains("BEGIN:VCALENDAR"));
		assertTrue(vfreebusy.contains("BEGIN:VFREEBUSY"));
		assertTrue(vfreebusy.contains("DTSTAMP:"));
		assertTrue(vfreebusy.contains("DTSTART:20160606T000000Z"));
		assertTrue(vfreebusy.contains("DTEND:20160613T000000Z"));

		// mon
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20160605T220000Z/20160606T060000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20160606T160000Z/20160606T220000Z"));

		// tue
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20160606T220000Z/20160607T060000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20160607T160000Z/20160607T220000Z"));

		// wed
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20160607T220000Z/20160608T060000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20160608T160000Z/20160608T220000Z"));

		// thu
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20160608T220000Z/20160609T060000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20160609T160000Z/20160609T220000Z"));

		// fri
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20160609T220000Z/20160610T060000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20160610T160000Z/20160610T220000Z"));

		// sat
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20160611T000000Z/20160612T000000Z"));

		// sun
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:20160612T000000Z/20160613T000000Z"));

		assertTrue(vfreebusy.contains("END:VFREEBUSY"));
		assertTrue(vfreebusy.contains("END:VCALENDAR"));
	}

	@Test
	public void testOutOfOffice_UserWorkingAllDay() throws ServerFault, IOException {
		ZonedDateTime dtstart = ZonedDateTime.of(2016, 6, 6, 1, 0, 0, 0, tz);
		ZonedDateTime dtend = ZonedDateTime.of(2016, 6, 7, 2, 0, 0, 0, tz);

		IUserSettings isettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, domainUid);
		Map<String, String> settings = isettings.get(testUser.uid);
		settings.put("working_days", "mon,tue,wed,thu,fri,sat");
		settings.put("work_hours_start", "0");
		settings.put("work_hours_end", "0");
		settings.put("timezone", "Europe/Paris");

		isettings.set(testUser.uid, settings);

		String vfreebusy = getVFreebusyService(userSecurityContext, userFreebusyContainer).getAsString(
				VFreebusyQuery.create(BmDateTimeHelper.time(dtstart, false), BmDateTimeHelper.time(dtend, false)));

		assertTrue(vfreebusy.contains("BEGIN:VCALENDAR"));
		assertTrue(vfreebusy.contains("BEGIN:VFREEBUSY"));
		assertTrue(vfreebusy.contains("END:VFREEBUSY"));
		assertTrue(vfreebusy.contains("END:VCALENDAR"));
		assertFalse(vfreebusy.contains("FREEBUSY;FBTYPE"));
	}

	@Test
	public void testGet() throws ServerFault {
		VEventSeries vevent = defaultVEvent();
		vevent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 10, 0, 0, 0, defaultTz));
		vevent.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 12, 0, 0, 0, defaultTz));

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		rrule.until = BmDateTimeHelper.time(ZonedDateTime.of(2022, 12, 31, 0, 0, 0, 0, defaultTz));
		vevent.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, vevent, sendNotifications);

		ZonedDateTime dtstart = ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, defaultTz);
		ZonedDateTime dtend = ZonedDateTime.of(2014, 12, 31, 0, 0, 0, 0, defaultTz);

		VFreebusy freebusy = getVFreebusyService(userSecurityContext, userFreebusyContainer)
				.get(VFreebusyQuery.create(BmDateTimeHelper.time(dtstart, false), BmDateTimeHelper.time(dtend, false)));

		assertNotNull(freebusy);
		assertEquals(dtstart, new BmDateTimeWrapper(freebusy.dtstart).toDateTime());
		assertEquals(dtend, new BmDateTimeWrapper(freebusy.dtend).toDateTime());
		assertEquals(636, freebusy.slots.size());

		int i = 0;
		for (Slot slot : freebusy.slots) {
			if (slot.type == Type.BUSY) {
				i++;
			}
		}

		assertEquals(12, i);
	}

	@Test
	public void testGetUsingExclusions() throws ServerFault {
		VEventSeries vevent = defaultVEvent();
		vevent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 10, 0, 0, 0, defaultTz));
		vevent.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 12, 0, 0, 0, defaultTz));

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		rrule.until = BmDateTimeHelper.time(ZonedDateTime.of(2022, 12, 31, 0, 0, 0, 0, defaultTz));
		vevent.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();
		String uid2 = "test_2" + System.nanoTime();
		String uid3 = "test_2" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, vevent, sendNotifications);
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid2, vevent, sendNotifications);
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid3, vevent, sendNotifications);

		ZonedDateTime dtstart = ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, defaultTz);
		ZonedDateTime dtend = ZonedDateTime.of(2014, 12, 31, 0, 0, 0, 0, defaultTz);

		VFreebusyQuery query = VFreebusyQuery.create(BmDateTimeHelper.time(dtstart, false),
				BmDateTimeHelper.time(dtend, false));
		query.excludedEvents = Arrays.asList(uid2);
		VFreebusy freebusy = getVFreebusyService(userSecurityContext, userFreebusyContainer).get(query);

		assertNotNull(freebusy);
		assertEquals(dtstart, new BmDateTimeWrapper(freebusy.dtstart).toDateTime());
		assertEquals(dtend, new BmDateTimeWrapper(freebusy.dtend).toDateTime());
		assertEquals(648, freebusy.slots.size());

		int i = 0;
		for (Slot slot : freebusy.slots) {
			if (slot.type == Type.BUSY) {
				i++;
			}
		}

		assertEquals(12 * 2, i);
	}

	@Test
	public void testMergeFreebusy() throws Exception {
		String calUid = "test-" + System.currentTimeMillis();
		ICalendarsMgmt service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ICalendarsMgmt.class, domainUid);
		service.create(calUid, CalendarDescriptor.create("test", testUser.uid, domainUid));
		CalendarDescriptor cal = service.getComplete(calUid);
		assertNotNull(cal);

		VEventSeries vevent = defaultVEvent();
		vevent.main.dtstart = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 10, 0, 0, 0, tz),
				Precision.DateTime);
		vevent.main.dtend = BmDateTimeWrapper.create(ZonedDateTime.of(2014, 1, 1, 12, 0, 0, 0, tz), Precision.DateTime);

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		rrule.until = BmDateTimeHelper.time(ZonedDateTime.of(2022, 12, 31, 0, 0, 0, 0, defaultTz));
		vevent.main.rrule = rrule;

		ContainerStore containerStore = new ContainerStore(testContext, dataDataSource, SecurityContext.SYSTEM);
		Container calContainer = containerStore.get(calUid);
		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, calContainer).create(uid, vevent, sendNotifications);

		ZonedDateTime dtstart = ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, tz);
		ZonedDateTime dtend = ZonedDateTime.of(2014, 12, 31, 0, 0, 0, 0, tz);

		String vfreebusy = getVFreebusyService(userSecurityContext, userFreebusyContainer).getAsString(
				VFreebusyQuery.create(BmDateTimeHelper.time(dtstart, false), BmDateTimeHelper.time(dtend, false)));

		// no BUSY but BUSY-UNAVAILABLE
		assertFalse(vfreebusy.contains("FBTYPE=BUSY:"));
		assertTrue(vfreebusy.contains("FBTYPE=BUSY-UNAVAILABLE:"));

		IFreebusyMgmt fbmgmt = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IFreebusyMgmt.class, IFreebusyUids.getFreebusyContainerUid(testUser.uid));
		fbmgmt.add(calUid);

		// unknown calendar
		fbmgmt.add("dead-calendar");

		vfreebusy = getVFreebusyService(userSecurityContext, userFreebusyContainer).getAsString(
				VFreebusyQuery.create(BmDateTimeHelper.time(dtstart, false), BmDateTimeHelper.time(dtend, false)));
		assertTrue(vfreebusy.contains("BEGIN:VFREEBUSY"));
		assertTrue(vfreebusy.contains("DTSTAMP:"));
		assertTrue(vfreebusy.contains("DTSTART:20140101T000000"));
		assertTrue(vfreebusy.contains("DTEND:20141231T000000"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140101T100000Z/20140101T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140201T100000Z/20140201T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140301T100000Z/20140301T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140401T100000Z/20140401T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140501T100000Z/20140501T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140601T100000Z/20140601T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140701T100000Z/20140701T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140801T100000Z/20140801T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20140901T100000Z/20140901T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20141001T100000Z/20141001T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20141101T100000Z/20141101T120000Z"));
		assertTrue(vfreebusy.contains("FREEBUSY;FBTYPE=BUSY:20141201T100000Z/20141201T120000Z"));
		assertTrue(vfreebusy.contains("END:VFREEBUSY"));
	}

	@Test
	public void testAcl() throws Exception {
		ZonedDateTime dtstart = ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, tz);
		ZonedDateTime dtend = ZonedDateTime.of(2014, 12, 31, 0, 0, 0, 0, tz);
		String vfreebusy = getVFreebusyService(userSecurityContext, userFreebusyContainer).getAsString(
				VFreebusyQuery.create(BmDateTimeHelper.time(dtstart, false), BmDateTimeHelper.time(dtend, false)));
		assertNotNull(vfreebusy);

		try {
			vfreebusy = getVFreebusyService(attendee1SecurityContext, userFreebusyContainer).getAsString(
					VFreebusyQuery.create(BmDateTimeHelper.time(dtstart, false), BmDateTimeHelper.time(dtend, false)));
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		AclStore aclStore = new AclStore(new BmTestContext(SecurityContext.SYSTEM),
				JdbcTestHelper.getInstance().getDataSource());
		aclStore.store(userFreebusyContainer,
				Arrays.asList(AccessControlEntry.create(attendee1SecurityContext.getSubject(), Verb.Read)));

		vfreebusy = getVFreebusyService(attendee1SecurityContext, userFreebusyContainer).getAsString(
				VFreebusyQuery.create(BmDateTimeHelper.time(dtstart, false), BmDateTimeHelper.time(dtend, false)));
		assertNotNull(vfreebusy);
	}

	@Test
	public void testDeclinedEvent() throws Exception {
		Container attendee1FreebusyContainer = createTestContainer(attendee1SecurityContext, systemDataSource,
				IFreebusyUids.TYPE, "Attendee 1", IFreebusyUids.getFreebusyContainerUid(attendee1.uid), attendee1.uid);

		VEventSeries vevent = defaultVEvent();
		vevent.main.categories.clear();
		vevent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 10, 0, 0, 0, defaultTz));
		vevent.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 12, 0, 0, 0, defaultTz));

		VEvent.Attendee att = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Declined, true, "", "", "",
				attendee1.value.contactInfos.identification.formatedName.value, null, null, null,
				attendee1.value.login + "@bm.lan");
		vevent.main.attendees.add(att);

		String uid = "test_" + System.nanoTime();
		getCalendarService(attendee1SecurityContext, attendee1CalendarContainer).create(uid, vevent, sendNotifications);
		assertNotNull(getCalendarService(attendee1SecurityContext, attendee1CalendarContainer).getComplete(uid));

		VFreebusy vfreebusy = getVFreebusyService(attendee1SecurityContext, attendee1FreebusyContainer)
				.get(VFreebusyQuery.create(vevent.main.dtstart, vevent.main.dtend));

		boolean found = false;
		for (Slot s : vfreebusy.slots) {
			if (s.dtstart.equals(vevent.main.dtstart) && s.dtend.equals(vevent.main.dtend)) {
				assertEquals(Type.FREE, s.type);
				found = true;
			}
		}

		assertTrue(found);

		String ics = getVFreebusyService(attendee1SecurityContext, attendee1FreebusyContainer)
				.getAsString(VFreebusyQuery.create(vevent.main.dtstart, vevent.main.dtend));

		assertFalse(ics.contains("FREEBUSY;FBTYPE=FREE:20140101"));
	}

	@Test
	public void testTentativeEvent() throws Exception {
		Container attendee1FreebusyContainer = createTestContainer(attendee1SecurityContext, systemDataSource,
				IFreebusyUids.TYPE, "Attendee 1", IFreebusyUids.getFreebusyContainerUid(attendee1.uid), attendee1.uid);

		VEventSeries vevent = defaultVEvent();
		vevent.main.categories.clear();
		vevent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 10, 0, 0, 0, defaultTz));
		vevent.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 12, 0, 0, 0, defaultTz));

		VEvent.Attendee att = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Tentative, true, "", "", "",
				attendee1.value.contactInfos.identification.formatedName.value, null, null, null,
				attendee1.value.login + "@bm.lan");
		vevent.main.attendees.add(att);

		String uid = "test_" + System.nanoTime();
		getCalendarService(attendee1SecurityContext, attendee1CalendarContainer).create(uid, vevent, sendNotifications);
		assertNotNull(getCalendarService(attendee1SecurityContext, attendee1CalendarContainer).getComplete(uid));

		VFreebusy vfreebusy = getVFreebusyService(attendee1SecurityContext, attendee1FreebusyContainer)
				.get(VFreebusyQuery.create(vevent.main.dtstart, vevent.main.dtend));

		boolean found = false;
		for (Slot s : vfreebusy.slots) {
			if (s.dtstart.equals(vevent.main.dtstart) && s.dtend.equals(vevent.main.dtend)) {
				assertEquals(Type.BUSYTENTATIVE, s.type);
				found = true;
			}
		}

		assertTrue(found);

		String ics = getVFreebusyService(attendee1SecurityContext, attendee1FreebusyContainer)
				.getAsString(VFreebusyQuery.create(vevent.main.dtstart, vevent.main.dtend));

		assertTrue(ics.contains("FREEBUSY;FBTYPE=BUSY-TENTATIVE:20140101"));

	}

	@Test
	public void testNeedsActionEvent() throws Exception {
		Container attendee1FreebusyContainer = createTestContainer(attendee1SecurityContext, systemDataSource,
				IFreebusyUids.TYPE, "Attendee 1", IFreebusyUids.getFreebusyContainerUid(attendee1.uid), attendee1.uid);

		VEventSeries vevent = defaultVEvent();
		vevent.main.categories.clear();
		vevent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 10, 0, 0, 0, defaultTz));
		vevent.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 12, 0, 0, 0, defaultTz));

		VEvent.Attendee att = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "",
				attendee1.value.contactInfos.identification.formatedName.value, null, null, null,
				attendee1.value.login + "@bm.lan");
		vevent.main.attendees.add(att);

		String uid = "test_" + System.nanoTime();
		getCalendarService(attendee1SecurityContext, attendee1CalendarContainer).create(uid, vevent, sendNotifications);
		assertNotNull(getCalendarService(attendee1SecurityContext, attendee1CalendarContainer).getComplete(uid));

		VFreebusy vfreebusy = getVFreebusyService(attendee1SecurityContext, attendee1FreebusyContainer)
				.get(VFreebusyQuery.create(vevent.main.dtstart, vevent.main.dtend));

		boolean found = false;
		for (Slot s : vfreebusy.slots) {
			if (s.dtstart.equals(vevent.main.dtstart) && s.dtend.equals(vevent.main.dtend)) {
				assertEquals(Type.BUSYTENTATIVE, s.type);
				found = true;
			}
		}

		assertTrue(found);

		String ics = getVFreebusyService(attendee1SecurityContext, attendee1FreebusyContainer)
				.getAsString(VFreebusyQuery.create(vevent.main.dtstart, vevent.main.dtend));

		assertTrue(ics.contains("FREEBUSY;FBTYPE=BUSY-TENTATIVE:20140101"));

	}

	@Test
	public void testGetShouldPreferContainerSettingsOverUserSettings() throws ServerFault {
		VEventSeries vevent = defaultVEvent();
		vevent.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 10, 0, 0, 0, defaultTz));
		vevent.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2014, 1, 1, 12, 0, 0, 0, defaultTz));

		VEvent.RRule rrule = new VEvent.RRule();
		rrule.frequency = VEvent.RRule.Frequency.MONTHLY;
		rrule.interval = 1;
		rrule.until = BmDateTimeHelper.time(ZonedDateTime.of(2022, 12, 31, 0, 0, 0, 0, defaultTz));
		vevent.main.rrule = rrule;

		String uid = "test_" + System.nanoTime();
		getCalendarService(userSecurityContext, userCalendarContainer).create(uid, vevent, sendNotifications);

		ZonedDateTime dtstart = ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, defaultTz);
		// DTend is exclusive so the time slot is exactly 52 weeks
		ZonedDateTime dtend = ZonedDateTime.of(2014, 12, 31, 0, 0, 0, 0, defaultTz);

		VFreebusy freebusy = getVFreebusyService(userSecurityContext, userFreebusyContainer)
				.get(VFreebusyQuery.create(BmDateTimeHelper.time(dtstart, false), BmDateTimeHelper.time(dtend, false)));

		assertEquals(dtstart, new BmDateTimeWrapper(freebusy.dtstart).toDateTime());
		assertEquals(dtend, new BmDateTimeWrapper(freebusy.dtend).toDateTime());
		// 2 busy-unavailable slot by work week day + 1 busy-unavalable by
		// week-end day + 1 busy per month
		// 52 * 5 * 2 + 52 * 2 + 12
		assertEquals(636, freebusy.slots.size());

		ICalendarSettings settingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ICalendarSettings.class, userCalendarContainer.uid);
		CalendarSettingsData settings = new CalendarSettingsData();
		settings.dayStart = 4 * 3600 * 1000;
		settings.dayEnd = 14 * 3600 * 1000;
		settings.minDuration = 30;
		settings.timezoneId = "Europe/Paris";
		settings.workingDays = Arrays.asList(Day.MO, Day.TH);
		settingsService.set(settings);

		freebusy = getVFreebusyService(userSecurityContext, userFreebusyContainer)
				.get(VFreebusyQuery.create(BmDateTimeHelper.time(dtstart, false), BmDateTimeHelper.time(dtend, false)));
		// 2 busy-unavailable slot by work week day + 1 busy-unavalable by
		// week-end day + 1 busy per month
		// 52 * 2 * 2 + 52 * 5 + 12
		assertEquals(480, freebusy.slots.size());
		for (Slot s : freebusy.slots) {
			if (s.type.equals(VFreebusy.Type.BUSYUNAVAILABLE)
					&& s.dtstart.iso8601.equals("2014-01-06T00:00:00.000+01:00")) {
				assertEquals(s.dtend.iso8601, "2014-01-06T04:00:00.000+01:00");
				return;
			}
		}
		fail("Fail to match the first monday as busy-unavailable");
	}

	protected IVFreebusy getVFreebusyService(SecurityContext context, Container container) throws ServerFault {
		return new VFreebusyService(new BmTestContext(context), container);
	}

	/**
	 * Check that we allow freebusy access when default calendar is shared
	 * (BM-13947).
	 */
	@Test
	public void testFreebusyAccessOnDefaultAgendaShare() throws ServerFault, SQLException {
		final ZonedDateTime dtstart = ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, defaultTz);
		final ZonedDateTime dtend = ZonedDateTime.of(2014, 12, 31, 0, 0, 0, 0, defaultTz);

		// attendee1 has not read access on testUser freebusy (see @Before
		// AbstractCalendarTests#beforeBefore)

		// attendee1 has not read access on testUser calendar either, so for now we
		// should raise an error
		Exception exception = null;
		try {
			getVFreebusyService(attendee1SecurityContext, userFreebusyContainer).get(
					VFreebusyQuery.create(BmDateTimeHelper.time(dtstart, false), BmDateTimeHelper.time(dtend, false)));
		} catch (Exception e) {
			exception = e;
		}
		assertNotNull(exception);
		assertEquals(ServerFault.class, exception.getClass());
		assertTrue(exception.getMessage().contains("Doesnt have role Read on container freebusy"));

		// now just add a read access on testUser default calendar, we should have a
		// response
		aclStoreData.store(userCalendarContainer,
				Arrays.asList(AccessControlEntry.create(attendee1SecurityContext.getSubject(), Verb.Read)));
		VFreebusy freebusy = getVFreebusyService(attendee1SecurityContext, userFreebusyContainer)
				.get(VFreebusyQuery.create(BmDateTimeHelper.time(dtstart, false), BmDateTimeHelper.time(dtend, false)));
		assertNotNull(freebusy);
		assertTrue(!freebusy.slots.isEmpty());
	}

}
