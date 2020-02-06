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
package net.bluemind.user.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.service.DirEntryHandler;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserMailIdentities;
import net.bluemind.user.api.User;
import net.bluemind.user.api.UserMailIdentity;

public class UserMailIdentitySanitizerTests {

	protected String domainUid;
	private SecurityContext domainAdminSecurityContext;

	protected Container userContainer;

	private IServer serverService;

	private ItemValue<Server> dataLocation;
	private ContainerStore containerHome;
	private SecurityContext userAdminSecurityContext;
	private SecurityContext userSecurityContext;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		domainUid = "dom" + System.currentTimeMillis() + ".test";
		String sid = "sid" + System.currentTimeMillis();

		domainAdminSecurityContext = BmTestContext
				.contextWithSession(sid, "admin@" + domainUid, domainUid, SecurityContext.ROLE_ADMIN)
				.getSecurityContext();

		userAdminSecurityContext = BmTestContext.contextWithSession("sid2" + System.currentTimeMillis(),
				"useradmin@" + domainUid, domainUid, BasicRoles.ROLE_MANAGE_USER).getSecurityContext();

		userSecurityContext = BmTestContext
				.contextWithSession("sid3" + System.currentTimeMillis(), "user@" + domainUid, domainUid)
				.getSecurityContext();

		containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), domainAdminSecurityContext);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		Server server = new Server();
		server.ip = "prec";
		server.tags = Lists.newArrayList("blue/job", "ur/anus");

		String cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		System.out.println(DirEntryHandler.class);

		PopulateHelper.initGlobalVirt(esServer, server, imapServer);
		PopulateHelper.createTestDomain(domainUid, esServer, imapServer);

		// create domain parititon on cyrus
		new CyrusService(cyrusIp).createPartition(domainUid);
		new CyrusService(cyrusIp).refreshPartitions(Arrays.asList(domainUid));
		new CyrusService(cyrusIp).reload();

		PopulateHelper.addUserWithRoles("useradmin", userAdminSecurityContext.getContainerUid(),
				BasicRoles.ROLE_MANAGE_USER);

		PopulateHelper.addUserWithRoles("user", userSecurityContext.getContainerUid());

		PopulateHelper.domainAdmin(domainUid, domainAdminSecurityContext.getSubject());
		userContainer = containerHome.get(domainUid);
		assertNotNull(userContainer);

		serverService = ServerSideServiceProvider.getProvider(domainAdminSecurityContext).instance(IServer.class,
				InstallationId.getIdentifier());

		dataLocation = serverService.getComplete(cyrusIp);
		System.err.println("srv: " + dataLocation.value.fqdn + ", uid: " + dataLocation.uid);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IUser getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);
	}

	@Test
	public void testUpdatingUsingValidIdentities() throws ServerFault, SQLException {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);

		IUserMailIdentities userMailIdentities = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserMailIdentities.class, domainUid, uid);

		UserMailIdentity mi = new UserMailIdentity();
		mi.mailboxUid = uid;
		mi.displayname = uid;
		mi.isDefault = true;
		mi.email = user.defaultEmail().address;
		mi.name = mi.displayname;
		mi.signature = "my - sig";
		mi.sentFolder = "Sent";
		userMailIdentities.create("mysIg2", mi);

		getService(domainAdminSecurityContext).update(uid, user);

		assertEquals(2, userMailIdentities.getIdentities().size());

	}

	@Test
	public void testUpdatingWithoutIdentitiesShouldCreateDefaultIdentity() throws ServerFault, SQLException {
		User user = defaultUser("test." + System.nanoTime());
		String uid = create(user);

		IUserMailIdentities userMailIdentities = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserMailIdentities.class, domainUid, uid);

		userMailIdentities.getIdentities().forEach(iden -> {
			userMailIdentities.delete(iden.id);
		});

		getService(domainAdminSecurityContext).update(uid, user);

		assertEquals(1, userMailIdentities.getIdentities().size());

	}

	@Test
	public void testUpdatingUsingInvalidDefaultIdentityShouldFixAddress() throws ServerFault, SQLException {
		User user = defaultUser("test." + System.nanoTime());
		user.emails = new ArrayList<>();
		user.emails.add(Email.create("test@test.fr", false));
		user.emails.add(Email.create("idontexist@dead.net", true));
		String uid = create(user);

		IUserMailIdentities userMailIdentities = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserMailIdentities.class, domainUid, uid);

		user.emails = new ArrayList<>();
		user.emails.add(Email.create("test@test.fr", true));

		getService(domainAdminSecurityContext).update(uid, user);

		UserMailIdentity userMailIdentity = userMailIdentities.get("default");
		assertEquals("test@test.fr", userMailIdentity.email);

	}

	@Test
	public void testUpdatingShouldDeleteNonDefaultIdentities() throws ServerFault, SQLException {
		User user = defaultUser("test." + System.nanoTime());
		user.emails.add(Email.create("test-idontexist@dead.net", false));
		user.emails.add(Email.create("test-me-netiher@dead.net", false));
		String uid = create(user);

		IUserMailIdentities userMailIdentities = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserMailIdentities.class, domainUid, uid);

		UserMailIdentity mi = new UserMailIdentity();
		mi.mailboxUid = uid;
		mi.displayname = uid;
		mi.isDefault = true;
		mi.email = "test-idontexist@dead.net";
		mi.name = mi.displayname;
		mi.signature = "my - sig";
		mi.sentFolder = "Sent";
		userMailIdentities.create("mysIg2", mi);

		mi = new UserMailIdentity();
		mi.mailboxUid = uid;
		mi.displayname = uid;
		mi.isDefault = true;
		mi.email = "test-me-netiher@dead.net";
		mi.name = mi.displayname;
		mi.signature = "my - sig";
		mi.sentFolder = "Sent";
		userMailIdentities.create("mysIg3", mi);

		for (Iterator<Email> email = user.emails.iterator(); email.hasNext();) {
			Email next = email.next();
			if (next.address.startsWith("test-")) {
				email.remove();
			}
		}

		getService(domainAdminSecurityContext).update(uid, user);

		assertEquals(1, userMailIdentities.getIdentities().size());
		assertEquals("default", userMailIdentities.getIdentities().get(0).id);

	}

	private String create(User user) {
		String uid = UUID.randomUUID().toString();
		try {
			getService(domainAdminSecurityContext).create(uid, user);
			return uid;
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		return null;
	}

	private User defaultUser(String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + domainUid;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = new ArrayList<>();
		user.emails.add(em);
		user.password = "password";
		user.routing = Routing.internal;
		VCard card = new VCard();
		card.identification.name = Name.create("Doe", "John", null, null, null, null);
		user.contactInfos = card;
		user.dataLocation = dataLocation.uid;
		return user;
	}

}
