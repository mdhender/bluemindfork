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
package net.bluemind.backend.cyrus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class CyrusBackendTests {

	private String domainUid = "bm.lan";
	private ItemValue<Server> dataLocation;
	private ItemValue<Server> dataLocation2;

	private String cyrusIp;
	private String cyrusIp2;

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

		cyrusIp = new BmConfIni().get("imap-role");
		assertNotNull(cyrusIp);
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		cyrusIp2 = new BmConfIni().get("imap2-role");
		assertNotNull(cyrusIp2);
		Server imapServer2 = new Server();
		imapServer2.ip = cyrusIp2;
		imapServer2.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer, imapServer2);

		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		dataLocation = serverService.getComplete(cyrusIp);
		dataLocation2 = serverService.getComplete(cyrusIp2);

		new CyrusService(cyrusIp).createPartition(domainUid);
		new CyrusService(cyrusIp).refreshPartitions(Arrays.asList(domainUid));
		new CyrusService(cyrusIp).reload();

		new CyrusService(cyrusIp2).createPartition(domainUid);
		new CyrusService(cyrusIp2).refreshPartitions(Arrays.asList(domainUid));
		new CyrusService(cyrusIp2).reload();

		PopulateHelper.createTestDomain(domainUid, imapServer, imapServer2);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testHooksAreCalled() throws Exception {

		final CountDownLatch cdl = new CountDownLatch(3);
		VertxPlatform.getPlatformManager().deployVerticle("net.bluemind.locator.LocatorVerticle", null, new URL[0], 1,
				null, new Handler<AsyncResult<String>>() {

					@Override
					public void handle(AsyncResult<String> event) {
						System.out.println("Locator, successful: " + event.succeeded());
						cdl.countDown();
					}
				});
		VertxPlatform.getPlatformManager().deployWorkerVerticle(true,
				"net.bluemind.user.hook.internal.UserHooksVerticle", null, new URL[0], 1, null,
				new Handler<AsyncResult<String>>() {

					@Override
					public void handle(AsyncResult<String> event) {
						System.out.println("UserHooks, successful: " + event.succeeded());
						cdl.countDown();
					}
				});
		VertxPlatform.getPlatformManager().deployWorkerVerticle(true,
				"net.bluemind.server.hook.internal.ServerHooksVerticle", null, new URL[0], 1, null,
				new Handler<AsyncResult<String>>() {

					@Override
					public void handle(AsyncResult<String> event) {
						System.out.println("ServerHooks, successful: " + event.succeeded());
						cdl.countDown();
					}
				});
		cdl.await();

		IUser userService = getService();
		String uid = "u" + System.currentTimeMillis();
		User user = defaultUser(uid);
		userService.create(uid, user);

		ItemValue<User> created = userService.getComplete(uid);
		assertNotNull(created);

		System.err.println("**** StoreClient admin0 / " + Token.admin0());
		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			System.err.println("**** check if the mailbox exists: " + user.login + "@" + domainUid);
			boolean exists = sc.isExist("user/" + user.login + "@" + domainUid);
			assertTrue(exists);

			System.err.println("**** StoreClient " + user.login + "@" + domainUid + "/ " + user.password);
		}
		User updated = userService.getComplete(uid).value;
		try (StoreClient sc = new StoreClient(cyrusIp, 1143, user.login + "@" + domainUid, user.password)) {
			assertTrue(sc.login());

			ListResult folders = sc.listAll();
			// +1 == INBOX
			assertEquals(DefaultFolder.USER_FOLDERS_NAME.size() + 1, folders.size());

			updated.login = "updated" + System.currentTimeMillis();

			System.out.println("**** rename !!!!");
			userService.update(uid, updated);
		}
		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			boolean oldBox = sc.isExist("user/" + user.login + "@" + domainUid);
			assertFalse(oldBox);
			boolean newBox = sc.isExist("user/" + updated.login + "@" + domainUid);
			assertTrue("Fail to rename mailbox to " + updated.login + "@" + domainUid, newBox);

		}

		System.out.println("****** Go XFER !");
		updated.dataLocation = dataLocation2.uid;
		userService.update(uid, updated);

		try (StoreClient sc = new StoreClient(cyrusIp2, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
			assertTrue(sc.isExist("user/" + updated.login + "@" + domainUid));
		}

		TaskRef tr = userService.delete(uid);
		TaskStatus status = TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), tr);
		assertEquals(TaskStatus.State.Success, status.state);

	}

	private User defaultUser(String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + domainUid;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.internal;
		VCard card = new VCard();
		card.identification.name = Name.create("Doe", "John", null, null, null, null);
		user.contactInfos = card;
		user.dataLocation = dataLocation.uid;
		return user;
	}

	private IUser getService() throws Exception {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid);
	}

	protected Properties loadTestProps(String propFileName) throws IOException {
		Properties props = new Properties();
		InputStream in = CyrusBackendTests.class.getClassLoader().getResourceAsStream("data/" + propFileName);
		if (in != null) {
			props.load(in);
			in.close();
		}
		return props;
	}
}
