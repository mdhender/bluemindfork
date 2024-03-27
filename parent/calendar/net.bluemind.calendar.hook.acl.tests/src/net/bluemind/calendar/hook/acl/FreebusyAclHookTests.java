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
package net.bluemind.calendar.hook.acl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.service.internal.MailboxStoreService;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;
import net.bluemind.user.persistence.UserSubscriptionStore;
import net.bluemind.user.service.internal.ContainerUserStoreService;

// prefixes :
// o2a => organiser to attendees, 
// o2a_XXX => create
// o2a_XXX_XXX => update
// o2a_XXX_ => delete
// MasterNotAttendee$OccurrenceAttendee => attendee not in master but pressent in occurence
// a2o => attendee to organiser
public class FreebusyAclHookTests {
	private static final long NOW = System.currentTimeMillis();
	private String domainUid;
	private ContainerStore containerHome;
	private Container userContainer;
	private User user1;
	private User user2;
	private User user3;
	private String user1Uid;
	private String user2Uid;
	private String user3Uid;
	protected MailboxStoreService mailboxStore;
	private ContainerUserStoreService userStoreService;
	private Container user1Calendar;
	private Container user2Calendar;
	private Container user3Calendar;
	protected DataSource dataDataSource;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		domainUid = "test.lan";

		Server pipo = new Server();
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		PopulateHelper.initGlobalVirt(pipo);
		dataDataSource = JdbcActivator.getInstance().getMailboxDataSource(pipo.ip);

		ItemValue<Server> dataLocation = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).getComplete(PopulateHelper.FAKE_CYRUS_IP);

		containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM);
		initDomain(dataLocation, pipo);
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

		user1Uid = PopulateHelper.addUser("u1", domainUid);
		user1 = PopulateHelper.getUser("u1", domainUid, Mailbox.Routing.internal);
		user1Calendar = createTestContainer(dataLocation, user1.login, user1Uid);

		user2Uid = PopulateHelper.addUser("u2", domainUid);
		user2 = PopulateHelper.getUser("u2", domainUid, Mailbox.Routing.internal);
		user2Calendar = createTestContainer(dataLocation, user2.login, user2Uid);

		user3Uid = PopulateHelper.addUser("u3", domainUid);
		user3 = PopulateHelper.getUser("u3", domainUid, Mailbox.Routing.internal);
		user3Calendar = createTestContainer(dataLocation, user3.login, user3Uid);

	}

	private Container createTestContainer(ItemValue<Server> dataLocation, String login, String userUid)
			throws ServerFault, SQLException {
		SecurityContext securityContext = new SecurityContext(login, login, new ArrayList<String>(),
				new ArrayList<String>(), domainUid);

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		Container cal = Container.create(ICalendarUids.TYPE + ":Default:" + userUid, ICalendarUids.TYPE,
				ICalendarUids.TYPE + ":Default:" + userUid, userUid, "test.lan", true);
		cal = containerHome.create(cal);
		Container fb = Container.create(IFreebusyUids.getFreebusyContainerUid(userUid), IFreebusyUids.TYPE,
				IFreebusyUids.getFreebusyContainerUid(userUid), userUid, "test.lan", true);
		fb = containerHome.create(fb);
		Container dom = containerHome.get(domainUid);

		UserSubscriptionStore userSubscriptionStore = new UserSubscriptionStore(SecurityContext.SYSTEM,
				JdbcTestHelper.getInstance().getDataSource(), dom);

		userSubscriptionStore.subscribe(securityContext.getSubject(), cal);

		Sessions.get().put(login, securityContext);

		return cal;
	}

	@Test
	public void testAddingFreebusyAclToCalendar() {
		IContainerManagement calcontainerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, ICalendarUids.defaultUserCalendar(user1Uid));

		AccessControlEntry me = new AccessControlEntry();
		me.subject = user2Uid;
		me.verb = Verb.Freebusy;
		calcontainerService.setAccessControlList(Arrays.asList(me));
		IContainerManagement fbcontainerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IFreebusyUids.getFreebusyContainerUid(user1Uid));

		List<AccessControlEntry> acl = fbcontainerService.getAccessControlList();
		assertTrue(acl.stream().filter(ace -> ace.subject.equals(user2Uid) && ace.verb.can(Verb.Read)).findAny()
				.isPresent());
	}

	@Test
	public void testAddingManageFreebusyAclToCalendar() {
		IContainerManagement calcontainerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, ICalendarUids.defaultUserCalendar(user1Uid));

		AccessControlEntry me = new AccessControlEntry();
		me.subject = user3Uid;
		me.verb = Verb.Manage;
		AccessControlEntry me2 = new AccessControlEntry();
		me2.subject = user2Uid;
		me2.verb = Verb.Write;
		calcontainerService.setAccessControlList(Arrays.asList(me, me2));

		IContainerManagement fbcontainerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IFreebusyUids.getFreebusyContainerUid(user1Uid));
		List<AccessControlEntry> acl = fbcontainerService.getAccessControlList();
		assertTrue(acl.stream().filter(ace -> ace.subject.equals(user2Uid) && ace.verb.can(Verb.Read)).findAny()
				.isPresent());
		assertTrue(acl.stream().filter(ace -> ace.subject.equals(user3Uid) && ace.verb.can(Verb.Manage)).findAny()
				.isPresent());
	}

	@Test
	public void testAddingFreebusyAclToCalendarToUserWhoAlreadyHasTheRight() {
		IContainerManagement calContainerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, ICalendarUids.defaultUserCalendar(user1Uid));
		AccessControlEntry me = new AccessControlEntry();
		me.subject = user2Uid;
		me.verb = Verb.Freebusy;
		calContainerService.setAccessControlList(Arrays.asList(me));

		IContainerManagement fbContainerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IFreebusyUids.getFreebusyContainerUid(user1Uid));
		AccessControlEntry me2 = new AccessControlEntry();
		me2.subject = user2Uid;
		me2.verb = Verb.Read;
		fbContainerService.setAccessControlList(Arrays.asList(me2));

		List<AccessControlEntry> acl = calContainerService.getAccessControlList();
		assertEquals(1,
				acl.stream().filter(ace -> ace.subject.equals(user2Uid) && ace.verb.can(Verb.Freebusy)).count());
	}

	@Test
	public void testRemovingFreebusyAclToCalendar() {
		IContainerManagement calContainerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, ICalendarUids.defaultUserCalendar(user1Uid));
		AccessControlEntry me = new AccessControlEntry();
		me.subject = user2Uid;
		me.verb = Verb.Freebusy;
		calContainerService.setAccessControlList(Arrays.asList(me));

		AccessControlEntry me2 = new AccessControlEntry();
		me2.subject = user2Uid;
		me2.verb = Verb.Invitation;
		calContainerService.setAccessControlList(Arrays.asList(me2));
		IContainerManagement fbcontainerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IFreebusyUids.getFreebusyContainerUid(user1Uid));

		List<AccessControlEntry> acl = fbcontainerService.getAccessControlList();
		assertFalse(acl.stream().filter(ace -> ace.subject.equals(user2Uid) && ace.verb.can(Verb.Read)).findAny()
				.isPresent());
	}

	@Test
	public void testKeepExistingAclOnContainer() {
		IContainerManagement calContainerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, ICalendarUids.defaultUserCalendar(user1Uid));
		AccessControlEntry me = new AccessControlEntry();
		me.subject = user2Uid;
		me.verb = Verb.Write;
		calContainerService.setAccessControlList(Arrays.asList(me));
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}
}
