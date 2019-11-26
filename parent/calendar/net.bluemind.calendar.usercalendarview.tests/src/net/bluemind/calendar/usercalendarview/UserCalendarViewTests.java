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
package net.bluemind.calendar.usercalendarview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.core.BMCore;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.AuthFault;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.io.Files;

public class UserCalendarViewTests {
	private String domainUid;
	private SecurityContext defaultSecurityContext;
	protected Container domain;
	private Container installation;
	private IServer serverService;
	private ItemValue<Server> serverValue;

	@Before
	public void before() throws Exception {
		BMCore.start();
		JdbcTestHelper.getInstance().beforeTest();

		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		JdbcActivator.getInstance().setDataSource(
				JdbcTestHelper.getInstance().getDataSource());

		defaultSecurityContext = new SecurityContext("user", "test",
				Arrays.<String> asList(), Arrays.<String> asList(), domainUid);
		Sessions.get().put(defaultSecurityContext.getSessionId(),
				defaultSecurityContext);

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper
				.getInstance().getDataSource(), defaultSecurityContext);

		domainUid = "testDomainUid";
		domain = Container.create(domainUid, "domain", domainUid, "me");
		domain = containerHome.create(domain);
		assertNotNull(domain);

		Container usersBook = Container.create("users_" + domainUid,
				"addressbook", domain.name + " users", "me");
		usersBook = containerHome.create(usersBook);

		AclStore aclStore = new AclStore(JdbcTestHelper.getInstance()
				.getDataSource());
		aclStore.store(
				domain,
				Arrays.asList(AccessControlEntry.create(
						defaultSecurityContext.getSubject(), Verb.All)));
		aclStore.store(
				usersBook,
				Arrays.asList(AccessControlEntry.create(
						defaultSecurityContext.getSubject(), Verb.All)));

		installation = Container.create(
				"bluemind-"
						+ Files.toString(new File("/etc/bm/mcast.id"),
								Charset.defaultCharset()), "installation",
				"installation", "me");
		installation = containerHome.create(installation);
		aclStore.store(
				installation,
				Arrays.asList(AccessControlEntry.create(
						defaultSecurityContext.getSubject(), Verb.All)));

		serverService = ServerSideServiceProvider.getProvider(
				defaultSecurityContext).instance(IServer.class,
				installation.uid);

		Server server = new Server();
		BmConfIni conf = new BmConfIni();
		server.fqdn = conf.get("host");
		server.tags = new String[] { "blue/job", "ur/anus" };
		serverService.create(server.fqdn, server);
		serverValue = serverService.getComplete(server.fqdn);

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		BMCore.stop();
	}

	protected IUser getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(
				IUser.class, domain.uid);
	}

	@Test
	public void testCreate() throws ServerFault, AuthFault, SQLException,
			InterruptedException {

		String login = "test." + System.nanoTime();
		User user = defaultUser(login);
		getService(defaultSecurityContext).create(user.uid, user);

		Thread.sleep(1000); // FIXME

		ServerSideServiceProvider.getProvider(defaultSecurityContext)
				.instance(IContainerManagement.class, getContainerUid(user))
				.getAccessControlList();

		List<AccessControlEntry> list = ServerSideServiceProvider
				.getProvider(defaultSecurityContext)
				.instance(IContainerManagement.class, getContainerUid(user))
				.getAccessControlList();
		assertNotNull(list);
		assertEquals(1, list.size());
		AccessControlEntry accessControlEntry = list.get(0);
		assertEquals(getUserSubject(user), accessControlEntry.subject);
		assertEquals(Verb.All, accessControlEntry.verb);

		getService(defaultSecurityContext).delete(user.uid);

		Thread.sleep(1000); // FIXME

		try {
			ServerSideServiceProvider
					.getProvider(defaultSecurityContext)
					.instance(IContainerManagement.class, getContainerUid(user))
					.getAccessControlList();
			fail();
		} catch (ServerFault sf) {

		}
	}

	private static String getUserSubject(User user) {
		return "user:" + user.uid;
	}

	private static String getContainerUid(User user) {
		return "calendarview:" + user.uid;
	}

	private User defaultUser(String login) {
		User user = new User();
		user.uid = UUID.randomUUID().toString();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + domainUid;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = User.Routing.internal;
		VCard card = new VCard();
		card.identification.name = Name.create("Doe", "John", null, null, null,
				null);
		user.contactInfos = card;
		user.dataLocation = serverValue;
		return user;
	}
}
