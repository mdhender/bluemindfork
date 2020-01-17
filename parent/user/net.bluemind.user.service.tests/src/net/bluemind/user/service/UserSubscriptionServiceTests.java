/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.user.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.service.DirEntryHandler;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUserSubscription;

public class UserSubscriptionServiceTests {

	private SecurityContext testContext;

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

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		String cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		System.out.println(DirEntryHandler.class);

		PopulateHelper.initGlobalVirt(esServer, imapServer);
		PopulateHelper.addDomain("bm.lan");

		PopulateHelper.addUser("test", "bm.lan");
		testContext = BmTestContext.contextWithSession("testUser", "test", "bm.lan").getSecurityContext();

		PopulateHelper.addUser("test2", "bm.lan");

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IUserSubscription getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IUserSubscription.class, "bm.lan");
	}

	@Test
	public void testUnsubscribe() throws SQLException {
		List<ContainerSubscriptionDescriptor> subs = getService(testContext).listSubscriptions("test", "type");
		assertTrue(subs.isEmpty());

		ContainerStore cs = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM);
		cs.create(Container.create("uid", "type", "osef", "test", "bm.lan"));

		getService(testContext).subscribe("test", Arrays.asList(ContainerSubscription.create("uid", false)));
		subs = getService(testContext).listSubscriptions("test", "type");
		assertEquals(1, subs.size());

		getService(testContext).unsubscribe("test", Arrays.asList("uid"));
		subs = getService(testContext).listSubscriptions("test", "type");
		assertTrue(subs.isEmpty());

		List<String> subscribers = getService(testContext).subscribers("uid");
		assertTrue(subscribers.isEmpty());
	}

	@Test
	public void testSubscribe() throws SQLException {
		List<ContainerSubscriptionDescriptor> subs = getService(testContext).listSubscriptions("test", "type");
		assertTrue(subs.isEmpty());

		ContainerStore cs = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM);
		cs.create(Container.create("uid", "type", "osef", "test", "bm.lan"));

		getService(testContext).subscribe("test", Arrays.asList(ContainerSubscription.create("uid", false)));
		subs = getService(testContext).listSubscriptions("test", "type");
		assertEquals(1, subs.size());

		List<String> subscribers = getService(testContext).subscribers("uid");
		assertEquals(1, subscribers.size());
	}

	@Test
	public void testOfflineSync() throws SQLException {
		String contUid = "cont" + System.nanoTime();
		List<ContainerSubscriptionDescriptor> subs = getService(testContext).listSubscriptions("test", "type");
		assertTrue(subs.isEmpty());

		ContainerStore cs = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);
		cs.create(Container.create(contUid, "type", "osef", "test", "bm.lan"));

		getService(testContext).subscribe("test", Arrays.asList(ContainerSubscription.create(contUid, false)));
		subs = getService(testContext).listSubscriptions("test", "type");
		assertEquals(1, subs.size());
		assertEquals(contUid, subs.get(0).containerUid);
		assertFalse(subs.get(0).offlineSync);

		// update subscription
		System.err.println(getClass() + " setting sub.offline to true on " + contUid + "...");
		getService(testContext).subscribe("test", Arrays.asList(ContainerSubscription.create(contUid, true)));
		System.err.println("after subscribe offline true");
		subs = getService(testContext).listSubscriptions("test", "type");
		assertEquals(1, subs.size());
		assertEquals(contUid, subs.get(0).containerUid);
		assertTrue(subs.get(0).offlineSync);
	}

	@Test
	public void testPreventUnsubscribingFromDefaultContainer() throws SQLException {

		String uid = UUID.randomUUID().toString();
		ContainerStore cs = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);
		cs.create(Container.create(uid, "type", "osef", "test", "bm.lan", true));

		getService(testContext).subscribe("test", Arrays.asList(ContainerSubscription.create(uid, false)));
		List<ContainerSubscriptionDescriptor> subs = getService(testContext).listSubscriptions("test", "type");
		assertEquals(1, subs.size());
		assertEquals(uid, subs.get(0).containerUid);

		getService(testContext).unsubscribe("test", Arrays.asList(uid));
		subs = getService(testContext).listSubscriptions("test", "type");
		assertEquals(1, subs.size());
		assertEquals(uid, subs.get(0).containerUid);
	}

}
