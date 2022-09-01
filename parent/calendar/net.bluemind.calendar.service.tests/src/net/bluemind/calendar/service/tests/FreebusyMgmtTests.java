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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusAdmins;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.IFreebusyMgmt;
import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.persistence.UserSubscriptionStore;

public class FreebusyMgmtTests {

	private static final String DOMAIN = "test.lan";
	protected SecurityContext defaultSecurityContext;
	protected SecurityContext anotherSecurityContext;
	protected Container container;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server imapServer = new Server();
		imapServer.ip = new BmConfIni().get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer);

		PopulateHelper.createTestDomain(DOMAIN, imapServer);

		this.createCyrusPartition(imapServer, DOMAIN);

		defaultSecurityContext = new SecurityContext("testUser", "test", Arrays.<String>asList(),
				Arrays.<String>asList(), DOMAIN);

		Sessions.get().put(defaultSecurityContext.getSessionId(), defaultSecurityContext);

		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				defaultSecurityContext);

		AclStore aclStore = new AclStore(JdbcTestHelper.getInstance().getDataSource());

		container = Container.create(UUID.randomUUID().toString(), IFreebusyUids.TYPE, "fb container",
				defaultSecurityContext.getSubject(), DOMAIN, true);
		container = containerStore.create(container);

		Container containerCal1 = Container.create("this-is-calendar", ICalendarUids.TYPE, "this-is-calendar",
				defaultSecurityContext.getSubject(), DOMAIN, false);
		containerStore.create(containerCal1);
		Container containerCal2 = Container.create("this-is-calendar2", ICalendarUids.TYPE, "this-is-calendar2",
				defaultSecurityContext.getSubject(), DOMAIN, false);
		containerStore.create(containerCal2);
		Container containerCal3 = Container.create("this-is-calendar3", ICalendarUids.TYPE, "this-is-calendar3",
				defaultSecurityContext.getSubject(), DOMAIN, false);
		containerStore.create(containerCal3);

		assertNotNull(container);

		aclStore.store(container,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));

		UserSubscriptionStore userSubscriptionStore = new UserSubscriptionStore(SecurityContext.SYSTEM,
				JdbcTestHelper.getInstance().getDataSource(), containerStore.get(DOMAIN));

		userSubscriptionStore.subscribe(defaultSecurityContext.getSubject(), container);

		anotherSecurityContext = new SecurityContext(UUID.randomUUID().toString(), "another", Arrays.<String>asList(),
				Arrays.<String>asList(), "another.lan");

		Sessions.get().put(anotherSecurityContext.getSessionId(), anotherSecurityContext);

	}

	private void createCyrusPartition(final Server imapServer, final String domainUid) {
		final CyrusService cyrusService = new CyrusService(imapServer.ip);
		cyrusService.createPartition(domainUid);
		cyrusService.refreshPartitions(Arrays.asList(domainUid));
		new CyrusAdmins(
				ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class, "default"),
				imapServer.ip).write();
		cyrusService.reload();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testFreebusyMgmt() throws Exception {
		IFreebusyMgmt service = getService(defaultSecurityContext);

		List<String> calendars = service.get();
		assertTrue(calendars.isEmpty());

		service.add("this-is-calendar");
		calendars = service.get();
		assertEquals(1, calendars.size());
		assertTrue(calendars.contains("this-is-calendar"));

		service.add("this-is-calendar2");
		service.add("this-is-calendar3");
		calendars = service.get();
		assertEquals(3, calendars.size());
		assertTrue(calendars.contains("this-is-calendar"));
		assertTrue(calendars.contains("this-is-calendar2"));
		assertTrue(calendars.contains("this-is-calendar3"));

		service.remove("this-is-calendar");
		calendars = service.get();
		assertEquals(2, calendars.size());
		assertTrue(calendars.contains("this-is-calendar2"));
		assertTrue(calendars.contains("this-is-calendar3"));

		service.remove("this-is-wtf");
		calendars = service.get();
		assertEquals(2, calendars.size());
		assertTrue(calendars.contains("this-is-calendar2"));
		assertTrue(calendars.contains("this-is-calendar3"));

		calendars.clear();
		calendars.add("this");
		calendars.add("is");
		calendars.add("calendar");
		// check non-existing calendars
		service.set(calendars);
		calendars = service.get();
		assertEquals(0, calendars.size());

		service = getService(anotherSecurityContext);
		try {
			service.get();
			fail();
		} catch (Exception e) {

		}

		try {
			service.add("bla");
			fail();
		} catch (Exception e) {

		}

		try {
			service.remove("blabla");
			fail();
		} catch (Exception e) {

		}

		try {
			service.set(calendars);
			fail();
		} catch (Exception e) {

		}
	}

	public IFreebusyMgmt getService(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(IFreebusyMgmt.class, container.uid);
	}

}
