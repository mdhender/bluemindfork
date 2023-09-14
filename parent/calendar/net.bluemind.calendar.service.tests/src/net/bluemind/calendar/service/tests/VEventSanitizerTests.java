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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.calendar.api.CalendarSettingsData;
import net.bluemind.calendar.api.ICalendarSettings;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.service.internal.VEventSanitizer;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.RRule;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.icalendar.api.ICalendarElement.RRule.WeekDay;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;

public class VEventSanitizerTests {

	private BmTestContext test1Context;
	private String domainUid;
	private ZoneId defaultTz = ZoneId.systemDefault();
	private final ZonedDateTime date1 = ZonedDateTime.of(2015, 05, 01, 0, 0, 0, 0, defaultTz);
	private Container user1DefaultCalendar;

	@Before
	public void beforeBefore() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		this.domainUid = "test.lan";

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		Server imapServer = new Server();
		imapServer.tags = Collections.singletonList("mail/imap");
		imapServer.ip = PopulateHelper.FAKE_CYRUS_IP;

		PopulateHelper.initGlobalVirt(esServer, imapServer);

		PopulateHelper.createTestDomain(domainUid, esServer, imapServer);

		BmTestContext systemContext = new BmTestContext(SecurityContext.SYSTEM);
		IUser users = systemContext.provider().instance(IUser.class, domainUid);
		users.create("test1", defaultUser("test1"));
		users.create("test2", defaultUser("test2"));

		test1Context = new BmTestContext(
				new SecurityContext("test1", "test1", Arrays.<String>asList("g1"), Arrays.<String>asList(), domainUid));

		DataSource ds = DataSourceRouter.get(test1Context, ICalendarUids.defaultUserCalendar("test1"));
		ContainerStore containerStore = new ContainerStore(test1Context, ds, test1Context.getSecurityContext());
		user1DefaultCalendar = containerStore.get(ICalendarUids.defaultUserCalendar("test1"));

