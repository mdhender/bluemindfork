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
package net.bluemind.calendar.usercalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.calendar.api.CalendarView;
import net.bluemind.calendar.api.CalendarView.CalendarViewType;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarView;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.service.internal.UserCalendarService;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTimeHelper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class UserCalendarTests {

	private String domainUid;
	private SecurityContext defaultSecurityContext;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		defaultSecurityContext = SecurityContext.SYSTEM;

		domainUid = "bm.lan";

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);
		PopulateHelper.addDomain(domainUid);

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();
		ElasticsearchTestHelper.getInstance().beforeTest();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IUser getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);
	}

	@Test
	public void testDefaultCalendar() throws Exception {

		String login = "test." + System.nanoTime();
		ItemValue<User> user = defaultUser(login);
		getService(defaultSecurityContext).create(user.uid, user.value);

		Thread.sleep(1000); // FIXME

		List<AccessControlEntry> list = ServerSideServiceProvider.getProvider(defaultSecurityContext)
				.instance(IContainerManagement.class, ICalendarUids.defaultUserCalendar(user.uid))
				.getAccessControlList();
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEquals(ImmutableSet.of(//
				AccessControlEntry.create(user.uid, Verb.All), //
				AccessControlEntry.create(domainUid, Verb.Invitation)//
		), ImmutableSet.copyOf(list));

		ICalendar cal = ServerSideServiceProvider.getProvider(defaultSecurityContext).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar(user.uid));
		VEventSeries event = defaultEvent(user.value.defaultEmail().address);
		cal.create("evt1", event, false);

		TaskRef tr = getService(defaultSecurityContext).delete(user.uid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(defaultSecurityContext), tr);

		try {
			ServerSideServiceProvider.getProvider(defaultSecurityContext)
					.instance(IContainerManagement.class, ICalendarUids.defaultUserCalendar(user.uid))
					.getAccessControlList();
			fail();
		} catch (ServerFault sf) {

		}

		IContainers cs = ServerSideServiceProvider.getProvider(defaultSecurityContext).instance(IContainers.class);
		assertNull(cs.getIfPresent(ICalendarUids.defaultUserCalendar(user.uid)));

	}

	@Test
	public void testCalendarView() throws Exception {

		String login = "test." + System.nanoTime();
		ItemValue<User> user = defaultUser(login);
		getService(defaultSecurityContext).create(user.uid, user.value);

		Thread.sleep(1000); // FIXME

		ServerSideServiceProvider.getProvider(defaultSecurityContext)
				.instance(IContainerManagement.class, getViewContainerUid(user)).getAccessControlList();

		List<AccessControlEntry> list = ServerSideServiceProvider.getProvider(defaultSecurityContext)
				.instance(IContainerManagement.class, getViewContainerUid(user)).getAccessControlList();
		assertNotNull(list);
		assertEquals(1, list.size());
		AccessControlEntry accessControlEntry = list.get(0);
		assertEquals(user.uid, accessControlEntry.subject);
		assertEquals(Verb.All, accessControlEntry.verb);

		TaskRef tr = getService(defaultSecurityContext).delete(user.uid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(defaultSecurityContext), tr);

		try {
			ServerSideServiceProvider.getProvider(defaultSecurityContext)
					.instance(IContainerManagement.class, getViewContainerUid(user)).getAccessControlList();
			fail();
		} catch (ServerFault sf) {

		}
	}

	@Test
	public void testFreebusy() throws Exception {
		String login = "test." + System.nanoTime();
		ItemValue<User> user = defaultUser(login);
		getService(defaultSecurityContext).create(user.uid, user.value);

		Thread.sleep(1000); // FIXME

		List<AccessControlEntry> list = ServerSideServiceProvider.getProvider(defaultSecurityContext)
				.instance(IContainerManagement.class, getFreebusyContainerUid(user)).getAccessControlList();
		assertNotNull(list);
		assertEquals(2, list.size());

		boolean foundPub = false;
		boolean foundMine = false;

		for (AccessControlEntry ace : list) {
			if (user.uid.equals(ace.subject)) {
				foundMine = true;
				assertEquals(Verb.All, ace.verb);
			}
			if (domainUid.equals(ace.subject)) {
				foundPub = true;
				assertEquals(Verb.Read, ace.verb);
			}
		}

		assertTrue(foundPub);
		assertTrue(foundMine);

		TaskRef tr = getService(defaultSecurityContext).delete(user.uid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(defaultSecurityContext), tr);

		try {
			ServerSideServiceProvider.getProvider(defaultSecurityContext)
					.instance(IContainerManagement.class, getFreebusyContainerUid(user)).getAccessControlList();
			fail();
		} catch (ServerFault sf) {

		}

	}

	private String getViewContainerUid(ItemValue<User> user) {
		return "calendarview:" + user.uid;
	}

	private String getFreebusyContainerUid(ItemValue<User> user) {
		return "freebusy:" + user.uid;
	}

	public void testUserCreatedCalendar() throws ServerFault, SQLException, InterruptedException {
		String login = "test." + System.nanoTime();
		ItemValue<User> user = defaultUser(login);
		getService(defaultSecurityContext).create(user.uid, user.value);

		UserCalendarService service = new UserCalendarService(defaultSecurityContext);

		String calendarName1 = "Calendar " + System.currentTimeMillis();
		// FIXME container.owner VS sc.subject
		String uid = service.create(domainUid, user, calendarName1);

		Thread.sleep(1000); // FIXME

		List<AccessControlEntry> list = ServerSideServiceProvider.getProvider(defaultSecurityContext)
				.instance(IContainerManagement.class, uid).getAccessControlList();
		assertNotNull(list);
		assertEquals(1, list.size());
		AccessControlEntry accessControlEntry = list.get(0);
		assertEquals(user.uid, accessControlEntry.subject);
		assertEquals(Verb.All, accessControlEntry.verb);

		service.delete(uid);

		try {
			ServerSideServiceProvider.getProvider(defaultSecurityContext).instance(IContainerManagement.class, uid)
					.getAccessControlList();
			fail();
		} catch (ServerFault sf) {

		}
	}

	@Test
	public void testSimpleUserCalendar_Acl() throws ServerFault {

		IDomainSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> domainSettings = settings.get();
		domainSettings.put(DomainSettingsKeys.domain_max_basic_account.name(), "");
		settings.set(domainSettings);

		String login = "test." + System.nanoTime();
		ItemValue<User> user = defaultUser(login);
		user.value.accountType = AccountType.SIMPLE;
		getService(defaultSecurityContext).create(user.uid, user.value);

		List<AccessControlEntry> list = ServerSideServiceProvider.getProvider(defaultSecurityContext)
				.instance(IContainerManagement.class, ICalendarUids.defaultUserCalendar(user.uid))
				.getAccessControlList();
		assertNotNull(list);
		assertEquals(1, list.size());
		AccessControlEntry accessControlEntry = list.get(0);
		assertEquals(user.uid, accessControlEntry.subject);
		assertEquals(Verb.All, accessControlEntry.verb);
	}

	@Test
	public void testDefaultView() throws ServerFault {
		String login = "test." + System.nanoTime();
		ItemValue<User> user = defaultUser(login);
		getService(defaultSecurityContext).create(user.uid, user.value);

		ICalendarView service = ServerSideServiceProvider.getProvider(defaultSecurityContext)
				.instance(ICalendarView.class, "calendarview:" + user.uid);
		ListResult<ItemValue<CalendarView>> views = service.list();
		assertEquals(1, views.total);
		CalendarView defaultView = views.values.get(0).value;
		assertTrue(defaultView.isDefault);
		assertEquals("$$calendarhome$$", defaultView.label);
		assertEquals(CalendarViewType.WEEK, defaultView.type);
		assertEquals(1, defaultView.calendars.size());
		assertEquals(ICalendarUids.defaultUserCalendar(user.uid), defaultView.calendars.get(0));
	}

	private ItemValue<User> defaultUser(String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + domainUid;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.none;
		VCard card = new VCard();
		card.identification.name = Name.create("Doe", "John", null, null, null, null);
		user.contactInfos = card;
		user.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		return ItemValue.create(login, user);
	}

	private VEventSeries defaultEvent(String userEmail) {
		VEventSeries series = new VEventSeries();
		series.icsUid = UUID.randomUUID().toString();

		VEvent event = new VEvent();

		event.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh")));
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		event.organizer = new VEvent.Organizer(userEmail);

		List<VEvent.Attendee> attendees = new ArrayList<>();

		event.attendees = attendees;

		event.categories = new ArrayList<TagRef>();
		series.main = event;
		return series;
	}
}
