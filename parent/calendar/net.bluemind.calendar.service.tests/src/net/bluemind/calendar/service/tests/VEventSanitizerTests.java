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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.service.internal.VEventSanitizer;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class VEventSanitizerTests {

	private BmTestContext test1Context;

	@Before
	public void beforeBefore() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		String domainUid = "bm.lan";

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);

		PopulateHelper.createTestDomain(domainUid, esServer);

		BmTestContext systemContext = new BmTestContext(SecurityContext.SYSTEM);
		IUser users = systemContext.provider().instance(IUser.class, domainUid);
		users.create("test1", defaultUser("test1"));
		users.create("test2", defaultUser("test2"));

		test1Context = new BmTestContext(
				new SecurityContext("test1", "test1", Arrays.<String>asList("g1"), Arrays.<String>asList(), "bm.lan"));

		IGroup groups = systemContext.provider().instance(IGroup.class, domainUid);
		groups.create("g1", defaultGroup("g1", PopulateHelper.FAKE_CYRUS_IP));
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
		em.address = login + "@bm.lan";
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

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, "bm.lan");

		VEvent vevent = new VEvent();

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(new DateTime(2015, 05, 01, 0, 0, 0), Precision.Date);
		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();

		sanitizer.sanitize(vevent);
		// fixed
		assertNull(vevent.priority);
		assertTrue(vevent.allDay());
		assertEquals(VEvent.Transparency.Transparent, vevent.transparency);

	}

	@Test
	public void testOrganizerWithoutEmailCannotCreateMeeting() throws ServerFault {

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, "bm.lan");

		VEvent vevent = new VEvent();

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(new DateTime(2015, 05, 01, 0, 0, 0), Precision.Date);
		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();

		vevent.organizer = new Organizer();
		vevent.organizer.commonName = "check";
		vevent.organizer.mailto = null;
		vevent.attendees = Arrays.asList(simpleAttendee());
		sanitizer.sanitize(vevent);

		assertEquals(0, vevent.attendees.size());
		assertNull(vevent.organizer);
	}

	@Test
	public void testSanitizeOrganizer() throws ServerFault {

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, "bm.lan");

		VEvent vevent = new VEvent();

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(new DateTime(2015, 05, 01, 0, 0, 0), Precision.Date);
		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();

		vevent.organizer = new Organizer();
		vevent.organizer.commonName = "check";
		vevent.organizer.mailto = "test1@bm.lan";
		vevent.attendees = Arrays.asList(simpleAttendee());
		sanitizer.sanitize(vevent);

		assertEquals("test1@bm.lan", vevent.organizer.mailto);
		assertEquals("test1 test1", vevent.organizer.commonName);
		assertEquals("bm://bm.lan/users/test1", vevent.organizer.dir);

		vevent = new VEvent();

		vevent.dtstart = BmDateTimeWrapper.create(new DateTime(2015, 05, 01, 0, 0, 0), Precision.Date);
		vevent.summary = "event " + System.currentTimeMillis();
		vevent.organizer = new Organizer();
		vevent.organizer.commonName = "check";
		vevent.organizer.dir = "bm://bm.lan/users/test1";
		vevent.attendees = Arrays.asList(simpleAttendee());
		sanitizer.sanitize(vevent);

		assertEquals("test1 test1", vevent.organizer.commonName);
		assertEquals("test1@bm.lan", vevent.organizer.mailto);
		assertEquals(1, vevent.attendees.size());

		// organizer not in system
		vevent.organizer = new Organizer();
		vevent.organizer.commonName = "check";
		vevent.organizer.mailto = "fake@bm.lan";
		sanitizer.sanitize(vevent);

		assertEquals("fake@bm.lan", vevent.organizer.mailto);
		assertEquals("check", vevent.organizer.commonName);
		assertNull(vevent.organizer.dir);

		// organizer not in system
		vevent.organizer = new Organizer();
		vevent.organizer.commonName = "check";
		vevent.organizer.mailto = "fake@bm.lan";
		vevent.organizer.dir = "bm://bm.lan/users/fake";
		sanitizer.sanitize(vevent);

		assertEquals("fake@bm.lan", vevent.organizer.mailto);
		assertEquals("check", vevent.organizer.commonName);
		assertNull(vevent.organizer.dir);

		// organizer is removed if attendees.size == 0
		vevent = new VEvent();

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(new DateTime(2015, 05, 01, 0, 0, 0), Precision.Date);
		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();

		vevent.organizer = new Organizer();
		vevent.organizer.commonName = "check";
		vevent.organizer.mailto = "test1@bm.lan";
		sanitizer.sanitize(vevent);
		assertNull(vevent.organizer);
	}

	@Test
	public void testSanitizeOrganizerInvalidMailto() throws ServerFault {

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, "bm.lan");

		VEvent vevent = new VEvent();

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(new DateTime(2015, 05, 01, 0, 0, 0), Precision.Date);
		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();

		vevent.organizer = new Organizer();
		vevent.organizer.commonName = "check";
		vevent.organizer.mailto = "test1";
		vevent.attendees = Arrays.asList(simpleAttendee());
		sanitizer.sanitize(vevent);
		assertNull(vevent.organizer);
		assertTrue(vevent.attendees.isEmpty());
	}

	private Attendee simpleAttendee() {
		Attendee attendee = new VEvent.Attendee();
		attendee.commonName = "check";
		attendee.mailto = "test2@bm.lan";
		attendee.partStatus = ParticipationStatus.NeedsAction;
		return attendee;
	}

	@Test
	public void testSanitizeAttendee() throws ServerFault {

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, "bm.lan");

		VEvent vevent = new VEvent();

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(new DateTime(2015, 05, 01, 0, 0, 0), Precision.Date);
		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();

		Attendee attendee = new VEvent.Attendee();
		attendee.commonName = "check";
		attendee.mailto = "test1@bm.lan";
		vevent.attendees = Arrays.asList(attendee);
		vevent.organizer = new Organizer("chef@bad-company.com");
		sanitizer.sanitize(vevent);

		assertEquals(1, vevent.attendees.size());
		assertEquals("test1@bm.lan", vevent.attendees.get(0).mailto);
		assertEquals("test1 test1", vevent.attendees.get(0).commonName);
		assertEquals("bm://bm.lan/users/test1", vevent.attendees.get(0).dir);

		vevent = new VEvent();

		vevent.dtstart = BmDateTimeWrapper.create(new DateTime(2015, 05, 01, 0, 0, 0), Precision.Date);
		vevent.summary = "event " + System.currentTimeMillis();
		attendee = new VEvent.Attendee();
		attendee.commonName = "check";
		attendee.mailto = "fake@gmail.com";
		attendee.dir = "bm://bm.lan/users/test1";
		vevent.attendees = Arrays.asList(attendee);
		vevent.organizer = new Organizer("chef@bad-company.com");
		sanitizer.sanitize(vevent);

		// BM-9907 returns user default email
		assertEquals("test1@bm.lan", vevent.attendees.get(0).mailto);
		assertEquals("test1 test1", vevent.attendees.get(0).commonName);
		assertEquals("bm://bm.lan/users/test1", vevent.attendees.get(0).dir);

		// attendee not in system
		vevent = new VEvent();

		vevent.dtstart = BmDateTimeWrapper.create(new DateTime(2015, 05, 01, 0, 0, 0), Precision.Date);
		vevent.summary = "event " + System.currentTimeMillis();
		attendee = new VEvent.Attendee();
		attendee.commonName = "check";
		attendee.mailto = "fake@bm.lan";
		attendee.dir = "fake://test";
		vevent.attendees = Arrays.asList(attendee);
		vevent.organizer = new Organizer("chef@bad-company.com");

		sanitizer.sanitize(vevent);
		// not modified
		assertEquals("fake@bm.lan", vevent.attendees.get(0).mailto);
		assertEquals("check", vevent.attendees.get(0).commonName);
		assertNull(vevent.attendees.get(0).dir);

		// attendee not in system (with dir)
		vevent = new VEvent();

		vevent.dtstart = BmDateTimeWrapper.create(new DateTime(2015, 05, 01, 0, 0, 0), Precision.Date);
		vevent.summary = "event " + System.currentTimeMillis();
		attendee = new VEvent.Attendee();
		attendee.commonName = "check";
		attendee.mailto = "fake@bm.lan";
		attendee.dir = "bm://bm.lan/users/fake";
		vevent.attendees = Arrays.asList(attendee);
		vevent.organizer = new Organizer("chef@bad-company.com");
		sanitizer.sanitize(vevent);

		assertEquals("fake@bm.lan", vevent.attendees.get(0).mailto);
		assertEquals("check", vevent.attendees.get(0).commonName);
		assertNull(vevent.attendees.get(0).dir);

	}

	@Test
	public void testSanitizeAttendeeWithInvalidEmail() throws ServerFault {

		// attendee with invalid email and without commonName
		VEvent vevent = new VEvent();

		vevent.dtstart = BmDateTimeWrapper.create(new DateTime(2015, 05, 01, 0, 0, 0), Precision.Date);
		vevent.summary = "event " + System.currentTimeMillis();
		Attendee attendee = new VEvent.Attendee();
		attendee.commonName = null;
		attendee.mailto = "fake_/email.com";
		attendee.dir = "fake://test";
		vevent.attendees = Arrays.asList(attendee);
		vevent.organizer = new Organizer("chef@bad-company.com");

		new VEventSanitizer(test1Context, "bm.lan").sanitize(vevent);
		assertEquals("fake_/email.com", attendee.commonName);
		assertNull(attendee.mailto);
	}

	@Test
	public void testSanitizeExDate() throws ServerFault {

		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, "bm.lan");

		VEvent vevent = new VEvent();

		// dtstart != null
		vevent.dtstart = BmDateTimeWrapper.create(new DateTime(2015, 05, 01, 0, 0, 0), Precision.Date);
		// summary != null
		vevent.summary = "event " + System.currentTimeMillis();
		vevent.exdate = ImmutableSet.of(BmDateTimeWrapper.create(new DateTime(2015, 05, 02, 0, 0, 0), Precision.Date));
		sanitizer.sanitize(vevent);
		// fixed
		assertNull(vevent.exdate);

	}

	@Test
	public void testSanitizeOrganizerIsTheOnlyAttendee() {
		VEventSanitizer sanitizer = new VEventSanitizer(test1Context, "bm.lan");

		VEvent vevent = new VEvent();
		vevent.dtstart = BmDateTimeWrapper.create(new DateTime(2015, 05, 01, 0, 0, 0), Precision.Date);
		vevent.summary = "event " + System.currentTimeMillis();
		Attendee attendee = new VEvent.Attendee();
		attendee.commonName = "test1";
		attendee.mailto = "test1@bm.lan";
		vevent.attendees = Arrays.asList(attendee);
		vevent.organizer = new Organizer("test1@bm.lan");

		VEventSeries series = VEventSeries.create(vevent);

		sanitizer.sanitize(series);

		assertNull(vevent.organizer);
		assertTrue(vevent.attendees.isEmpty());

	}
}
