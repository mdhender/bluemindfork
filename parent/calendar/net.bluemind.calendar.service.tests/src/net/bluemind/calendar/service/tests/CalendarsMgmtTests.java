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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.backend.cyrus.CyrusAdmins;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;

public class CalendarsMgmtTests {

	private String domainUid;
	private SecurityContext domainAdmin;
	private SecurityContext dummy;
	private SecurityContext dummy2;
	private BmTestContext testContext;

	@Before
	public void before() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		Server imapServer = new Server();
		imapServer.ip = new BmConfIni().get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, imapServer);

		domainUid = "test.lan";

		domainAdmin = BmTestContext.contextWithSession("testUser", "test", domainUid, SecurityContext.ROLE_ADMIN)
				.getSecurityContext();

		PopulateHelper.createTestDomain(domainUid, esServer, imapServer);

		this.createCyrusPartition(imapServer, domainUid);

		PopulateHelper.domainAdmin(domainUid, domainAdmin.getSubject());
		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		dummy = new SecurityContext("dummy", PopulateHelper.addUser("dummy", domainUid), Arrays.<String>asList(),
				Arrays.<String>asList(), domainUid);
		dummy2 = new SecurityContext("dummy2", PopulateHelper.addUser("dummy2", domainUid), Arrays.<String>asList(),
				Arrays.<String>asList(), domainUid);

		Sessions.get().put(dummy.getSessionId(), dummy);
		Sessions.get().put(dummy2.getSessionId(), dummy2);

		testContext = new BmTestContext(SecurityContext.SYSTEM);
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
	public void testCreate_Domain() throws ServerFault, SQLException {
		String calUid = "testdomab" + System.currentTimeMillis();
		ICalendarsMgmt service = service(domainAdmin);
		service.create(calUid, CalendarDescriptor.create("test", domainUid, domainUid));
		CalendarDescriptor cal = service.getComplete(calUid);
		assertNotNull(cal);
		assertEquals(domainUid, cal.domainUid);
		assertEquals(calUid, cal.owner);
		assertEquals("test", cal.name);

		// check dir entry is there
		IDirectory dir = testContext.provider().instance(IDirectory.class, domainUid);
		DirEntry entry = dir.findByEntryUid(calUid);
		assertNotNull(entry);
		assertEquals(DirEntry.Kind.CALENDAR, entry.kind);
		assertEquals("test", entry.displayName);

		calUid = "testdomab" + System.currentTimeMillis();
		service = service(dummy);
		try {
			service.create(calUid, CalendarDescriptor.create("test", domainUid, domainUid));
			fail("should not be possible");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testDelete_Domain() throws ServerFault, SQLException {
		String calUid = "testdomab" + System.currentTimeMillis();
		ICalendarsMgmt service = service(domainAdmin);
		service.create(calUid, CalendarDescriptor.create("test", domainUid, domainUid));

		service.delete(calUid);

		DataSource ds = DataSourceRouter.get(testContext, calUid);
		ContainerStore cs = new ContainerStore(testContext, ds, domainAdmin);
		assertNull(cs.get(calUid));

		// dummy cannot do it
		calUid = "testdomab" + System.currentTimeMillis();
		service = service(domainAdmin);
		service.create(calUid, CalendarDescriptor.create("test", domainUid, domainUid));
		service = service(dummy);

		try {
			service.delete(calUid);
			fail("should not be possible");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ds = DataSourceRouter.get(testContext, calUid);
		cs = new ContainerStore(testContext, ds, domainAdmin);
		assertNotNull(cs.get(calUid));

	}

	@Test
	public void testCreate_User() throws ServerFault, SQLException {
		String calUid = "testdomab" + System.currentTimeMillis();
		ICalendarsMgmt service = service(domainAdmin);
		service.create(calUid, CalendarDescriptor.create("test", dummy.getSubject(), domainUid));
		CalendarDescriptor cal = service.getComplete(calUid);
		assertNotNull(cal);
		assertEquals(domainUid, cal.domainUid);
		assertEquals(dummy.getSubject(), cal.owner);
		assertEquals("test", cal.name);

		// check dir entry is not there
		IDirectory dir = testContext.provider().instance(IDirectory.class, domainUid);
		DirEntry entry = dir.findByEntryUid(calUid);
		assertNull(entry);

		calUid = "testdomab" + System.currentTimeMillis();
		service = service(dummy);
		// dummy can do it
		service.create(calUid, CalendarDescriptor.create("test", dummy.getSubject(), domainUid));
		calUid = "testdomab" + System.currentTimeMillis();
		service = service(dummy);

		try {
			service.create(calUid, CalendarDescriptor.create("test", dummy2.getSubject(), domainUid));
			fail("should not be possible");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testDelete_User() throws ServerFault, SQLException {
		String calUid = "testdomab" + System.currentTimeMillis();
		ICalendarsMgmt service = service(dummy);
		service.create(calUid, CalendarDescriptor.create("test", dummy.getSubject(), domainUid));

		service.delete(calUid);
		DataSource ds = DataSourceRouter.get(testContext, calUid);
		ContainerStore cs = new ContainerStore(testContext, ds, domainAdmin);
		assertNull(cs.get(calUid));

		// dummy2 cannot do it
		calUid = "testdomab" + System.currentTimeMillis();
		service.create(calUid, CalendarDescriptor.create("test", dummy.getSubject(), domainUid));
		service = service(dummy2);
		try {
			service.delete(calUid);
			fail("should not be possible");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
		assertNotNull(cs.get(calUid));

	}

	private ICalendarsMgmt service(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(ICalendarsMgmt.class, domainUid);
	}
}
