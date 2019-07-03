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
package net.bluemind.resource.hook.ics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.hook.internal.VEventMessage;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.group.persistance.GroupStore;
import net.bluemind.group.service.internal.ContainerGroupStoreService;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.RRule;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;
import net.bluemind.icalendar.api.ICalendarElement.Role;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.service.internal.ResourceContainerStoreService;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;
import net.bluemind.user.service.internal.ContainerUserStoreService;

public class IcsHookTests {
	private String domainUid;
	private Container dirContainer;
	private ResourceContainerStoreService resourceStore;
	private String dataLocationUid;
	private ItemValue<User> user2;
	private SecurityContext user2SecurityContext;
	private ItemValue<User> user1;
	private SecurityContext user1SecurityContext;
	private ItemValue<User> admin;
	private SecurityContext adminSecurityContext;
	private String resourceUid;
	private ResourceDescriptor rd;
	private ItemValue<Group> group1;
	private Container resourceCalendarContainer;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		domainUid = "dom" + System.currentTimeMillis() + ".test";

		Server imapServer = new Server();
		imapServer.ip = new BmConfIni().get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer);

		ItemValue<Server> dataLocation = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).getComplete(imapServer.ip);
		dataLocationUid = dataLocation.uid;

		ItemValue<Domain> domain = initDomain(dataLocation);

		resourceUid = createResource(domain);

		ContainerStore containerHome = new ContainerStore(null,
				DataSourceRouter.get(new BmTestContext(adminSecurityContext), ICalendarUids.TYPE + ":" + resourceUid),
				adminSecurityContext);
		resourceCalendarContainer = containerHome.get(ICalendarUids.TYPE + ":" + resourceUid);

		IContainerManagement containerMgmt = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, ICalendarUids.TYPE + ":" + resourceUid);
		containerMgmt.setAccessControlList(Arrays.asList(AccessControlEntry.create(user1.uid, Verb.Write),
				AccessControlEntry.create(group1.uid, Verb.Write)));
	}

	private ItemValue<Domain> initDomain(ItemValue<Server> dataLocation, Server... servers) throws Exception {
		ItemValue<Domain> domain = PopulateHelper.createTestDomain(domainUid, servers);

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);
		dirContainer = containerHome.get(domainUid);

		ContainerUserStoreService userStoreService = new ContainerUserStoreService(
				new BmTestContext(SecurityContext.SYSTEM), dirContainer, domain);

		String nt = "" + System.nanoTime();
		String adm = "adm" + nt;
		admin = defaultUser(dataLocation, adm, adm);
		userStoreService.create(admin.uid, adm, admin.value);
		adminSecurityContext = new SecurityContext(adm, adm, new ArrayList<String>(),
				Arrays.asList(SecurityContext.ROLE_ADMIN), domainUid);
		Sessions.get().put(adm, adminSecurityContext);

		String u1 = "u1." + nt;
		user1 = defaultUser(dataLocation, u1, u1);
		userStoreService.create(user1.uid, u1, user1.value);
		user1SecurityContext = new SecurityContext(u1, u1, new ArrayList<String>(), new ArrayList<String>(), domainUid);
		Sessions.get().put(u1, user1SecurityContext);

		String u2 = "u2." + nt;
		user2 = defaultUser(dataLocation, u2, u2);
		userStoreService.create(user2.uid, u2, user2.value);
		user2SecurityContext = new SecurityContext(u1, u1, new ArrayList<String>(), new ArrayList<String>(), domainUid);
		Sessions.get().put(u1, user2SecurityContext);

		GroupStore groupStore = new GroupStore(JdbcTestHelper.getInstance().getDataSource(), dirContainer);
		ContainerGroupStoreService groupStoreService = new ContainerGroupStoreService(
				new BmTestContext(SecurityContext.SYSTEM), dirContainer, domain);

		String g1 = "g1." + nt;
		group1 = ItemValue.create(g1, getDefaultGroup(g1));
		groupStoreService.create(group1.uid, group1.value.name, group1.value);

		Item g1Item = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), dirContainer, SecurityContext.SYSTEM)
				.get(g1);

		groupStore.addUsersMembers(g1Item,
				new ItemStore(JdbcTestHelper.getInstance().getDataSource(), dirContainer, SecurityContext.SYSTEM)
						.getMultiple(Arrays.asList(user2.uid)));
		return domain;
	}

	private Group getDefaultGroup(String uid) {
		Group g = new Group();
		g.name = "group-" + System.nanoTime();
		g.description = "description " + g.name;

		g.hidden = false;
		g.hiddenMembers = false;

		Email e = new Email();
		e.address = g.name + "@blue-mind.loc";
		g.emails = new ArrayList<Email>(1);
		g.emails.add(e);

		return g;
	}

	private ItemValue<User> defaultUser(ItemValue<Server> dataLocation, String uid, String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + dirContainer.uid;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.none;
		user.dataLocation = dataLocation.uid;

		VCard card = new VCard();
		card.identification.name = Name.create("Doe", "John", null, null, null, null);
		user.contactInfos = card;
		return ItemValue.create(uid, user);
	}

	private String createResource(ItemValue<Domain> domain) throws ServerFault, SQLException {
		resourceStore = new ResourceContainerStoreService(new BmTestContext(SecurityContext.SYSTEM), domain,
				dirContainer);

		String rdUid = "resource-" + System.nanoTime();
		rd = defaultDescriptor();
		resourceStore.create(rdUid, rd);

		ContainerDescriptor calContainerDescriptor = ContainerDescriptor.create(ICalendarUids.TYPE + ":" + rdUid,
				"Calendar of " + rd.label, rdUid, ICalendarUids.TYPE, domainUid, true);

		IContainers containers = ServerSideServiceProvider.getProvider(adminSecurityContext)
				.instance(IContainers.class);

		containers.create(calContainerDescriptor.uid, calContainerDescriptor);

		return rdUid;
	}

	private ResourceDescriptor defaultDescriptor() {
		ResourceDescriptor rd = new ResourceDescriptor();
		rd.label = "test 1";
		rd.description = "hi !";
		rd.typeIdentifier = "testType";
		rd.dataLocation = dataLocationUid;
		rd.emails = Arrays.asList(Email.create("test1@" + domainUid, true));
		rd.properties = Arrays.asList(ResourceDescriptor.PropertyValue.create("test1", "value1"));
		return rd;
	}

	private ItemValue<VEventSeries> defaultVEvent(String title) {
		VEvent event = new VEvent();
		DateTimeZone tz = DateTimeZone.forID("Europe/Paris");

		long now = System.currentTimeMillis();
		long start = now + (1000 * 60 * 60);
		DateTime temp = new DateTime(start, tz);
		event.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		temp = new DateTime(start + (1000 * 60 * 60), tz);
		event.dtend = BmDateTimeWrapper.create(temp, Precision.DateTime);
		event.summary = title + "-" + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.priority = 1;
		event.organizer = new VEvent.Organizer("U1", "u1@apr-vmnet.loc");
		event.organizer.uri = "";
		event.attendees = new ArrayList<>();
		event.categories = new ArrayList<TagRef>(0);

		event.rdate = new HashSet<BmDateTime>();
		event.rdate.add(BmDateTimeWrapper.create(temp, Precision.Date));

		VEventSeries series = new VEventSeries();
		series.main = event;
		return ItemValue.create(UUID.randomUUID().toString(), series);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void notResourceCalendar() throws SQLException, ServerFault {
		ContainerDescriptor calContainerDescriptor = ContainerDescriptor.create(
				ICalendarUids.TYPE + ":Default:" + user1.uid, "Calendar of " + user1.value.login, user1.uid,
				ICalendarUids.TYPE, domainUid, true);

		IContainers containers = ServerSideServiceProvider.getProvider(adminSecurityContext)
				.instance(IContainers.class);

		containers.create(calContainerDescriptor.uid, calContainerDescriptor);

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.securityContext = adminSecurityContext;
		veventMessage.sendNotifications = false;

		ContainerStore containerHome = new ContainerStore(null, DataSourceRouter
				.get(new BmTestContext(adminSecurityContext), ICalendarUids.TYPE + ":Default:" + user1.uid),
				adminSecurityContext);
		veventMessage.container = containerHome.get(ICalendarUids.TYPE + ":Default:" + user1.uid);

		FakeSendmail fakeSendmail = new FakeSendmail();
		try {
			new ResourceIcsHook(fakeSendmail).onEventCreated(veventMessage);
			fail();
		} catch (Exception e) {

		}
		try {
			new ResourceIcsHook(fakeSendmail).onEventUpdated(veventMessage);
			fail();
		} catch (Exception e) {

		}
		try {
			new ResourceIcsHook(fakeSendmail).onEventDeleted(veventMessage);
			fail();
		} catch (Exception e) {

		}
	}

	@Test
	public void createHook() throws ServerFault, SQLException {
		ItemValue<VEventSeries> event = defaultVEvent("createHook");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		event.value.main.attendees.add(
				VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "admin", "", "", "", "", "", "", admin.value.emails.iterator().next().address));

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.securityContext = adminSecurityContext;
		veventMessage.sendNotifications = false;
		veventMessage.container = resourceCalendarContainer;

		FakeSendmail fakeSendmail = new FakeSendmail();
		new ResourceIcsHook(fakeSendmail).onEventCreated(veventMessage);
		assertTrue(fakeSendmail.mailSent);

		assertEquals(1, fakeSendmail.from.size());
		assertEquals(rd.emails.iterator().next().address, fakeSendmail.from.iterator().next());

		assertEquals(2, fakeSendmail.to.size());
		assertTrue(fakeSendmail.to.contains(user1.value.emails.iterator().next().address));
		assertTrue(fakeSendmail.to.contains(user2.value.emails.iterator().next().address));
	}

	@Test
	public void updateHook() throws ServerFault, SQLException {
		ItemValue<VEventSeries> event = defaultVEvent("createHook");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		event.value.main.attendees.add(
				VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "admin", "", "", "", "", "", "", admin.value.emails.iterator().next().address));

		ItemValue<VEventSeries> old = defaultVEvent("createHook");
		old.value.main.location = "somewhere over the rainbow";

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = old.value;
		veventMessage.securityContext = adminSecurityContext;
		veventMessage.sendNotifications = false;
		veventMessage.container = resourceCalendarContainer;

		FakeSendmail fakeSendmail = new FakeSendmail();
		new ResourceIcsHook(fakeSendmail).onEventUpdated(veventMessage);

		assertEquals(1, fakeSendmail.from.size());
		assertEquals(rd.emails.iterator().next().address, fakeSendmail.from.iterator().next());

		assertEquals(2, fakeSendmail.to.size());
		assertTrue(fakeSendmail.to.contains(user1.value.emails.iterator().next().address));
		assertTrue(fakeSendmail.to.contains(user2.value.emails.iterator().next().address));
	}

	@Test
	public void deleteHook() throws ServerFault, SQLException {
		ItemValue<VEventSeries> event = defaultVEvent("createHook");
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		event.value.main.attendees.add(
				VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "admin", "", "", "", "", "", "", admin.value.emails.iterator().next().address));

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.securityContext = adminSecurityContext;
		veventMessage.sendNotifications = false;
		veventMessage.container = resourceCalendarContainer;

		FakeSendmail fakeSendmail = new FakeSendmail();
		new ResourceIcsHook(fakeSendmail).onEventDeleted(veventMessage);
		assertTrue(fakeSendmail.mailSent);
	}

	@Test
	public void updateHook_addException() throws ServerFault, SQLException {
		ItemValue<VEventSeries> old = defaultVEvent("createHook");
		old.value.main.rrule = new RRule();
		old.value.main.rrule.frequency = Frequency.DAILY;
		old.value.main.rrule.interval = 1;
		old.value.main.status = ICalendarElement.Status.NeedsAction;
		old.value.main.attendees.add(
				VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "admin", "", "", "", "", "", "", admin.value.emails.iterator().next().address));

		ItemValue<VEventSeries> event = defaultVEvent("createHook");
		event.value.main.rrule = new RRule();
		event.value.main.rrule.frequency = Frequency.DAILY;
		event.value.main.rrule.interval = 1;
		event.value.main.status = ICalendarElement.Status.NeedsAction;
		event.value.main.attendees.add(
				VEvent.Attendee.create(CUType.Individual, "", Role.RequiredParticipant, ParticipationStatus.NeedsAction,
						false, "admin", "", "", "", "", "", "", admin.value.emails.iterator().next().address));
		event.value.occurrences = Arrays.asList(createSimpleOccur(event.value.main));

		VEventMessage veventMessage = new VEventMessage();
		veventMessage.itemUid = event.uid;
		veventMessage.vevent = event.value;
		veventMessage.oldEvent = old.value;
		veventMessage.securityContext = adminSecurityContext;
		veventMessage.sendNotifications = false;
		veventMessage.container = resourceCalendarContainer;

		FakeSendmail fakeSendmail = new FakeSendmail();
		new ResourceIcsHook(fakeSendmail).onEventUpdated(veventMessage);

		assertEquals(1, fakeSendmail.from.size());
		assertEquals(rd.emails.iterator().next().address, fakeSendmail.from.iterator().next());

		assertEquals(2, fakeSendmail.to.size());
		assertTrue(fakeSendmail.to.contains(user1.value.emails.iterator().next().address));
		assertTrue(fakeSendmail.to.contains(user2.value.emails.iterator().next().address));
	}

	private VEventOccurrence createSimpleOccur(VEvent event) {
		BmDateTime recurId = BmDateTimeWrapper.create(new BmDateTimeWrapper(event.dtstart).toJodaTime().plusDays(1),
				Precision.DateTime);
		VEventOccurrence occurr = VEventOccurrence.fromEvent(event, recurId);
		occurr.dtstart = BmDateTimeWrapper
				.create(new BmDateTimeWrapper(event.dtstart).toJodaTime().plusDays(1).plusHours(1), Precision.DateTime);
		occurr.dtend = BmDateTimeWrapper
				.create(new BmDateTimeWrapper(event.dtend).toJodaTime().plusDays(1).plusHours(1), Precision.DateTime);
		return occurr;
	}

}