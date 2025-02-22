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
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.service.DirEntryHandler;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IInternalUserSubscription;
import net.bluemind.user.api.IUserSubscription;

public class UserSubscriptionServiceTests {

	private SecurityContext testContext;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		Server pipo = new Server();
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		System.out.println(DirEntryHandler.class);

		PopulateHelper.initGlobalVirt(esServer, pipo);
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

	protected IInternalUserSubscription getInternalService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IInternalUserSubscription.class, "bm.lan");
	}

	@Test
	public void testUnsubscribe() throws SQLException {
		List<ContainerSubscriptionDescriptor> subs = getService(testContext).listSubscriptions("test", "type");
		assertTrue(subs.isEmpty());

		ContainerStore cs = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);
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

		ContainerStore cs = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);
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

	@Test
	public void testSubscribeNullContainer() {
		try {
			getService(testContext).subscribe("test", Arrays.asList(ContainerSubscription.create("not-here", false)));
			fail();
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.NOT_FOUND, sf.getCode());
		}
	}

	@Test
	public void testUnsubscribeNullContainer() {
		try {
			getService(testContext).unsubscribe("test", Arrays.asList("not-here"));
		} catch (ServerFault sf) {
			fail();
		}
	}

	@Test
	public void testSubscribeWithContainerDescriptor() throws SQLException {
		List<String> subscribers = getService(testContext).subscribers("uid");
		assertTrue(subscribers.isEmpty());

		ContainerDescriptor descriptor = ContainerDescriptor.create("uid", "osef", "test", "type", "bm.lan", false);
		descriptor.offlineSync = false;
		getInternalService(testContext).subscribe("test", descriptor);

		subscribers = getService(testContext).subscribers("uid");
		assertEquals(1, subscribers.size());
		assertEquals("test", subscribers.get(0));

		List<ContainerSubscriptionDescriptor> subs = getService(testContext).listSubscriptions("test", "type");
		assertEquals(0, subs.size());
	}

	@Test
	public void testUnsubscribeWithContainerDescriptor() throws SQLException {
		List<ContainerSubscriptionDescriptor> subs = getService(testContext).listSubscriptions("test", "type");
		assertTrue(subs.isEmpty());

		ContainerStore cs = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);
		Container container = Container.create("uid", "type", "osef", "test", "bm.lan");
		cs.create(container);
		getService(testContext).subscribe("test", Arrays.asList(ContainerSubscription.create("uid", false)));

		subs = getService(testContext).listSubscriptions("test", "type");
		assertEquals(1, subs.size());
		List<String> subscribers = getService(testContext).subscribers("uid");
		assertEquals(1, subscribers.size());

		ContainerDescriptor descriptor = ContainerDescriptor.create(container.uid, container.name, container.owner,
				container.type, container.domainUid, container.defaultContainer);
		getInternalService(testContext).unsubscribe("test", descriptor);

		subs = getService(testContext).listSubscriptions("test", "type");
		assertEquals(1, subs.size());
		subscribers = getService(testContext).subscribers("uid");
		assertTrue(subscribers.isEmpty());

	}

}
