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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.persistence.DirEntryStore;
import net.bluemind.domain.api.Domain;
import net.bluemind.imap.CreateMailboxResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.persistence.ServerStore;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ServerHookTests {

	private String cyrusIp;
	private BmTestContext context;
	private ItemValue<Server> server;
	private ItemValue<Domain> domain;

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

		// register TEST_TAG host to locator
		cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		// DataLocation server
		Server fakeImapServer = new Server();
		fakeImapServer.ip = "10.0.0.1";
		fakeImapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer, fakeImapServer);

		context = new BmTestContext(SecurityContext.SYSTEM);
		server = context.provider().instance(IServer.class, InstallationId.getIdentifier()).getComplete(imapServer.ip);
		assertNotNull(server);

		String domainUid = "test" + System.nanoTime() + ".fr";
		PopulateHelper.createTestDomain(domainUid, imapServer);
		domain = ItemValue.create(domainUid, Domain.create("domainUid", null, null, null));
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	// disable because i can't log in after that ...
	// @Test
	public void testOnServerTagged() throws Exception {

		hook().onServerTagged(context, server, "mail/imap");

		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());
		}

	}

	@Test
	public void testOnServerAssigned() throws Exception {
		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				context.getSecurityContext());
		ServerStore serverStore = new ServerStore(JdbcTestHelper.getInstance().getDataSource(),
				containerStore.get(InstallationId.getIdentifier()));
		serverStore.assign(server.uid, domain.uid, "mail/imap");

		hook().onServerAssigned(context, server, domain, "mail/imap");
		Thread.sleep(100);
		try (StoreClient sc = new StoreClient(server.value.address(), 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			CreateMailboxResult ok = sc.createMailbox("mbox" + System.currentTimeMillis() + "@" + domain.uid,
					CyrusPartition.forServerAndDomain(server, domain.uid).name);

			assertEquals(true, ok.isOk());
		}
	}

	@Test
	public void testOnServerPreUnassigned_Ok() throws Exception {
		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				context.getSecurityContext());
		ServerStore serverStore = new ServerStore(JdbcTestHelper.getInstance().getDataSource(),
				containerStore.get(InstallationId.getIdentifier()));
		serverStore.assign(server.uid, domain.uid, "mail/imap");

		try {
			hook().onServerPreUnassigned(context, server, domain, "mail/imap");
		} catch (ServerFault sf) {
			fail(sf.getMessage());
		}
	}

	@Test
	public void testOnServerPreUnassigned_Failure() throws Exception {
		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				context.getSecurityContext());
		ServerStore serverStore = new ServerStore(JdbcTestHelper.getInstance().getDataSource(),
				containerStore.get(InstallationId.getIdentifier()));
		serverStore.assign(server.uid, domain.uid, "mail/imap");

		DirEntry de = DirEntry.create(null, "path", DirEntry.Kind.USER, "test" + System.nanoTime(), "test",
				"test@test.com", false, false, false, server.uid);

		Container dom = containerStore.get(domain.uid);
		ItemStore is = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), dom, SecurityContext.SYSTEM);
		is.create(Item.create(de.entryUid, null));
		Item item = is.get(de.entryUid);
		DirEntryStore des = new DirEntryStore(JdbcTestHelper.getInstance().getDataSource(), dom);
		des.create(item, de);

		try {
			hook().onServerPreUnassigned(context, server, domain, "mail/imap");
			fail("could not be able to unassign mail/imap");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.FAILURE, sf.getCode());
		}
	}

	private ServerHook hook() {
		return new ServerHook();
	}

}
