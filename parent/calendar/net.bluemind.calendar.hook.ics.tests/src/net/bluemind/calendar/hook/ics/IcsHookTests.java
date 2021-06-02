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
package net.bluemind.calendar.hook.ics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventCounter;
import net.bluemind.calendar.api.VEventCounter.CounterOriginator;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.calendar.hook.IcsHook;
import net.bluemind.calendar.hook.internal.VEventMessage;
import net.bluemind.calendar.service.internal.VEventSanitizer;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.testhelper.FakeSendmail;
import net.bluemind.core.sendmail.testhelper.TestMail;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.IDirEntryPath;
import net.bluemind.domain.api.Domain;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.RRule;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.icalendar.api.ICalendarElement.RRule.WeekDay;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.service.internal.MailboxStoreService;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;
import net.bluemind.user.persistence.UserSubscriptionStore;
import net.bluemind.user.service.internal.ContainerUserStoreService;
import net.bluemind.videoconferencing.api.IVideoConferencing;
import net.bluemind.videoconferencing.api.VideoConferencingResourceDescriptor;

// prefixes :
// o2a => organiser to attendees, 
// o2a_XXX => create
// o2a_XXX_XXX => update
// o2a_XXX_ => delete
// MasterNotAttendee$OccurrenceAttendee => attendee not in master but pressent in occurence
// a2o => attendee to organiser
public class IcsHookTests {
	private static final long NOW = System.currentTimeMillis();
	private String domainUid;
	private ContainerStore containerHome;
	private Container userContainer;
	private User user1;
	private User user2;
	protected MailboxStoreService mailboxStore;
	private ContainerUserStoreService userStoreService;
	private Container userCalendar;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		domainUid = "test.lan";

		// register elasticsearch to locator
		// Server esServer = new Server();
		// esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		// esServer.tags = Lists.newArrayList("bm/es");

