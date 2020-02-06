/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.inject.imap.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.common.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusReplicationHelper;
import net.bluemind.backend.cyrus.replication.testhelper.SyncServerHelper;
import net.bluemind.cli.inject.imap.ImapInjector;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.locator.LocatorVerticle;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.vertx.testhelper.Deploy;

public class ImapInjectorTests {

	@BeforeClass
	public static void oneShotBefore() {
		System.setProperty("es.mailspool.count", "1");
	}

	private String domainUid;
	private CyrusReplicationHelper cyrusReplication;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		Deploy.verticles(false, LocatorVerticle::new).get(5, TimeUnit.SECONDS);

		BmConfIni ini = new BmConfIni();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		Server imapServer = new Server();
		imapServer.ip = ini.get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		ItemValue<Server> cyrusServer = ItemValue.create("localhost", imapServer);
		CyrusService cyrusService = new CyrusService(cyrusServer);
		cyrusService.reset();

		PopulateHelper.initGlobalVirt(imapServer, esServer);
		ElasticsearchTestHelper.getInstance().beforeTest();
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		this.domainUid = "test" + System.currentTimeMillis() + ".lab";

		PopulateHelper.addDomain(domainUid, Routing.none);

		System.err.println("Setup replication START");
		this.cyrusReplication = new CyrusReplicationHelper(imapServer.ip);
		cyrusReplication.installReplication();
		System.err.println("Setup replication END");

		JdbcActivator.getInstance().addMailboxDataSource(cyrusReplication.server().uid,
				JdbcTestHelper.getInstance().getMailboxDataDataSource());

		CountDownLatch cdl = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(ar -> {
			cdl.countDown();
		});
		boolean beforeTimeout = cdl.await(30, TimeUnit.SECONDS);
		assertTrue(beforeTimeout);

		MQ.init().get(30, TimeUnit.SECONDS);

		SyncServerHelper.waitFor();

		cyrusReplication.startReplication().get(5, TimeUnit.SECONDS);

		for (int i = 0; i < 10; i++) {
			String uid = "user" + Strings.padStart(Integer.toString(i), 3, '0');
			PopulateHelper.addUser(uid, domainUid, Routing.internal);
		}

	}

	@After
	public void after() throws Exception {
		cyrusReplication.stopReplication().get(5, TimeUnit.SECONDS);
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testInjector() throws InterruptedException {
		System.err.println("The test is here....");
		ImapInjector inject = new ImapInjector(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM),
				domainUid);
		ApplyCounter.reset();
		assertNotNull(inject);
		int MSG = 5000;
		int MAX_STALLS = 500;
		Runnable r = () -> inject.runCycle(MSG);
		Thread injection = new Thread(r, "injector-thread");
		injection.start();
		int stalled = 0;
		long time = System.currentTimeMillis();
		do {
			int cur = ApplyCounter.total();
			Thread.sleep(5000);
			int afterSleep = ApplyCounter.total();
			System.err.println("Applied " + afterSleep + " message(s), stalls: " + stalled + " after "
					+ (System.currentTimeMillis() - time) + "ms (alive: " + injection.isAlive() + ").");
			if (cur == afterSleep) {
				stalled++;
			}
		} while (ApplyCounter.total() < MSG && stalled < MAX_STALLS);
		Thread.sleep(2000);
		System.err.println("Waiting for injection end...");
		injection.join(60000);
		if (stalled >= MAX_STALLS) {
			throw new RuntimeException("Test stalled");
		}
	}

}