		IGroup groups = systemContext.provider().instance(IGroup.class, domainUid);
		groups.create("g1", defaultGroup("g1", imapServer.ip));
		groups.add("g1", Arrays.asList(Member.user("test1")));
	}

	private Group defaultGroup(String name, String cyrusIp) {
		Group g = new Group();
		g.dataLocation = cyrusIp;
		g.name = name;
		return g;
	}

	private User defaultUser(String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + this.domainUid;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.none;
		VCard card = new VCard();
		card.identification.name = Name.create(login, login, null, null, null, null);
		user.contactInfos = card;
		return user;
	}

	@Test
	public void testSanitizeSimple() throws ServerFault {

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);

		VEvent vevent = new VEvent();

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();

		sanitizer.sanitize(vevent, true);
		// fixed
		assertNull(vevent.priority);
		assertTrue(vevent.allDay());
		assertEquals(VEvent.Transparency.Transparent, vevent.transparency);

	}

	@Test
	public void testPrecisionOfDtendIsSameAsDTstart() throws ServerFault {

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);

		VEvent vevent = new VEvent();
		ZonedDateTime exDate = ZonedDateTime.of(2015, 06, 01, 0, 0, 0, 0, defaultTz);

		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		vevent.dtend = BmDateTimeWrapper.create(exDate, Precision.DateTime);

		sanitizer.sanitize(vevent, true);
		assertEquals(vevent.dtstart.precision, vevent.dtend.precision);
		assertEquals(Precision.Date, vevent.dtstart.precision);
	}

	@Test
	public void testPrecisionOfExDatesIsSameAsDTstart() throws ServerFault {

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);

		VEvent vevent = new VEvent();

		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		vevent.rrule = new RRule();
		vevent.rrule.frequency = Frequency.DAILY;
		vevent.exdate = new HashSet<>(Arrays.asList(BmDateTimeWrapper.create(date1, Precision.DateTime)));

		sanitizer.sanitize(vevent, true);
		assertEquals(vevent.dtstart.precision, vevent.exdate.iterator().next().precision);
		assertEquals(Precision.Date, vevent.dtstart.precision);
	}

	@Test
	public void testSanitizeRruleByday() throws ServerFault {

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);

		// when the FREQ rule part is not set to MONTHLY or YEARLY.
		VEvent vevent = new VEvent();
		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		vevent.rrule = new RRule();
		vevent.rrule.frequency = Frequency.WEEKLY;
		vevent.rrule.byDay = Arrays.asList(new WeekDay("MO", 2));

		sanitizer.sanitize(vevent, false);
		assertEquals(0, vevent.rrule.byDay.get(0).offset);

		// with the FREQ rule part set to YEARLY when the BYWEEKNO rule part is
		// specified.
		vevent.rrule.frequency = Frequency.YEARLY;
		vevent.rrule.byWeekNo = Arrays.asList(1, 3);
		vevent.rrule.byDay = Arrays.asList(new WeekDay("MO", 2));

		sanitizer.sanitize(vevent, false);
		assertEquals(0, vevent.rrule.byDay.get(0).offset);

		// when the FREQ rule part is set to MONTHLY.
		vevent.rrule.frequency = Frequency.MONTHLY;
		vevent.rrule.byDay = Arrays.asList(new WeekDay("MO", 2));

		sanitizer.sanitize(vevent, false);
		assertEquals(2, vevent.rrule.byDay.get(0).offset);

		// with the FREQ rule part NOT set to YEARLY when the BYWEEKNO rule part is
		// specified.
		vevent.rrule.frequency = Frequency.MONTHLY;
		vevent.rrule.byWeekNo = Arrays.asList(1, 3);
		vevent.rrule.byDay = Arrays.asList(new WeekDay("MO", 2));

		sanitizer.sanitize(vevent, false);
		assertEquals(2, vevent.rrule.byDay.get(0).offset);
	}

	@Test
	public void testOrganizerWithoutEmailCannotCreateMeeting() throws ServerFault {

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);

		VEvent vevent = new VEvent();

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();

		vevent.organizer = new Organizer();
		vevent.organizer.commonName = "check";
		vevent.organizer.mailto = null;
		vevent.attendees = Arrays.asList(simpleAttendee());
		sanitizer.sanitize(vevent, true);

		assertEquals(0, vevent.attendees.size());
		assertNull(vevent.organizer);
	}

	@Test
	public void testSanitizeOrganizer() throws ServerFault {

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);

		VEvent vevent = new VEvent();

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();

		vevent.organizer = new Organizer();
		vevent.organizer.commonName = "check";
		vevent.organizer.mailto = "test1@" + this.domainUid;
		vevent.attendees = Arrays.asList(simpleAttendee());
		sanitizer.sanitize(vevent, true);

		assertEquals("test1@" + this.domainUid, vevent.organizer.mailto);
		assertEquals("test1 test1", vevent.organizer.commonName);
		assertEquals("bm://" + this.domainUid + "/users/test1", vevent.organizer.dir);

		vevent = new VEvent();

		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		vevent.summary = "event " + System.currentTimeMillis();
		vevent.organizer = new Organizer();
		vevent.organizer.commonName = "check";
		vevent.organizer.dir = "bm://" + this.domainUid + "/users/test1";
		vevent.attendees = Arrays.asList(simpleAttendee());
		sanitizer.sanitize(vevent, true);

		assertEquals("test1 test1", vevent.organizer.commonName);
		assertEquals("test1@" + this.domainUid, vevent.organizer.mailto);
		assertEquals(1, vevent.attendees.size());

		// organizer not in system
		vevent.organizer = new Organizer();
		vevent.organizer.commonName = "check";
		vevent.organizer.mailto = "fake@" + this.domainUid;
		sanitizer.sanitize(vevent, true);

		assertEquals("fake@" + this.domainUid, vevent.organizer.mailto);
		assertEquals("check", vevent.organizer.commonName);
		assertNull(vevent.organizer.dir);

		// organizer not in system
		vevent.organizer = new Organizer();
		vevent.organizer.commonName = "check";
		vevent.organizer.mailto = "fake@" + this.domainUid;
		vevent.organizer.dir = "bm://" + this.domainUid + "/users/fake";
		sanitizer.sanitize(vevent, true);

		assertEquals("fake@" + this.domainUid, vevent.organizer.mailto);
		assertEquals("check", vevent.organizer.commonName);
		assertNull(vevent.organizer.dir);

		// organizer is removed if attendees.size == 0
		vevent = new VEvent();

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();

		vevent.organizer = new Organizer();
		vevent.organizer.commonName = "check";
		vevent.organizer.mailto = "test1@" + this.domainUid;
		sanitizer.sanitize(vevent, true);
		assertNull(vevent.organizer);
	}

	@Test
	public void testSanitizeOrganizerInvalidMailto() throws ServerFault {

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);

		VEvent vevent = new VEvent();

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();

		vevent.organizer = new Organizer();
		vevent.organizer.commonName = "check";
		vevent.organizer.mailto = "test1";
		vevent.attendees = Arrays.asList(simpleAttendee());
		sanitizer.sanitize(vevent, true);
		assertNull(vevent.organizer);
		assertTrue(vevent.attendees.isEmpty());
	}

	private Attendee simpleAttendee() {
		Attendee attendee = new VEvent.Attendee();
		attendee.commonName = "check";
		attendee.mailto = "test2@" + this.domainUid;
		attendee.partStatus = ParticipationStatus.NeedsAction;
		return attendee;
	}

	@Test
	public void testSanitizeAttendee() throws ServerFault {

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);

		VEvent vevent = new VEvent();

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();

		Attendee attendee = new VEvent.Attendee();
		attendee.commonName = "check";
		attendee.mailto = "test1@" + this.domainUid;
		vevent.attendees = Arrays.asList(attendee);
		vevent.organizer = new Organizer("chef@bad-company.com");
		sanitizer.sanitize(vevent, true);

		assertEquals(1, vevent.attendees.size());
		assertEquals("test1@" + this.domainUid, vevent.attendees.get(0).mailto);
		assertEquals("test1 test1", vevent.attendees.get(0).commonName);
		assertEquals("bm://" + this.domainUid + "/users/test1", vevent.attendees.get(0).dir);

		vevent = new VEvent();

		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		vevent.summary = "event " + System.currentTimeMillis();
		attendee = new VEvent.Attendee();
		attendee.commonName = "check";
		attendee.mailto = "fake@gmail.com";
		attendee.dir = "bm://" + this.domainUid + "/users/test1";
		vevent.attendees = Arrays.asList(attendee);
		vevent.organizer = new Organizer("chef@bad-company.com");
		sanitizer.sanitize(vevent, true);

		// BM-9907 returns user default email
		assertEquals("test1@" + this.domainUid, vevent.attendees.get(0).mailto);
		assertEquals("test1 test1", vevent.attendees.get(0).commonName);
		assertEquals("bm://" + this.domainUid + "/users/test1", vevent.attendees.get(0).dir);

		// attendee not in system
		vevent = new VEvent();

		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		vevent.summary = "event " + System.currentTimeMillis();
		attendee = new VEvent.Attendee();
		attendee.commonName = "check";
		attendee.mailto = "fake@" + this.domainUid;
		attendee.dir = "fake://test";
		vevent.attendees = Arrays.asList(attendee);
		vevent.organizer = new Organizer("chef@bad-company.com");

		sanitizer.sanitize(vevent, true);
		// not modified
		assertEquals("fake@" + this.domainUid, vevent.attendees.get(0).mailto);
		assertEquals("check", vevent.attendees.get(0).commonName);
		assertNull(vevent.attendees.get(0).dir);

		// attendee not in system (with dir)
		vevent = new VEvent();

		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		vevent.summary = "event " + System.currentTimeMillis();
		attendee = new VEvent.Attendee();
		attendee.commonName = "check";
		attendee.mailto = "fake@" + this.domainUid;
		attendee.dir = "bm://" + this.domainUid + "/users/fake";
		vevent.attendees = Arrays.asList(attendee);
		vevent.organizer = new Organizer("chef@bad-company.com");
		sanitizer.sanitize(vevent, true);

		assertEquals("fake@" + this.domainUid, vevent.attendees.get(0).mailto);
		assertEquals("check", vevent.attendees.get(0).commonName);
		assertNull(vevent.attendees.get(0).dir);

	}

	@Test
	public void testSanitizeAttendeeWithInvalidEmail() throws ServerFault {

		// attendee with invalid email and without commonName
		VEvent vevent = new VEvent();

		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		vevent.summary = "event " + System.currentTimeMillis();
		Attendee attendee = new VEvent.Attendee();
		attendee.commonName = null;
		attendee.mailto = "fake_/email.com";
		attendee.dir = "fake://test";
		vevent.attendees = Arrays.asList(attendee);
		vevent.organizer = new Organizer("chef@bad-company.com");

		new VEventSanitizer(test1Context, user1DefaultCalendar).sanitize(vevent, true);
		assertEquals("fake_/email.com", attendee.commonName);
		assertNull(attendee.mailto);

		attendee.mailto = "CAPITALS@CAPITALS.CA";
		vevent.attendees = Arrays.asList(attendee);
		new VEventSanitizer(test1Context, user1DefaultCalendar).sanitize(vevent, true);
		assertEquals("CAPITALS@CAPITALS.CA", attendee.mailto);
	}

	@Test
	public void testSanitizeExDate() throws ServerFault {

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);

		VEvent vevent = new VEvent();

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();
		vevent.exdate = ImmutableSet
				.of(BmDateTimeWrapper.create(ZonedDateTime.of(2015, 05, 02, 0, 0, 0, 0, defaultTz), Precision.Date));
		sanitizer.sanitize(vevent, true);
		// fixed
		assertNull(vevent.exdate);

	}

	@Test
	public void testSanitizeOrganizerIsTheOnlyAttendee() {
		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);

		VEvent vevent = new VEvent();
		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		vevent.summary = "event " + System.currentTimeMillis();
		Attendee attendee = new VEvent.Attendee();
		attendee.commonName = "test1";
		attendee.mailto = "test1@" + this.domainUid;
		vevent.attendees = Arrays.asList(attendee);
		vevent.organizer = new Organizer("test1@" + this.domainUid);

		VEventSeries series = VEventSeries.create(vevent);

		sanitizer.sanitize(series, true);

		assertNull(vevent.organizer);
		assertTrue(vevent.attendees.isEmpty());

	}

	@Test
	public void draftIsSetToFalseIfNotificationsAreSent() throws ServerFault {
		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);
		VEvent vevent = new VEvent();
		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		vevent.summary = "event " + System.currentTimeMillis();
		vevent.draft = true;
		VEventOccurrence exception = VEventOccurrence.fromEvent(vevent, vevent.dtstart);
		exception.draft = true;
		VEventSeries series = VEventSeries.create(vevent, exception);
		sanitizer.sanitize(series, true);
		assertFalse(series.main.draft);
		assertFalse(series.occurrences.get(0).draft);
	}

	@Test
	public void draftExceptionIsADraft() throws ServerFault {
		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);
		VEvent vevent = new VEvent();
		vevent.dtstart = BmDateTimeWrapper.create(date1, Precision.Date);
		vevent.summary = "event " + System.currentTimeMillis();
		vevent.draft = true;
		VEventOccurrence exception = VEventOccurrence.fromEvent(vevent, vevent.dtstart);
		exception.draft = false;
		VEventSeries series = VEventSeries.create(vevent, exception);
		sanitizer.sanitize(series, false);
		assertTrue(series.main.draft);
	}

	@Test
	public void testSanitizeTimezoneUsingCalendarSettings() throws ServerFault {
		ICalendarSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ICalendarSettings.class, user1DefaultCalendar.uid);
		CalendarSettingsData calendarSettingsData = settings.get();
		calendarSettingsData.dayStart = 8;
		calendarSettingsData.dayEnd = 18;
		calendarSettingsData.minDuration = 30;
		calendarSettingsData.workingDays = Arrays.asList(CalendarSettingsData.Day.MO, CalendarSettingsData.Day.TH);
		calendarSettingsData.timezoneId = "Europe/Berlin";
		settings.set(calendarSettingsData);

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);
		VEvent vevent = new VEvent();
		vevent.dtstart = new BmDateTime("2019-12-03T10:15:30+01:00", "Europe/Paris", Precision.DateTime);
		vevent.summary = "event " + System.currentTimeMillis();

		sanitizer.sanitize(vevent, true);

		assertEquals("Europe/Berlin", vevent.dtstart.timezone);
	}

	@Test
	public void testSanitizeTimezoneUsingUserSetting() throws ServerFault {
		IUserSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, user1DefaultCalendar.domainUid);
		Map<String, String> setting = settings.get("test1");
		setting.put("timezone", "Europe/Oslo");
		settings.set("test1", setting);

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);
		VEvent vevent = new VEvent();
		vevent.dtstart = new BmDateTime("2019-12-03T10:15:30+01:00", "Europe/Paris", Precision.DateTime);
		vevent.summary = "event " + System.currentTimeMillis();

		sanitizer.sanitize(vevent, true);

		assertEquals("Europe/Oslo", vevent.dtstart.timezone);
	}

	@Test
	public void testSanitizeTimezoneUsingDomainSetting() throws ServerFault {
		IDomainSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, user1DefaultCalendar.domainUid);
		Map<String, String> setting = settings.get();
		setting.put("timezone", "Europe/Ljubljana");
		settings.set(setting);

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);
		VEvent vevent = new VEvent();
		vevent.dtstart = new BmDateTime("2019-12-03T10:15:30+01:00", "Europe/Paris", Precision.DateTime);
		vevent.summary = "event " + System.currentTimeMillis();

		sanitizer.sanitize(vevent, true);

		assertEquals("Europe/Ljubljana", vevent.dtstart.timezone);
	}

	@Test
	public void testSanitizeNonMatchingTimezoneUsingDomainSettingShouldNotChangeTimezone() throws ServerFault {
		IDomainSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, user1DefaultCalendar.domainUid);
		Map<String, String> setting = settings.get();
		setting.put("timezone", "America/Asuncion");
		settings.set(setting);

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, user1DefaultCalendar);
		VEvent vevent = new VEvent();
		vevent.dtstart = new BmDateTime("2019-12-03T10:15:30+01:00", "America/La_Paz", Precision.DateTime);
		vevent.summary = "event " + System.currentTimeMillis();

		sanitizer.sanitize(vevent, true);

		assertEquals("America/La_Paz", vevent.dtstart.timezone);
	}

}