		Server imapServer = new Server();
		imapServer.ip = new BmConfIni().get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer);

		ItemValue<Server> dataLocation = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).getComplete(imapServer.ip);

		containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM);
		initDomain(dataLocation, imapServer);
	}

	private void initDomain(ItemValue<Server> dataLocation, Server... servers) throws Exception {
		ItemValue<Domain> domain = PopulateHelper.createTestDomain(domainUid, servers);

		userContainer = containerHome.get(domainUid);

		userStoreService = new ContainerUserStoreService(new BmTestContext(SecurityContext.SYSTEM), userContainer,
				domain);

		Container mboxContainer = containerHome.get(domainUid);
		assertNotNull(mboxContainer);

		mailboxStore = new MailboxStoreService(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM,
				mboxContainer);

		ItemValue<User> user1Item = createTestUSer(dataLocation, "u1");
		user1 = user1Item.value;
		ItemValue<User> user2Item = createTestUSer(dataLocation, "u2");
		user2 = user2Item.value;
	}

	private ItemValue<User> createTestUSer(ItemValue<Server> dataLocation, String login)
			throws ServerFault, SQLException {
		ItemValue<User> user = defaultUser(dataLocation, login, login);
		userStoreService.create(user.uid, login, user.value);
		SecurityContext securityContext = new SecurityContext(login, login, new ArrayList<String>(),
				new ArrayList<String>(), domainUid);
		userCalendar = createTestContainer(securityContext, ICalendarUids.TYPE, user.value.login,
				ICalendarUids.TYPE + ":Default:" + user.uid, user.uid);
		Sessions.get().put(login, securityContext);
		return user;
	}

	private Container createTestContainer(SecurityContext context, String type, String login, String name, String owner)
			throws SQLException {
		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), context);
		Container container = Container.create(name, type, name, owner, "test.lan", true);
		container = containerHome.create(container);

		Container dom = containerHome.get(domainUid);
		UserSubscriptionStore userSubscriptionStore = new UserSubscriptionStore(SecurityContext.SYSTEM,
				JdbcTestHelper.getInstance().getDataSource(), dom);

		userSubscriptionStore.subscribe(context.getSubject(), container);
		return container;
	}

	private ItemValue<User> defaultUser(ItemValue<Server> dataLocation, String uid, String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@test.lan";
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.internal;
		user.dataLocation = dataLocation.uid;

		VCard card = new VCard();
		card.identification.name = Name.create("Doe", "John", null, null, null, null);
		card.identification.formatedName = FormatedName.create(login);
		user.contactInfos = card;
		return ItemValue.create(uid, user);
	}

	private ItemValue<VEventSeries> defaultVEvent(String title) {
		VEvent event = new VEvent();
		ZoneId tz = ZoneId.of("Europe/Paris");

		long now = NOW;
		long start = now + (1000 * 60 * 60);
		ZonedDateTime temp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), tz);
		event.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		temp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start + (1000 * 60 * 60)), tz);
		event.dtend = BmDateTimeWrapper.create(temp, Precision.DateTime);
		event.summary = title;
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.priority = 1;
		event.organizer = new VEvent.Organizer(null, user1.defaultEmail().address);
		event.organizer.dir = "bm://" + IDirEntryPath.path(domainUid, "u1", Kind.USER);
		event.attendees = new ArrayList<>();
		event.categories = new ArrayList<TagRef>(0);

		event.rdate = new HashSet<BmDateTime>();
		event.rdate.add(BmDateTimeWrapper.create(temp, Precision.Date));

		VEventSeries series = new VEventSeries();
		series.main = event;
		return ItemValue.create(UUID.randomUUID().toString(), series);
	}

	private ItemValue<VEventSeries> defaultRecurrentVEvent(String title) {
		ItemValue<VEventSeries> series = defaultVEvent(title);
		RRule rrule = new RRule();
		rrule.frequency = Frequency.DAILY;
		rrule.interval = 1;
		series.value.main.rrule = rrule;
		return series;
	}

	private ItemValue<VEventSeries> defaultRecurrentVEventWithException(String title) {
		ItemValue<VEventSeries> series = defaultVEvent(title);
		RRule rrule = new RRule();
		rrule.frequency = Frequency.DAILY;
		rrule.interval = 1;
		series.value.main.rrule = rrule;

		BmDateTime dtstart = series.value.main.dtstart;
		long exc = BmDateTimeWrapper.toTimestamp(dtstart.iso8601, dtstart.timezone);
		BmDateTime exceptionDate = BmDateTimeWrapper.fromTimestamp(exc + TimeUnit.DAYS.toMillis(2));
		VEventOccurrence exception = VEventOccurrence.fromEvent(series.value.main.copy(), exceptionDate);
		series.value.occurrences = Arrays.asList(exception);

		return series;
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void invite() throws ServerFault, SQLException {
		ItemValue<VEventSeries> event = defaultVEvent("invite");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan"));

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventCreated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));
		assertTrue(m.getBody() instanceof Multipart);
		Multipart body = (Multipart) m.getBody();
		for (Entity part : body.getBodyParts()) {
			if ("event.ics".equals(part.getFilename())) {
				return;
			}
		}
		fail("Did not find any ics part in the message.");
	}

	@Test
	public void inviteShouldIncludeAcceptCounterPropositions() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent("invite");
		event.value.acceptCounters = false;
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan"));

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventCreated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));
		assertTrue(m.getBody() instanceof Multipart);
		Multipart body = (Multipart) m.getBody();
		for (Entity part : body.getBodyParts()) {
			if ("event.ics".equals(part.getFilename())) {
				TextBody tb = (TextBody) part.getBody();
				InputStream in = tb.getInputStream();
				String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
				in.close();
				assertTrue(icsContent.contains("X-MICROSOFT-DISALLOW-COUNTER:true"));
				return;
			}
		}
		fail("Did not find any ics part in the message.");
	}

	@Test
	public void update() throws ServerFault, SQLException, UnsupportedEncodingException, IOException {
		ItemValue<VEventSeries> event = defaultVEvent("update");
		event.value.acceptCounters = true;
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "yeah yeah ok gg";
		event.value.main.attendees.add(attendee);

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		VEvent updated = event.value.main.copy();
		updated.summary = updated.summary + "-updated";
		VEventSeries update = new VEventSeries();
		update.main = updated;

		// organizer update meeting title
		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = update;
		veventMessage.oldEvent = event.value;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;

		assertTrue(m.getSubject().contains(updated.summary));
		assertTrue(m.getBody() instanceof Multipart);
		Multipart body = (Multipart) m.getBody();
		for (Entity part : body.getBodyParts()) {
			if ("event.ics".equals(part.getFilename())) {
				TextBody tb = (TextBody) part.getBody();
				InputStream in = tb.getInputStream();
				String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
				in.close();
				assertTrue(icsContent.contains("X-MICROSOFT-DISALLOW-COUNTER:false"));
				return;
			}
		}
		fail("Did not find any ics part in the message.");
	}

	@Test
	public void updateShouldIncludeAcceptPropositions()
			throws ServerFault, SQLException, UnsupportedEncodingException, IOException {
		ItemValue<VEventSeries> event = defaultVEvent("update");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "yeah yeah ok gg";
		event.value.main.attendees.add(attendee);

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		VEvent updated = event.value.main.copy();
		updated.summary = updated.summary + "-updated";
		VEventSeries update = new VEventSeries();
		update.main = updated;

		// organizer update meeting title
		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = update;
		veventMessage.oldEvent = event.value;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;

		assertTrue(m.getSubject().contains(updated.summary));
		assertTrue(m.getBody() instanceof Multipart);
		Multipart body = (Multipart) m.getBody();
		for (Entity part : body.getBodyParts()) {
			if ("event.ics".equals(part.getFilename())) {
				TextBody tb = (TextBody) part.getBody();
				InputStream in = tb.getInputStream();
				String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
				in.close();
				assertFalse(icsContent.contains("X-RESPONSE-COMMENT="));
				return;
			}
		}
		fail("Did not find any ics part in the message.");
	}

	@Test
	public void testExecutingIcsHookOnNonDefaultCalendarShouldNotSendEmails() throws ServerFault, SQLException {
		ItemValue<VEventSeries> event = defaultVEvent("update");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan"));

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		// organizer update meeting title
		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = event.value;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = Container.create("someContainer", null, null, null, domainUid, false);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertFalse(fakeSendmail.mailSent);

	}

	@Test
	public void cancel() throws ServerFault, SQLException, IOException {
		ItemValue<VEventSeries> event = defaultVEvent("cancel");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "yeah yeah ok gg";
		event.value.main.attendees.add(attendee);

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		// organizer cancel meeting
		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventDeleted(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));
		assertTrue(m.getBody() instanceof Multipart);
		Multipart body = (Multipart) m.getBody();
		for (Entity part : body.getBodyParts()) {
			if ("event.ics".equals(part.getFilename())) {
				TextBody tb = (TextBody) part.getBody();
				InputStream in = tb.getInputStream();
				String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
				in.close();
				assertFalse(icsContent.contains("X-RESPONSE-COMMENT="));
				return;
			}
		}
		fail("Did not find any ics part in the message.");
	}

	@Test
	public void accept() throws ServerFault, SQLException, UnsupportedEncodingException, IOException {
		ItemValue<VEventSeries> event = defaultVEvent("accept");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan"));

		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		VEvent accepted = (VEvent) event.value.main.copy();
		accepted.attendees = new ArrayList<>(1);
		Attendee u2 = event.value.main.attendees.iterator().next();
		Attendee u2Accepted = Attendee.create(u2.cutype, u2.member, u2.role, ParticipationStatus.Accepted, u2.rsvp,
				u2.delTo, u2.delFrom, u2.sentBy, u2.commonName, u2.dir, u2.lang, u2.uri, u2.mailto);
		u2Accepted.responseComment = "bang bang";
		accepted.attendees.add(u2Accepted);

		VEventSeries acceptedSeries = new VEventSeries();
		acceptedSeries.main = accepted;

		// user2 accept meeting
		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = acceptedSeries;
		veventMessage.oldEvent = event.value;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user2.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(accepted.summary));
		assertTrue(m.getBody() instanceof Multipart);
		Multipart body = (Multipart) m.getBody();
		for (Entity part : body.getBodyParts()) {
			if ("event.ics".equals(part.getFilename())) {
				TextBody tb = (TextBody) part.getBody();
				InputStream in = tb.getInputStream();
				String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
				in.close();
				assertTrue(icsContent.contains("X-RESPONSE-COMMENT=" + u2Accepted.responseComment));
				return;
			}
		}
		fail("Did not find any ics part in the message.");
	}

	@Test
	public void counter() throws ServerFault, SQLException, UnsupportedEncodingException, IOException {
		ItemValue<VEventSeries> event = defaultVEvent("counter");
		event.value.main.status = ICalendarElement.Status.Tentative;
		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan"));

		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		VEvent countered = (VEvent) event.value.main.copy();
		countered.attendees = new ArrayList<>(1);
		Attendee u2 = event.value.main.attendees.iterator().next();
		Attendee u2Accepted = Attendee.create(u2.cutype, u2.member, u2.role, ParticipationStatus.Accepted, u2.rsvp,
				u2.delTo, u2.delFrom, u2.sentBy, u2.commonName, u2.dir, u2.lang, u2.uri, u2.mailto);
		u2Accepted.responseComment = "no no, not at this time";
		countered.attendees.add(u2Accepted);

		VEventSeries acceptedSeries = new VEventSeries();
		acceptedSeries.main = countered;
		VEventCounter counterObj = new VEventCounter();
		counterObj.counter = VEventOccurrence.fromEvent(countered, null);
		acceptedSeries.counters = Arrays.asList(counterObj);

		// user2 counters meeting
		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = acceptedSeries;
		veventMessage.oldEvent = event.value;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user2.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(countered.summary));
		assertTrue(m.getBody() instanceof Multipart);
		Multipart body = (Multipart) m.getBody();
		for (Entity part : body.getBodyParts()) {
			if ("event.ics".equals(part.getFilename())) {
				TextBody tb = (TextBody) part.getBody();
				InputStream in = tb.getInputStream();
				String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
				in.close();
				assertTrue(icsContent.contains("COUNTER"));
				return;
			}
		}
		fail("Did not find any ics part in the message.");
	}

	@Test
	public void counterOnMain_DeclineCounter_Should_SendDecline() throws Exception {
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		ItemValue<VEventSeries> event = defaultVEvent("counterOnMain_DeclineCounter_Should_SendDecline");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "yeah yeah ok gg";
		event.value.main.attendees.add(attendee);

		VEventOccurrence counterEvent = VEventOccurrence.fromEvent(event.value.main.copy(), null);
		counterEvent.dtstart = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 3532);
		counterEvent.dtend = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 4533);
		VEventCounter counter = new VEventCounter();
		counter.originator = CounterOriginator.from(attendee.commonName, attendee.mailto);
		counter.counter = counterEvent;

		VEventSeries oldSeries = new VEventSeries();
		oldSeries.counters = Arrays.asList(counter);
		oldSeries.main = event.value.main;

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = oldSeries;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		Message m = fakeSendmail.messages.get(0).message;
		Multipart body = (Multipart) m.getBody();
		boolean checked = false;
		for (Entity part : body.getBodyParts()) {
			if ("event.ics".equals(part.getFilename())) {
				TextBody tb = (TextBody) part.getBody();
				try (InputStream in = tb.getInputStream()) {
					String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
					assertTrue(icsContent.contains("DECLINECOUNTER"));
					checked = true;
				}
				break;
			}
		}
		assertTrue(checked);

	}

	@Test
	public void counterOnNonExistingOccurrence_DeclineCounter_Should_SendDecline() throws Exception {
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		ItemValue<VEventSeries> event = defaultRecurrentVEvent(
				"counterOnNonExistingOccurrence_DeclineCounter_Should_SendDecline");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "yeah yeah ok gg";
		event.value.main.attendees.add(attendee);

		BmDateTime dtstart = event.value.main.dtstart;
		long exc = BmDateTimeWrapper.toTimestamp(dtstart.iso8601, dtstart.timezone);
		BmDateTime exceptionDate = BmDateTimeWrapper.fromTimestamp(exc + TimeUnit.DAYS.toMillis(2));
		VEventOccurrence counterEvent = VEventOccurrence.fromEvent(event.value.main.copy(), exceptionDate);
		counterEvent.dtstart = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 3532);
		counterEvent.dtend = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 4533);
		VEventCounter counter = new VEventCounter();
		counter.originator = CounterOriginator.from(attendee.commonName, attendee.mailto);
		counter.counter = counterEvent;

		VEventSeries oldSeries = new VEventSeries();
		oldSeries.counters = Arrays.asList(counter);
		oldSeries.main = event.value.main;

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = oldSeries;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		Message m = fakeSendmail.messages.get(0).message;
		Multipart body = (Multipart) m.getBody();
		boolean checked = false;
		for (Entity part : body.getBodyParts()) {
			if ("event.ics".equals(part.getFilename())) {
				TextBody tb = (TextBody) part.getBody();
				try (InputStream in = tb.getInputStream()) {
					String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
					assertTrue(icsContent.contains("DECLINE"));
					checked = true;
				}
				break;
			}
		}
		assertTrue(checked);

	}

	@Test
	public void counterOnExistingOccurrence_DeclineCounter_Should_SendDecline() throws Exception {
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		ItemValue<VEventSeries> event = defaultRecurrentVEventWithException(
				"counterOnExistingOccurrence_DeclineCounter_Should_SendDecline");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "yeah yeah ok gg";
		event.value.main.attendees.add(attendee);

		VEventOccurrence counterEvent = event.value.occurrences.get(0).copy();
		counterEvent.dtstart = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 3532);
		counterEvent.dtend = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 4533);
		VEventCounter counter = new VEventCounter();
		counter.originator = CounterOriginator.from(attendee.commonName, attendee.mailto);
		counter.counter = counterEvent;
		counter.counter.attendees.add(attendee);

		VEventSeries oldSeries = new VEventSeries();
		oldSeries.counters = Arrays.asList(counter);
		oldSeries.occurrences = event.value.occurrences;
		oldSeries.main = event.value.main;

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = oldSeries;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		Message m = fakeSendmail.messages.get(0).message;
		Multipart body = (Multipart) m.getBody();
		boolean checked = false;
		for (Entity part : body.getBodyParts()) {
			if ("event.ics".equals(part.getFilename())) {
				TextBody tb = (TextBody) part.getBody();
				try (InputStream in = tb.getInputStream()) {
					String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
					assertTrue(icsContent.contains("DECLINE"));
					checked = true;
				}
				break;
			}
		}
		assertTrue(checked);

	}

	@Test
	public void counterOnMain_UpdateMain_Should_NOT_SendDecline() throws Exception {
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		ItemValue<VEventSeries> event = defaultVEvent("counterOnMain_UpdateMain_Should_NOT_SendDecline");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "yeah yeah ok gg";
		event.value.main.attendees.add(attendee);

		VEventOccurrence counterEvent = VEventOccurrence.fromEvent(event.value.main.copy(), null);
		counterEvent.dtstart = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 3532);
		counterEvent.dtend = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 4533);
		VEventCounter counter = new VEventCounter();
		counter.originator = CounterOriginator.from(attendee.commonName, attendee.mailto);
		counter.counter = counterEvent;

		event.value.counters = Arrays.asList(counter);

		VEventSeries oldSeries = new VEventSeries();
		oldSeries.counters = Arrays.asList(counter);
		oldSeries.main = event.value.main.copy();

		// update main
		event.value.main.summary = "updated";

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = oldSeries;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		Message m = fakeSendmail.messages.get(0).message;
		Multipart body = (Multipart) m.getBody();
		boolean checked = false;
		for (Entity part : body.getBodyParts()) {
			if ("event.ics".equals(part.getFilename())) {
				TextBody tb = (TextBody) part.getBody();
				try (InputStream in = tb.getInputStream()) {
					String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
					assertTrue(icsContent.contains("REQUEST"));
					checked = true;
				}
				break;
			}
		}
		assertTrue(checked);
	}

	@Test
	public void counterOnMain_UpdateException_ShouldSendDecline() throws Exception {
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		ItemValue<VEventSeries> event = defaultRecurrentVEventWithException(
				"counterOnMain_UpdateException_ShouldSendDecline");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "yeah yeah ok gg";
		event.value.main.attendees.add(attendee);
		event.value.occurrences.get(0).attendees.add(attendee);

		VEventOccurrence counterEvent = VEventOccurrence.fromEvent(event.value.main.copy(), null);
		counterEvent.dtstart = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 3532);
		counterEvent.dtend = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 4533);
		VEventCounter counter = new VEventCounter();
		counter.originator = CounterOriginator.from(attendee.commonName, attendee.mailto);
		counter.counter = counterEvent;

		VEventSeries oldSeries = new VEventSeries();
		oldSeries.counters = Arrays.asList(counter);
		oldSeries.main = event.value.main.copy();
		oldSeries.occurrences = Arrays.asList(event.value.occurrences.get(0).copy());

		// update exception
		event.value.occurrences.get(0).summary = "updated";

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = oldSeries;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(2, fakeSendmail.messages.size());
		boolean checkedRequest = false;
		boolean checkedDecline = false;

		for (TestMail mail : fakeSendmail.messages) {
			Message m = mail.message;
			Multipart body = (Multipart) m.getBody();
			for (Entity part : body.getBodyParts()) {
				if ("event.ics".equals(part.getFilename())) {
					TextBody tb = (TextBody) part.getBody();
					try (InputStream in = tb.getInputStream()) {
						String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
						if (icsContent.contains("REQUEST")) {
							checkedRequest = true;
						} else if (icsContent.contains("DECLINECOUNTER")) {
							checkedDecline = true;
						}
					}
				}
			}
		}
		assertTrue(checkedRequest);
		assertTrue(checkedDecline);
	}

	@Test
	public void counterOnExistingException_UpdateMain_ShouldSendDecline() throws Exception {
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		ItemValue<VEventSeries> event = defaultRecurrentVEventWithException(
				"counterOnExistingException_UpdateMain_ShouldSendDecline");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "yeah yeah ok gg";
		event.value.main.attendees.add(attendee);
		event.value.occurrences.get(0).attendees.add(attendee);

		VEventOccurrence counterEvent = event.value.occurrences.get(0).copy();
		counterEvent.dtstart = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 3532);
		counterEvent.dtend = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 4533);
		VEventCounter counter = new VEventCounter();
		counter.originator = CounterOriginator.from(attendee.commonName, attendee.mailto);
		counter.counter = counterEvent;

		VEventSeries oldSeries = new VEventSeries();
		oldSeries.counters = Arrays.asList(counter);
		oldSeries.main = event.value.main.copy();
		oldSeries.occurrences = Arrays.asList(event.value.occurrences.get(0).copy());

		// update main
		event.value.main.summary = "updated";

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = oldSeries;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(2, fakeSendmail.messages.size());
		boolean checkedRequest = false;
		boolean checkedDecline = false;

		for (TestMail mail : fakeSendmail.messages) {
			Message m = mail.message;
			Multipart body = (Multipart) m.getBody();
			for (Entity part : body.getBodyParts()) {
				if ("event.ics".equals(part.getFilename())) {
					TextBody tb = (TextBody) part.getBody();
					try (InputStream in = tb.getInputStream()) {
						String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
						if (icsContent.contains("REQUEST")) {
							checkedRequest = true;
						} else if (icsContent.contains("DECLINECOUNTER")) {
							checkedDecline = true;
						}
					}
				}
			}
		}
		assertTrue(checkedRequest);
		assertTrue(checkedDecline);
	}

	@Test
	public void counterOnExistingException_UpdateThisException_Should_NOT_SendDecline() throws Exception {
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		ItemValue<VEventSeries> event = defaultRecurrentVEventWithException(
				"counterOnExistingException_UpdateThisException_Should_NOT_SendDecline");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "yeah yeah ok gg";
		event.value.main.attendees.add(attendee);
		event.value.occurrences.get(0).attendees.add(attendee);

		VEventOccurrence counterEvent = event.value.occurrences.get(0).copy();
		counterEvent.dtstart = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 3532);
		counterEvent.dtend = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 4533);
		VEventCounter counter = new VEventCounter();
		counter.originator = CounterOriginator.from(attendee.commonName, attendee.mailto);
		counter.counter = counterEvent;

		VEventSeries oldSeries = new VEventSeries();
		oldSeries.counters = Arrays.asList(counter);
		oldSeries.main = event.value.main.copy();
		oldSeries.occurrences = Arrays.asList(event.value.occurrences.get(0).copy());

		// update exception
		event.value.occurrences.get(0).summary = "updated";

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = oldSeries;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		boolean checkedRequest = false;

		for (TestMail mail : fakeSendmail.messages) {
			Message m = mail.message;
			Multipart body = (Multipart) m.getBody();
			for (Entity part : body.getBodyParts()) {
				if ("event.ics".equals(part.getFilename())) {
					TextBody tb = (TextBody) part.getBody();
					try (InputStream in = tb.getInputStream()) {
						String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
						if (icsContent.contains("REQUEST")) {
							checkedRequest = true;
						}
					}
				}
			}
		}
		assertTrue(checkedRequest);
	}

	@Test
	public void counterOnExistingException_UpdateOtherException_ShouldSendDecline() throws Exception {
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		ItemValue<VEventSeries> event = defaultRecurrentVEventWithException(
				"counterOnExistingException_UpdateOtherException_ShouldSendDecline");
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "yeah yeah ok gg";

		BmDateTime dtstart = event.value.main.dtstart;
		long exc = BmDateTimeWrapper.toTimestamp(dtstart.iso8601, dtstart.timezone);
		BmDateTime exceptionDate = BmDateTimeWrapper.fromTimestamp(exc + TimeUnit.DAYS.toMillis(4));
		VEventOccurrence exception = VEventOccurrence.fromEvent(event.value.main.copy(), exceptionDate);
		event.value.occurrences = new ArrayList<>(event.value.occurrences);
		exception.attendees.add(attendee);
		event.value.occurrences.add(exception);
		event.value.main.status = ICalendarElement.Status.NeedsAction;

		event.value.main.attendees.add(attendee);
		event.value.occurrences.get(0).attendees.add(attendee);

		VEventOccurrence counterEvent = event.value.occurrences.get(0).copy();
		counterEvent.dtstart = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 333);
		counterEvent.dtend = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 4533);
		VEventCounter counter = new VEventCounter();
		counter.originator = CounterOriginator.from(attendee.commonName, attendee.mailto);
		counter.counter = counterEvent;

		VEventSeries oldSeries = new VEventSeries();
		oldSeries.counters = Arrays.asList(counter);
		oldSeries.main = event.value.main.copy();
		oldSeries.occurrences = Arrays.asList(event.value.occurrences.get(0).copy(),
				event.value.occurrences.get(1).copy());

		// update other exception
		event.value.occurrences.get(1).summary = "updated";

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = oldSeries;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(2, fakeSendmail.messages.size());
		boolean checkedRequest = false;
		boolean checkedDecline = false;

		for (TestMail mail : fakeSendmail.messages) {
			Message m = mail.message;
			Multipart body = (Multipart) m.getBody();
			for (Entity part : body.getBodyParts()) {
				if ("event.ics".equals(part.getFilename())) {
					TextBody tb = (TextBody) part.getBody();
					try (InputStream in = tb.getInputStream()) {
						String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
						if (icsContent.contains("REQUEST")) {
							checkedRequest = true;
						} else if (icsContent.contains("DECLINECOUNTER")) {
							checkedDecline = true;
						}
					}
				}
			}
		}
		assertTrue(checkedRequest);
		assertTrue(checkedDecline);

	}

	@Test
	public void counterOnNonExistingException_UpdateMain_Should_SendDecline() throws Exception {
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		ItemValue<VEventSeries> event = defaultRecurrentVEventWithException(
				"counterOnNonExistingException_UpdateMain_Should_SendDecline");

		event.value.main.status = ICalendarElement.Status.NeedsAction;
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "yeah yeah ok gg";
		event.value.main.attendees.add(attendee);
		event.value.occurrences.get(0).attendees.add(attendee);

		BmDateTime dtstart = event.value.main.dtstart;
		long exc = BmDateTimeWrapper.toTimestamp(dtstart.iso8601, dtstart.timezone);
		BmDateTime exceptionDate = BmDateTimeWrapper.fromTimestamp(exc + TimeUnit.DAYS.toMillis(4));
		VEventOccurrence exception = VEventOccurrence.fromEvent(event.value.main.copy(), exceptionDate);
		VEventOccurrence counterEvent = exception;
		counterEvent.dtstart = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 3532);
		counterEvent.dtend = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 4533);
		VEventCounter counter = new VEventCounter();
		counter.originator = CounterOriginator.from(attendee.commonName, attendee.mailto);
		counter.counter = counterEvent;

		VEventSeries oldSeries = new VEventSeries();
		oldSeries.counters = Arrays.asList(counter);
		oldSeries.main = event.value.main.copy();
		oldSeries.occurrences = Arrays.asList(event.value.occurrences.get(0).copy());

		// update other exception
		event.value.main.summary = "updated";

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = oldSeries;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(2, fakeSendmail.messages.size());
		boolean checkedRequest = false;
		boolean checkedDecline = false;

		for (TestMail mail : fakeSendmail.messages) {
			Message m = mail.message;
			Multipart body = (Multipart) m.getBody();
			for (Entity part : body.getBodyParts()) {
				if ("event.ics".equals(part.getFilename())) {
					TextBody tb = (TextBody) part.getBody();
					try (InputStream in = tb.getInputStream()) {
						String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
						if (icsContent.contains("REQUEST")) {
							checkedRequest = true;
						} else if (icsContent.contains("DECLINECOUNTER")) {
							checkedDecline = true;
						}
					}
				}
			}
		}
		assertTrue(checkedRequest);
		assertTrue(checkedDecline);
	}

	@Test
	public void counterOnNonExistingException_AddingThisException_Should_NOT_SendDecline() throws Exception {
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		ItemValue<VEventSeries> event = defaultRecurrentVEventWithException(
				"counterOnNonExistingException_AddingThisException_Should_NOT_SendDecline");

		event.value.main.status = ICalendarElement.Status.NeedsAction;
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "yeah yeah ok gg";
		event.value.main.attendees.add(attendee);
		event.value.occurrences.get(0).attendees.add(attendee);

		BmDateTime dtstart = event.value.main.dtstart;
		long exc = BmDateTimeWrapper.toTimestamp(dtstart.iso8601, dtstart.timezone);
		BmDateTime exceptionDate = BmDateTimeWrapper.fromTimestamp(exc + TimeUnit.DAYS.toMillis(4));
		VEventOccurrence exception = VEventOccurrence.fromEvent(event.value.main.copy(), exceptionDate);
		VEventOccurrence counterEvent = exception;
		counterEvent.attendees.add(attendee);
		counterEvent.dtstart = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 3532);
		counterEvent.dtend = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 4533);
		VEventCounter counter = new VEventCounter();
		counter.originator = CounterOriginator.from(attendee.commonName, attendee.mailto);
		counter.counter = counterEvent;

		VEventSeries oldSeries = new VEventSeries();
		oldSeries.counters = Arrays.asList(counter);
		oldSeries.main = event.value.main.copy();
		oldSeries.occurrences = Arrays.asList(event.value.occurrences.get(0).copy());

		// adding exception
		event.value.occurrences = new ArrayList<>(event.value.occurrences);
		event.value.occurrences.add(counter.counter);

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = oldSeries;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(2, fakeSendmail.messages.size());
		int checkedRequest = 0;

		for (TestMail mail : fakeSendmail.messages) {
			Message m = mail.message;
			Multipart body = (Multipart) m.getBody();
			for (Entity part : body.getBodyParts()) {
				if ("event.ics".equals(part.getFilename())) {
					TextBody tb = (TextBody) part.getBody();
					try (InputStream in = tb.getInputStream()) {
						String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
						if (icsContent.contains("REQUEST")) {
							checkedRequest++;
						}
					}
				}
			}
		}
		assertEquals(2, checkedRequest);
	}

	@Test
	public void counterOnNonExistingException_UpdateOtherException_ShouldSendDecline() throws Exception {
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		ItemValue<VEventSeries> event = defaultRecurrentVEventWithException(
				"counterOnNonExistingException_UpdateOtherException_ShouldSendDecline");

		event.value.main.status = ICalendarElement.Status.NeedsAction;
		Attendee attendee = VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan");
		attendee.responseComment = "yeah yeah ok gg";
		event.value.main.attendees.add(attendee);
		event.value.occurrences.get(0).attendees.add(attendee);

		BmDateTime dtstart = event.value.main.dtstart;
		long exc = BmDateTimeWrapper.toTimestamp(dtstart.iso8601, dtstart.timezone);
		BmDateTime exceptionDate = BmDateTimeWrapper.fromTimestamp(exc + TimeUnit.DAYS.toMillis(4));
		VEventOccurrence exception = VEventOccurrence.fromEvent(event.value.main.copy(), exceptionDate);
		VEventOccurrence counterEvent = exception;
		counterEvent.attendees.add(attendee);
		counterEvent.dtstart = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 3532);
		counterEvent.dtend = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis() + 4533);
		VEventCounter counter = new VEventCounter();
		counter.originator = CounterOriginator.from(attendee.commonName, attendee.mailto);
		counter.counter = counterEvent;

		VEventSeries oldSeries = new VEventSeries();
		oldSeries.counters = Arrays.asList(counter);
		oldSeries.main = event.value.main.copy();
		oldSeries.occurrences = Arrays.asList(event.value.occurrences.get(0).copy());

		// update exception
		event.value.occurrences.get(0).summary = "update";

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = oldSeries;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertEquals(2, fakeSendmail.messages.size());
		boolean checkedRequest = false;
		boolean checkedDecline = false;

		for (TestMail mail : fakeSendmail.messages) {
			Message m = mail.message;
			Multipart body = (Multipart) m.getBody();
			for (Entity part : body.getBodyParts()) {
				if ("event.ics".equals(part.getFilename())) {
					TextBody tb = (TextBody) part.getBody();
					try (InputStream in = tb.getInputStream()) {
						String icsContent = new String(ByteStreams.toByteArray(in), part.getCharset());
						if (icsContent.contains("REQUEST")) {
							checkedRequest = true;
						} else if (icsContent.contains("DECLINECOUNTER")) {
							checkedDecline = true;
						}
					}
				}
			}
		}
		assertTrue(checkedRequest);
		assertTrue(checkedDecline);

	}

	@Test
	public void createEventRecipientsList() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent("createEventRecipientsList");
		event.value.main.attendees = new ArrayList<>();

		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u0", "", "", "", "", "", "u0", "req-u0@test.lan"));
		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "req-u2@test.lan"));

		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.OptionalParticipant,
				ParticipationStatus.NeedsAction, false, "u3", "", "", "", "", "", "u3", "opt-u3@test.lan"));
		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.OptionalParticipant,
				ParticipationStatus.NeedsAction, false, "u4", "", "", "", "", "", "u4", "opt-u4@test.lan"));

		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventCreated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(4, fakeSendmail.messages.size());
		ArrayList<String> recipients = new ArrayList<String>(4);

		// 4 emails because 4 attendees
		// 1 recipient per email
		for (TestMail tm : fakeSendmail.messages) {
			assertEquals(1, tm.to.size());
			recipients.add(tm.to.iterator().next());
			Message m = tm.message;

			// required participants
			assertEquals(2, m.getTo().size());
			for (Address s : m.getTo()) {
				assertTrue(s.toString().startsWith("req-"));
			}

			// optional participants
			assertEquals(2, m.getCc().size());
			for (Address s : m.getCc()) {
				assertTrue(s.toString().startsWith("opt-"));
			}
		}

		assertTrue(recipients.contains("req-u0@test.lan"));
		assertTrue(recipients.contains("req-u2@test.lan"));
		assertTrue(recipients.contains("opt-u3@test.lan"));
		assertTrue(recipients.contains("opt-u4@test.lan"));

	}

	@Test
	public void deleteEventRecipientsList() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent("deleteEventRecipientsList");
		event.value.main.attendees = new ArrayList<>();

		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u0", "", "", "", "", "", "u0", "req-u0@test.lan"));
		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "req-u2@test.lan"));

		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.OptionalParticipant,
				ParticipationStatus.NeedsAction, false, "u3", "", "", "", "", "", "u3", "opt-u3@test.lan"));
		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.OptionalParticipant,
				ParticipationStatus.NeedsAction, false, "u4", "", "", "", "", "", "u4", "opt-u4@test.lan"));

		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventDeleted(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(4, fakeSendmail.messages.size());
		ArrayList<String> recipients = new ArrayList<String>(4);

		// 4 emails because 4 attendees
		// 1 recipient per email
		for (TestMail tm : fakeSendmail.messages) {
			assertEquals(1, tm.to.size());
			recipients.add(tm.to.iterator().next());
			Message m = tm.message;

			// required participants
			assertEquals(2, m.getTo().size());
			for (Address s : m.getTo()) {
				assertTrue(s.toString().startsWith("req-"));
			}

			// optional participants
			assertEquals(2, m.getCc().size());
			for (Address s : m.getCc()) {
				assertTrue(s.toString().startsWith("opt-"));
			}
		}

		assertTrue(recipients.contains("req-u0@test.lan"));
		assertTrue(recipients.contains("req-u2@test.lan"));
		assertTrue(recipients.contains("opt-u3@test.lan"));
		assertTrue(recipients.contains("opt-u4@test.lan"));

	}

	@Test
	public void createEventExceptionRecipientsList() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent("createEventExceptionRecipientsList");

		event.value.main.rrule = new RRule();
		event.value.main.rrule.frequency = Frequency.DAILY;
		event.value.main.rrule.interval = 1;

		event.value.main.attendees = new ArrayList<>();
		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u0", "", "", "", "", "", "u0", "req-u0@test.lan"));
		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "req-u2@test.lan"));

		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.OptionalParticipant,
				ParticipationStatus.NeedsAction, false, "u3", "", "", "", "", "", "u3", "opt-u3@test.lan"));
		event.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.OptionalParticipant,
				ParticipationStatus.NeedsAction, false, "u4", "", "", "", "", "", "u4", "opt-u4@test.lan"));

		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.oldEvent = event.value;

		VEvent updated = event.value.main.copy();
		Set<net.bluemind.core.api.date.BmDateTime> exdate = new HashSet<>(1);
		exdate.add(BmDateTimeWrapper.create(ZonedDateTime.now(), Precision.Date));
		updated.exdate = exdate;
		VEventSeries updatedSeries = new VEventSeries();
		updatedSeries.main = updated;
		veventMessage.vevent = updatedSeries;

		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(4, fakeSendmail.messages.size());
		ArrayList<String> recipients = new ArrayList<String>(4);

		// 4 emails because 4 attendees
		// 1 recipient per email
		for (TestMail tm : fakeSendmail.messages) {
			assertEquals(1, tm.to.size());
			recipients.add(tm.to.iterator().next());
			Message m = tm.message;

			// required participants
			assertEquals(2, m.getTo().size());
			for (Address s : m.getTo()) {
				assertTrue(s.toString().startsWith("req-"));
			}

			// optional participants
			assertEquals(2, m.getCc().size());
			for (Address s : m.getCc()) {
				assertTrue(s.toString().startsWith("opt-"));
			}
		}

		assertTrue(recipients.contains("req-u0@test.lan"));
		assertTrue(recipients.contains("req-u2@test.lan"));
		assertTrue(recipients.contains("opt-u3@test.lan"));
		assertTrue(recipients.contains("opt-u4@test.lan"));

	}

	@Test
	public void o2a__MasterAttendee() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		FakeSendmail fakeSendmail = icsHookOnCreate(securityContext, user1.login, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REQUEST", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertTrue(series.value.occurrences.isEmpty());
	}

	@Test
	public void o2a__MasterNotAttendee$OccurrenceAttendee() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		event.value.occurrences = Arrays.asList(createSimpleOccur(event.value.main));

		// remove attendees from master
		event.value.main.attendees = Collections.emptyList();

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		FakeSendmail fakeSendmail = icsHookOnCreate(securityContext, user1.login, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REQUEST", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNull(series.value.main);
		assertEquals(1, series.value.occurrences.size());
	}

	@Test
	public void o2a__MasterAttendee$OccurrenceAttendee() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		event.value.occurrences = Arrays.asList(createSimpleOccur(event.value.main));

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		FakeSendmail fakeSendmail = icsHookOnCreate(securityContext, user1.login, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REQUEST", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertEquals(1, series.value.occurrences.size());
	}

	@Test
	public void o2a__MasterAttendee$OccurrenceNotAttendee() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		event.value.occurrences = Arrays.asList(createSimpleOccur(event.value.main));
		event.value.occurrences.get(0).attendees = Collections.emptyList();

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);
		FakeSendmail fakeSendmail = icsHookOnCreate(securityContext, user1.login, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REQUEST", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertEquals(0, series.value.occurrences.size());
	}

	@Test
	public void o2a_MasterAttendee_MasterNotAttendee() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");

		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		newEvent.value.main.attendees = Collections.emptyList();

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, event, newEvent);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("CANCEL", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertTrue(series.value.occurrences.isEmpty());
	}

	@Test
	public void o2a_MasterAttendee_MasterNAttendeeWithExDate() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		newEvent.value.main.exdate = ImmutableSet.of(BmDateTimeWrapper.create(
				new BmDateTimeWrapper(newEvent.value.main.dtstart).toDateTime().plusDays(1), Precision.DateTime));
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, event, newEvent);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("CANCEL", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertEquals(1, series.value.occurrences.size());
		assertNull(series.value.occurrences.get(0).exdate);
	}

	@Test
	public void o2a_MasterNotAttendee_MasterAttendee() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");

		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		oldEvent.value.main.attendees = Collections.emptyList();
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);
		eventSanitizer.sanitize(oldEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, oldEvent, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REQUEST", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertTrue(series.value.occurrences.isEmpty());
	}

	@Test
	public void o2a_tooMuchInICS() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");

		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		VEventOccurrence occur = createSimpleOccur(newEvent.value.main);
		newEvent.value.occurrences = Arrays.asList(occur);

		// make some "weird" changes in newEvent
		newEvent.value.main.rrule.byDay = Collections.emptyList();
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, event, newEvent);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REQUEST", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		// should be null because with didnt not really changed main event
		assertNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertEquals(1, series.value.occurrences.size());
	}

	@Test
	public void o2a_tooMuchInICS_OnWeeklyUpdate() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		event.value.main.rrule.frequency = ICalendarElement.RRule.Frequency.WEEKLY;
		event.value.main.rrule.byDay = Arrays.asList(ICalendarElement.RRule.WeekDay.MO);

		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		VEventOccurrence occur = createSimpleOccur(newEvent.value.main);
		occur.rrule = null;
		occur.summary = "updated summary";
		newEvent.value.occurrences = Arrays.asList(occur);
		newEvent.value.main.rrule.frequency = ICalendarElement.RRule.Frequency.WEEKLY;
		newEvent.value.main.rrule.byDay = Arrays.asList(new ICalendarElement.RRule.WeekDay("MO"));

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, event, newEvent);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(occur.summary));

		String mailContent = extractMailBody(m);
		assertTrue(mailContent.contains(occur.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REQUEST", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		// should be null because with didnt not really changed main event
		assertNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertEquals(1, series.value.occurrences.size());
	}

	@Test
	public void o2a_MasterAttendee_MasterAttendee$OcurrenceNotAttendee() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");

		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		VEventOccurrence occur = createSimpleOccur(newEvent.value.main);
		occur.attendees = Collections.emptyList();
		newEvent.value.occurrences = Arrays.asList(occur);

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, event, newEvent);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("CANCEL", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertEquals(1, series.value.occurrences.size());
	}

	@Test
	public void o2a_MasterAttendee$OccurenceAttendee_MasterAttendee$OcurrenceNotAttendee() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		VEventOccurrence occur = createSimpleOccur(event.value.main);
		occur.attendees = Collections.emptyList();
		event.value.occurrences = Arrays.asList(occur);

		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		oldEvent.value.occurrences = Arrays.asList(createSimpleOccur(event.value.main));
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);

		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);
		eventSanitizer.sanitize(oldEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, oldEvent, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("CANCEL", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertEquals(1, series.value.occurrences.size());
	}

	@Test
	public void o2a_MasterNotAttendee$OccurenceAttendee_MasterNotAttendee$OcurrenceNotAttendee() throws Exception {
		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		oldEvent.value.occurrences = Arrays.asList(createSimpleOccur(oldEvent.value.main));
		oldEvent.value.main.attendees = Collections.emptyList();

		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		event.value.main.attendees = Collections.emptyList();
		event.value.occurrences = Arrays.asList(createSimpleOccur(event.value.main));

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);
		eventSanitizer.sanitize(oldEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, oldEvent, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("CANCEL", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertEquals(1, series.value.occurrences.size());
	}

	@Test
	public void o2a_MasterNotAttendee$OccurenceAttendee_MasterAttendee$OcurrenceAttendee() throws Exception {
		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		oldEvent.value.occurrences = Arrays.asList(createSimpleOccur(oldEvent.value.main));
		oldEvent.value.main.attendees = Collections.emptyList();

		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		event.value.occurrences = Arrays.asList(createSimpleOccur(event.value.main));

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);
		eventSanitizer.sanitize(oldEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, oldEvent, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);

		String method = getIcsPartMethod(m);
		assertEquals("REQUEST", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertEquals(1, series.value.occurrences.size());
	}

	@Test
	public void o2a_MasterNotAttendee$OccurenceAttendee_MasterNotAttendee() throws Exception {
		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		oldEvent.value.occurrences = Arrays.asList(createSimpleOccur(oldEvent.value.main));
		oldEvent.value.main.attendees = Collections.emptyList();

		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		event.value.main.attendees = Collections.emptyList();

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);
		eventSanitizer.sanitize(oldEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, oldEvent, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String method = getIcsPartMethod(m);
		assertEquals("CANCEL", method);
	}

	@Test
	public void o2a_MasterAttendee$OccurenceNotAttendee_MasterNotAttendee$OcurrenceNotAttendee() throws Exception {
		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		oldEvent.value.occurrences = Arrays.asList(createSimpleOccur(oldEvent.value.main));
		oldEvent.value.occurrences.get(0).attendees = Collections.emptyList();

		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		event.value.main.attendees = Collections.emptyList();
		event.value.occurrences = Arrays.asList(createSimpleOccur(event.value.main));

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(oldEvent.value, true);
		eventSanitizer.sanitize(event.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, oldEvent, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);

		assertEquals("CANCEL", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertTrue(series.value.occurrences.isEmpty());
	}

	@Test
	public void o2a_MasterAttendee$OccurenceNotAttendee_MasterAttendee$OcurrenceAttendee() throws Exception {
		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		oldEvent.value.occurrences = Arrays.asList(createSimpleOccur(oldEvent.value.main));
		oldEvent.value.occurrences.get(0).attendees = Collections.emptyList();

		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		event.value.occurrences = Arrays.asList(createSimpleOccur(event.value.main));

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);
		eventSanitizer.sanitize(oldEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, oldEvent, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REQUEST", method); // or ADD ?
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertEquals(1, series.value.occurrences.size());
	}

	@Test
	public void o2a_MasterAttendee_() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		FakeSendmail fakeSendmail = icsHookOnDelete(securityContext, user1.login, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("CANCEL", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertTrue(series.value.occurrences.isEmpty());
	}

	@Test
	public void o2a_MasterNotAttendee_() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		event.value.main.attendees = Collections.emptyList();

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		FakeSendmail fakeSendmail = icsHookOnDelete(securityContext, user1.login, event);
		assertFalse(fakeSendmail.mailSent);
	}

	@Test
	public void o2a_MasterNotAttendee$OccurrenceAttendee_() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		event.value.occurrences = Arrays.asList(createSimpleOccur(event.value.main));
		event.value.main.attendees = Collections.emptyList();

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		FakeSendmail fakeSendmail = icsHookOnDelete(securityContext, user1.login, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("CANCEL", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertEquals(1, series.value.occurrences.size());
	}

	@Test
	public void o2a_MasterAttendee$OccurrenceAttendee_() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		event.value.occurrences = Arrays.asList(createSimpleOccur(event.value.main));

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		FakeSendmail fakeSendmail = icsHookOnDelete(securityContext, user1.login, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("CANCEL", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertTrue(series.value.occurrences.isEmpty());
	}

	@Test
	public void o2a_MasterAttendee$OccurrenceNotAttendee_() throws Exception {
		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		event.value.occurrences = Arrays.asList(createSimpleOccur(event.value.main));
		event.value.occurrences.get(0).attendees = Collections.emptyList();
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);

		FakeSendmail fakeSendmail = icsHookOnDelete(securityContext, user1.login, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(event.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("CANCEL", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertTrue(series.value.occurrences.isEmpty());
	}

	@Test
	public void a2o_MasterAttendee$OccurrenceAttendee_accept_MasterAttendee() throws Exception {
		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		oldEvent.value.occurrences = Arrays.asList(createSimpleOccur(oldEvent.value.main));

		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		newEvent.value.occurrences = Arrays.asList(createSimpleOccur(newEvent.value.main));

		newEvent.value.main.attendees.get(0).partStatus = ParticipationStatus.Accepted;
		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(oldEvent.value, true);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user2.login, oldEvent, newEvent);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(oldEvent.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REPLY", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertEquals(1, series.value.main.attendees.size());
		assertEquals(VEvent.ParticipationStatus.Accepted, series.value.main.attendees.get(0).partStatus);
		assertNotNull(series.value.occurrences);
		assertTrue(series.value.occurrences.isEmpty());
	}

	@Test
	public void a2o_MasterAttendee_accept_MasterAttendee_ShouldOnlyIncludeSenderAsAttendee() throws Exception {
		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		oldEvent.value.main.attendees = new ArrayList<>(oldEvent.value.main.attendees);
		oldEvent.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u3", "", "", "", "", "", "u3", "u3@test.lan"));
		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		newEvent.value.main.attendees = new ArrayList<>(newEvent.value.main.attendees);
		newEvent.value.main.attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u3", "", "", "", "", "", "u3", "u3@test.lan"));

		newEvent.value.main.attendees.get(0).partStatus = ParticipationStatus.Accepted;
		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(oldEvent.value, true);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user2.login, oldEvent, newEvent);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(oldEvent.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REPLY", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertEquals(1, series.value.main.attendees.size());
		assertEquals(VEvent.ParticipationStatus.Accepted, series.value.main.attendees.get(0).partStatus);
		assertNotNull(series.value.occurrences);
		assertTrue(series.value.occurrences.isEmpty());
	}

	public void a2o_MasterAttendee$OccurrenceAttendee_decline_MasterAttendee() throws Exception {
		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		oldEvent.value.occurrences = Arrays.asList(createSimpleOccur(oldEvent.value.main));

		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		newEvent.value.occurrences = Arrays.asList(createSimpleOccur(newEvent.value.main));

		newEvent.value.main.attendees.get(0).partStatus = ParticipationStatus.Declined;
		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(oldEvent.value, true);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user2.login, oldEvent, newEvent);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(oldEvent.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REPLY", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertEquals(1, series.value.main.attendees.size());
		assertEquals(VEvent.ParticipationStatus.Declined, series.value.main.attendees.get(0).partStatus);
		assertNotNull(series.value.occurrences);
		assertTrue(series.value.occurrences.isEmpty());
	}

	@Test
	public void a2o_MasterAttendee$OccurrenceAttendee_accept_OccurrenceAttendee() throws Exception {
		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		oldEvent.value.occurrences = Arrays.asList(createSimpleOccur(oldEvent.value.main));

		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		newEvent.value.occurrences = Arrays.asList(createSimpleOccur(newEvent.value.main));

		newEvent.value.occurrences.get(0).attendees.get(0).partStatus = ParticipationStatus.Accepted;
		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(oldEvent.value, true);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user2.login, oldEvent, newEvent);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(oldEvent.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REPLY", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNull(series.value.main);
		assertEquals(1, series.value.occurrences.size());
		assertEquals(VEvent.ParticipationStatus.Accepted, series.value.occurrences.get(0).attendees.get(0).partStatus);
	}

	@Test
	public void a2o_MasterAttendee_accept_OccurrenceAttendee() throws Exception {
		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");

		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		newEvent.value.occurrences = Arrays.asList(createSimpleOccur(newEvent.value.main));

		newEvent.value.occurrences.get(0).attendees.get(0).partStatus = ParticipationStatus.Accepted;
		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(oldEvent.value, true);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user2.login, oldEvent, newEvent);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(oldEvent.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REPLY", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNull(series.value.main);
		assertEquals(1, series.value.occurrences.size());
		assertEquals(VEvent.ParticipationStatus.Accepted, series.value.occurrences.get(0).attendees.get(0).partStatus);
	}

	@Test
	public void a2o_MasterAttendee$OccurrenceAttendee_accept_Both() throws Exception {
		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		oldEvent.value.occurrences = Arrays.asList(createSimpleOccur(oldEvent.value.main));

		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		newEvent.value.occurrences = Arrays.asList(createSimpleOccur(newEvent.value.main));

		newEvent.value.occurrences.get(0).attendees.get(0).partStatus = ParticipationStatus.Accepted;
		newEvent.value.main.attendees.get(0).partStatus = ParticipationStatus.Accepted;
		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(oldEvent.value, true);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user2.login, oldEvent, newEvent);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(2, fakeSendmail.messages.size());

		TestMail acceptMaster = fakeSendmail.messages.get(0);
		assertEquals(1, acceptMaster.to.size());
		Message m = acceptMaster.message;
		assertTrue(m.getSubject().contains(oldEvent.value.main.summary));

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REPLY", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertEquals(1, series.value.main.attendees.size());
		assertEquals(VEvent.ParticipationStatus.Accepted, series.value.main.attendees.get(0).partStatus);
		assertNotNull(series.value.occurrences);
		assertTrue(series.value.occurrences.isEmpty());

		TestMail acceptOccurrence = fakeSendmail.messages.get(1);
		assertEquals(1, acceptOccurrence.to.size());
		m = acceptOccurrence.message;
		assertTrue(m.getSubject().contains(oldEvent.value.main.summary));

		ics = getIcsPartAsText(m);
		method = getIcsPartMethod(m);
		assertEquals("REPLY", method);
		seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		series = seriesList.get(0);
		assertNull(series.value.main);
		assertEquals(1, series.value.occurrences.size());
		assertEquals(VEvent.ParticipationStatus.Accepted, series.value.occurrences.get(0).attendees.get(0).partStatus);

	}

	@Test
	public void a2o_MasterAttendee$OccurrenceAttendee_delete_DeclinedOccurrence() throws Exception {
		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		newEvent.value.occurrences = Arrays.asList(createSimpleOccur(newEvent.value.main));
		newEvent.value.occurrences.get(0).attendees.get(0).partStatus = ParticipationStatus.Declined;
		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnDelete(securityContext, user2.login, newEvent);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REPLY", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		// master declined
		assertNotNull(series.value.main);

		// everything else was already declined
		// but we resend it (no problem)
		assertEquals(1, series.value.occurrences.size());
		assertEquals(VEvent.ParticipationStatus.Declined, series.value.occurrences.get(0).attendees.get(0).partStatus);
	}

	@Test
	public void a2o_MasterAttendee$OccurrenceAttendee_delete_() throws Exception {
		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		newEvent.value.occurrences = Arrays.asList(createSimpleOccur(newEvent.value.main));
		newEvent.value.occurrences.get(0).attendees.get(0).partStatus = ParticipationStatus.Declined;
		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnDelete(securityContext, user2.login, newEvent);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());

		TestMail acceptMaster = fakeSendmail.messages.get(0);
		assertEquals(1, acceptMaster.to.size());
		Message m = acceptMaster.message;

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REPLY", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.main);
		assertEquals(1, series.value.main.attendees.size());
		assertEquals(VEvent.ParticipationStatus.Declined, series.value.main.attendees.get(0).partStatus);
		assertEquals(1, series.value.occurrences.size());
		assertEquals(VEvent.ParticipationStatus.Declined, series.value.occurrences.get(0).attendees.get(0).partStatus);

	}

	@Test
	public void a2o_MasterAttendee$OccurrenceAttendee_delete_DeclinedBoth() throws Exception {
		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		newEvent.value.occurrences = Arrays.asList(createSimpleOccur(newEvent.value.main));
		newEvent.value.occurrences.get(0).attendees.get(0).partStatus = ParticipationStatus.Declined;
		newEvent.value.main.attendees.get(0).partStatus = ParticipationStatus.Declined;
		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnDelete(securityContext, user2.login, newEvent);
		assertFalse(fakeSendmail.mailSent);
	}

	@Test
	public void a2o_MasterAttendee$OccurrenceAttendee_delete_DeclinedMaster() throws Exception {
		ItemValue<VEventSeries> newEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		newEvent.value.occurrences = Arrays.asList(createSimpleOccur(newEvent.value.main));
		newEvent.value.main.attendees.get(0).partStatus = ParticipationStatus.Declined;
		assertEquals(ParticipationStatus.NeedsAction, newEvent.value.occurrences.get(0).attendees.get(0).partStatus);
		SecurityContext securityContext = Sessions.get().getIfPresent(user2.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnDelete(securityContext, user2.login, newEvent);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;

		String ics = getIcsPartAsText(m);
		String method = getIcsPartMethod(m);
		assertEquals("REPLY", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNull(series.value.main);
		assertEquals(VEvent.ParticipationStatus.Declined, series.value.occurrences.get(0).attendees.get(0).partStatus);
	}

	@Test
	public void a2o_MasterAttendee_accept_MasterAttendee_ShouldNotSendParticipationChangesToOrganizer()
			throws Exception {

		String resUid = UUID.randomUUID().toString();
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IVideoConferencing.class, domainUid)
				.createResource(resUid, VideoConferencingResourceDescriptor.create("coucou", "test-provider",
						Arrays.asList(AccessControlEntry.create(domainUid, Verb.Invitation))));

		ResourceDescriptor res = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IResources.class, domainUid).get(resUid);

		ItemValue<VEventSeries> oldEvent = defaultVEvent(
				"a2o_MasterAttendee_accept_MasterAttendee_ShouldNotSendParticipationChangesToOrganizer");
		oldEvent.value.main.attendees = Arrays.asList(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, resUid, "", "", "", "", "", resUid, res.defaultEmailAddress()));

		ItemValue<VEventSeries> newEvent = defaultVEvent(
				"a2o_MasterAttendee_accept_MasterAttendee_ShouldNotSendParticipationChangesToOrganizer");
		newEvent.value.main.attendees = Arrays.asList(
				VEvent.Attendee.create(CUType.Resource, "", Role.RequiredParticipant, ParticipationStatus.Accepted,
						false, resUid, "", "", "", "", "", resUid, res.defaultEmailAddress()));

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);

		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(oldEvent.value, true);
		eventSanitizer.sanitize(newEvent.value, true);

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = newEvent.uid;
		veventMessage.vevent = newEvent.value;
		veventMessage.oldEvent = oldEvent.value;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = Container.create(ICalendarUids.resourceCalendar(resUid), "calendar", "cal", resUid,
				domainUid, true);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);

		assertFalse(fakeSendmail.mailSent);
		assertTrue(fakeSendmail.messages.isEmpty());
	}

	@Test
	public void o2a_MasterAttendee_update_add_occurrence_deleteRecpient_addRecipient() throws Exception {
		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "invite1",
				"invite1@test.lan");

		ItemValue<VEventSeries> newEvent = ItemValue.create(oldEvent.uid, oldEvent.value.copy());
		newEvent.value.occurrences = Arrays.asList(createSimpleOccur(
				defaultVEventWithAttendeeAndSimpleRecur("occ1", "invite2", "invite2@test.lan").value.main));

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(oldEvent.value, true);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, oldEvent, newEvent);

		assertTrue(fakeSendmail.mailSent);
		assertEquals(2, fakeSendmail.messages.size());

		TestMail tm1 = fakeSendmail.messages.get(0);
		assertEquals(1, tm1.to.size());
		assertEquals("invite2@test.lan", tm1.to.iterator().next());
		Message m1 = tm1.message;
		String method1 = getIcsPartMethod(m1);
		assertEquals("REQUEST", method1);

		TestMail tm2 = fakeSendmail.messages.get(1);
		assertEquals(1, tm2.to.size());
		assertEquals("invite1@test.lan", tm2.to.iterator().next());
		Message m2 = tm2.message;
		String method2 = getIcsPartMethod(m2);
		assertEquals("CANCEL", method2);
	}

	@Test
	public void o2a_MasterNotAttendee_MasterNotAttendee$OccurrenceAttendee() throws Exception {
		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		oldEvent.value.main.organizer = null;
		oldEvent.value.main.attendees = new ArrayList<>();
		VEventOccurrence occ = createSimpleOccur(oldEvent.value.main);
		occ.organizer = new Organizer("u1@test.lan");
		occ.attendees = Arrays.asList(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan"));
		oldEvent.value.occurrences = Arrays.asList(occ);

		ItemValue<VEventSeries> event = defaultVEventWithAttendeeAndSimpleRecur("invite", "u2", "u2@test.lan");
		event.value.main.organizer = null;
		event.value.main.attendees = new ArrayList<>();
		occ = createSimpleOccur(event.value.main);
		occ.summary = "UPDATED";
		occ.organizer = new Organizer("u1@test.lan");
		occ.attendees = Arrays.asList(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, "u2", "", "", "", "", "", "u2", "u2@test.lan"));
		event.value.occurrences = Arrays.asList(occ);

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);
		eventSanitizer.sanitize(oldEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, oldEvent, event);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		assertEquals(1, tm.to.size());
		Message m = tm.message;
		assertTrue(m.getSubject().contains(occ.summary));

		String ics = getIcsPartAsText(m);

		String method = getIcsPartMethod(m);
		assertEquals("REQUEST", method);
		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNull(series.value.main);
		assertNotNull(series.value.occurrences);
		assertEquals(1, series.value.occurrences.size());
	}

	@Test
	public void o2a_MasterAttendee$OccurrenceAttendee_MasterAttendee$OccurrenceAttendee_cancel_OccurrenceAttendee()
			throws Exception {
		ItemValue<VEventSeries> oldEvent = defaultVEventWithAttendee("invite", "u2", "u2@test.lan");

		VEvent event = oldEvent.value.main;
		event.dtstart = new BmDateTime("2019-12-02T12:00:00.000+01:00", "Europe/Paris", Precision.DateTime);
		event.dtend = new BmDateTime("2019-12-02T14:00:00.000+01:00", "Europe/Paris", Precision.DateTime);
		event.rrule = new RRule();
		event.rrule.frequency = Frequency.WEEKLY;
		event.rrule.byDay = Arrays.asList(WeekDay.WE);
		event.rrule.interval = 1;

		BmDateTime recurId = new BmDateTime(event.dtstart.iso8601, event.dtstart.timezone, event.dtstart.precision);
		VEventOccurrence occurr = VEventOccurrence.fromEvent(event, recurId);
		occurr.dtstart = BmDateTimeWrapper.create(new BmDateTimeWrapper(event.dtstart).toDateTime().minusDays(1),
				Precision.DateTime);
		occurr.dtend = BmDateTimeWrapper.create(new BmDateTimeWrapper(event.dtend).toDateTime().minusDays(1),
				Precision.DateTime);

		oldEvent.value.occurrences = Arrays.asList(occurr);

		ItemValue<VEventSeries> newEvent = ItemValue.create(oldEvent.uid, oldEvent.value.copy());
		newEvent.value.occurrences = null;
		newEvent.value.main.exdate = new LinkedHashSet<BmDateTime>(Arrays.asList(newEvent.value.main.dtstart));

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(oldEvent.value, true);
		eventSanitizer.sanitize(newEvent.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, oldEvent, newEvent);

		assertTrue(fakeSendmail.mailSent);
		assertEquals(1, fakeSendmail.messages.size());
		TestMail tm = fakeSendmail.messages.get(0);
		Message m = tm.message;

		String ics = getIcsPartAsText(m);
		System.err.println(ics);

		String method = getIcsPartMethod(m);
		assertEquals("CANCEL", method);

		List<ItemValue<VEventSeries>> seriesList = VEventServiceHelper.convertToVEventList(ics, Optional.empty());

		assertEquals(1, seriesList.size());

		ItemValue<VEventSeries> series = seriesList.get(0);
		assertNotNull(series.value.occurrences);
		dateEquals(occurr.dtstart, series.value.occurrences.get(0).dtstart);
	}

	private void dateEquals(BmDateTime a, BmDateTime b) {
		assertEquals(new BmDateTimeWrapper(a).toUTCTimestamp(), new BmDateTimeWrapper(b).toUTCTimestamp());
	}

	@Test
	public void o2a_sequenceOverwriteEventChangeCalculation() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent("invite");
		event.value.main.attendees = addAttendee("u2", "u2@test.lan", event.value.main.attendees);

		ItemValue<VEventSeries> old = ItemValue.create(event.uid, event.value.copy());
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(event.value, true);
		eventSanitizer.sanitize(old.value, true);
		event.value.main.sequence = 2;

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, old, event);

		assertTrue(fakeSendmail.mailSent);
		assertEquals(1, fakeSendmail.messages.size());
		TestMail message = fakeSendmail.messages.get(0);
		String method = getIcsPartMethod(message.message);
		assertEquals("REQUEST", method);

	}

	@Test
	public void o2a_expectDraftToBeSentAsNewEvent() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent("invite");
		event.value.main.attendees = addAttendee("u2", "u2@test.lan", event.value.main.attendees);
		event.value.main.draft = true;
		ItemValue<VEventSeries> old = ItemValue.create(event.uid, event.value.copy());
		old.value.main.attendees = addAttendee("u4", "u4@test.lan", old.value.main.attendees);
		event.value.main.attendees = addAttendee("u3", "u3@test.lan", event.value.main.attendees);

		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);
		VEventSanitizer eventSanitizer = new VEventSanitizer(new BmTestContext(securityContext), userCalendar);
		eventSanitizer.sanitize(old.value, false);
		eventSanitizer.sanitize(event.value, true);

		FakeSendmail fakeSendmail = icsHookOnUpdate(securityContext, user1.login, old, event);

		assertTrue(fakeSendmail.mailSent);
		// No cancellation mail to U4
		assertEquals(2, fakeSendmail.messages.size());
		TestMail u2Message = fakeSendmail.messages.get(0);
		String method = getIcsPartMethod(u2Message.message);
		assertEquals("REQUEST", method);
		TestMail u3Message = fakeSendmail.messages.get(1);
		method = getIcsPartMethod(u2Message.message);
		assertEquals("REQUEST", method);
		// Only new event message
		assertEquals(u2Message.message.getSubject(), u3Message.message.getSubject());
	}

	@Test
	public void o2a_expectDraftCancellationNotToBeSent() throws Exception {
		ItemValue<VEventSeries> event = defaultVEvent("invite");
		event.value.main.attendees = addAttendee("u2", "u2@test.lan", event.value.main.attendees);
		event.value.main.draft = true;
		SecurityContext securityContext = Sessions.get().getIfPresent(user1.login);

		FakeSendmail fakeSendmail = icsHookOnDelete(securityContext, user1.login, event);

		assertFalse(fakeSendmail.mailSent);

	}

	private FakeSendmail icsHookOnCreate(SecurityContext securityContext, String userUid, ItemValue<VEventSeries> event)
			throws SQLException {

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;

		veventMessage.vevent = event.value;
		veventMessage.oldEvent = null;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.login);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventCreated(veventMessage);

		return fakeSendmail;
	}

	private FakeSendmail icsHookOnUpdate(SecurityContext securityContext, String userUid,
			ItemValue<VEventSeries> oldEvent, ItemValue<VEventSeries> event) throws SQLException {

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;

		veventMessage.vevent = event.value;
		veventMessage.oldEvent = oldEvent.value;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + userUid);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventUpdated(veventMessage);

		return fakeSendmail;
	}

	private FakeSendmail icsHookOnDelete(SecurityContext securityContext, String userUid,
			ItemValue<VEventSeries> oldEvent) throws SQLException {

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = oldEvent.uid;

		veventMessage.vevent = oldEvent.value;
		veventMessage.oldEvent = null;
		veventMessage.securityContext = securityContext;
		veventMessage.sendNotifications = true;
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + userUid);

		FakeSendmail fakeSendmail = new FakeSendmail();
		new IcsHook(fakeSendmail).onEventDeleted(veventMessage);

		return fakeSendmail;
	}

	private String extractMailBody(Message m) throws IOException {
		assertTrue(m.getBody() instanceof Multipart);
		Multipart body = (Multipart) m.getBody();
		String content = null;
		for (Entity part : body.getBodyParts()) {
			System.err.println(part.getMimeType());
			if ("multipart/alternative".equals(part.getMimeType())) {
				Multipart alt = (Multipart) part.getBody();
				for (Entity e : alt.getBodyParts()) {
					if ("text/html".equals(e.getMimeType())) {
						TextBody tb = (TextBody) e.getBody();
						InputStream in = tb.getInputStream();
						content = new String(ByteStreams.toByteArray(in), e.getCharset());
						in.close();
					}
				}
			}
			if (content != null) {
				break;
			}
		}

		assertNotNull(content);
		return content;
	}

	private String getIcsPartMethod(Message m) {
		assertTrue(m.getBody() instanceof Multipart);
		Multipart body = (Multipart) m.getBody();
		String method = null;
		for (Entity part : body.getBodyParts()) {

			Header h = part.getHeader();
			System.err.println("!!" + h);

			if ("text/calendar".equals(part.getMimeType())) {
				ContentTypeField ctField = (ContentTypeField) h.getField("content-type");
				String mparam = ctField.getParameter("method");
				method = mparam;
			} else if (part instanceof Message) {
				if (((Message) part).isMultipart()) {
					method = getIcsPartMethod((Message) part);
				}
			}
			if (method != null) {
				break;
			}
		}

		assertNotNull(method);
		return method;
	}

	public String getIcsPartAsText(Message m) throws IOException {
		assertTrue(m.getBody() instanceof Multipart);
		String ics = null;
		Multipart body = (Multipart) m.getBody();
		for (Entity part : body.getBodyParts()) {

			if ("text/calendar".equals(part.getMimeType())) {
				TextBody tb = (TextBody) part.getBody();
				ics = IOUtils.toString(tb.getReader());
			} else if (part instanceof Message) {
				if (((Message) part).isMultipart()) {
					ics = getIcsPartAsText((Message) part);
				}
			}
			if (ics != null) {
				break;
			}
		}

		assertNotNull(ics);
		return ics;
	}

	private ItemValue<VEventSeries> defaultVEventWithAttendee(String string, String userUid, String userEmail) {
		ItemValue<VEventSeries> event = defaultVEvent("invite");
		event.value.main.attendees = addAttendee(userUid, userEmail, event.value.main.attendees);
		return event;
	}

	private List<Attendee> addAttendee(String userUid, String userEmail, List<Attendee> attendees) {
		attendees = new ArrayList<>(attendees);
		attendees.add(VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant,
				ParticipationStatus.NeedsAction, false, userUid, null, null, null, null, null, userUid, userEmail));
		return attendees;
	}

	private ItemValue<VEventSeries> defaultVEventWithAttendeeAndSimpleRecur(String string, String userUid,
			String userEmail) {
		ItemValue<VEventSeries> event = defaultVEventWithAttendee(string, userUid, userEmail);

		event.value.icsUid = event.uid;
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		event.value.main.rrule = new VEvent.RRule();
		event.value.main.rrule.frequency = Frequency.DAILY;
		event.value.main.rrule.interval = 10; // ?
		return event;
	}

	private VEventOccurrence createSimpleOccur(VEvent event) {
		BmDateTime recurId = BmDateTimeWrapper.create(new BmDateTimeWrapper(event.dtstart).toDateTime().plusDays(1),
				Precision.DateTime);
		VEventOccurrence occurr = VEventOccurrence.fromEvent(event, recurId);
		occurr.dtstart = BmDateTimeWrapper
				.create(new BmDateTimeWrapper(event.dtstart).toDateTime().plusDays(1).plusHours(1), Precision.DateTime);
		occurr.dtend = BmDateTimeWrapper
				.create(new BmDateTimeWrapper(event.dtend).toDateTime().plusDays(1).plusHours(1), Precision.DateTime);
		return occurr;
	}

}
