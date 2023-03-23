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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.elasticsearch.common.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;

import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.cli.inject.imap.ImapInjector;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.sds.sync.api.SdsSyncEvent;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ImapInjectorTests {

	private String domainUid;

	@Before
	public void before() throws Exception {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP + "," + PopulateHelper.FAKE_CYRUS_IP_2);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP + "," + PopulateHelper.FAKE_CYRUS_IP_2);
		System.setProperty("ahcnode.fail.https.ok", "true");

		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(pipo, esServer);
		ElasticsearchTestHelper.getInstance().beforeTest();
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		this.domainUid = "test" + System.currentTimeMillis() + ".lab";
		PopulateHelper.addDomain(domainUid, Routing.none);

		JdbcActivator.getInstance().addMailboxDataSource("dataloc",
				JdbcTestHelper.getInstance().getMailboxDataDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		MQ.init().get(30, TimeUnit.SECONDS);

		for (int i = 0; i < 10; i++) {
			String uid = "user" + Strings.padStart(Integer.toString(i), 3, '0');
			PopulateHelper.addUser(uid, domainUid, Routing.internal);
		}
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailboxes mailboxesApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, domainUid);

		mailboxesApi.list().stream().filter(m -> m.value.type.equals(Mailbox.Type.user)).forEach(m -> {
			IDbReplicatedMailboxes apiMailbox = prov.instance(IDbReplicatedMailboxes.class, domainUid.replace(".", "_"),
					"user." + m.value.name.replace(".", "^"));
			long count = apiMailbox.all().stream().count();
			System.err.println(count);
		});
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testInjector() throws InterruptedException {
		System.err.println("The test is here....");

		Server local = new Server();
		local.ip = "127.0.0.1";
		ItemValue<Server> srv = new ItemValue<>();
		srv.value = local;
		ImapInjector inject = new ImapInjector(srv, ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM),
				domainUid);

		assertNotNull(inject);
		int MSG = 500;
		int MAX_STALLS = 50;

		var eventBus = VertxPlatform.eventBus();
		AtomicInteger at = new AtomicInteger();
		eventBus.consumer(SdsSyncEvent.BODYADD.busName(), m -> {
			System.err.println("Messages read from " + SdsSyncEvent.BODYADD.busName() + ": " + at.addAndGet(1));
		});

		RateLimiter rpm = RateLimiter.create(5000.0);
		Runnable r = () -> inject.runCycle(rpm, MSG);
		Thread injection = new Thread(r, "injector-thread");
		injection.start();
		int stalled = 0;
		long time = System.currentTimeMillis();

		do {
			int cur = at.get();
			Thread.sleep(5000);
			int afterSleep = at.get();
			System.err.println("Applied " + afterSleep + " message(s), stalls: " + stalled + " after "
					+ (System.currentTimeMillis() - time) + "ms (alive: " + injection.isAlive() + ").");
			if (cur == afterSleep) {
				stalled++;
			}
		} while (at.get() < MSG && stalled < MAX_STALLS);
		Thread.sleep(2000);
		System.err.println("Waiting for injection end...");
		injection.join(60000);
		if (stalled >= MAX_STALLS) {
			throw new RuntimeException("Test stalled");
		}
	}

}
