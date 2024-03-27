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
package net.bluemind.core.container.hierarchy.hook.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;

public class UserHierarchyTests {

	private String domainUid;

	@Before
	public void before() throws Exception {
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);

		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		BmConfIni ini = new BmConfIni();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		Server pipo = new Server();
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		PopulateHelper.initGlobalVirt(esServer, pipo);
		System.err.println("Deploying with es: " + esServer.ip + ", imap: " + PopulateHelper.FAKE_CYRUS_IP);

		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);
		ElasticsearchTestHelper.getInstance().beforeTest();
		domainUid = "pipo" + System.currentTimeMillis() + ".io";
		PopulateHelper.addDomain(domainUid, Routing.none);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@After
	public void after() throws Exception {
		System.err.println("After test");
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreateUserPopulatesHierarchy() throws Exception {
		String userUid = PopulateHelper.addUser("test", domainUid, Routing.internal);
		System.err.println("After populateUser ends.");
		Thread.sleep(1000);
		IContainersFlatHierarchy hierarchyService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainersFlatHierarchy.class, domainUid, "test");
		assertNotNull(hierarchyService);
		List<ItemValue<ContainerHierarchyNode>> nodes = hierarchyService.list();
		assertFalse(nodes.isEmpty());
		Set<Long> hierIds = new HashSet<>();
		for (ItemValue<ContainerHierarchyNode> iv : nodes) {
			System.err.println(" * " + iv);
			hierIds.add(iv.internalId);
		}
		IUser userApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid);
		System.err.println("*** pre-delete");
		TaskRef tr = userApi.delete(userUid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), tr);

		Thread.sleep(1000);
		System.err.println("*** post-delete");
		userUid = PopulateHelper.addUser("test", domainUid, Routing.internal);
		System.err.println("**** pre re-recreate sleep");
		Thread.sleep(2000);
		System.err.println("**** post re-recreate");
		hierarchyService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainersFlatHierarchy.class, domainUid, "test");
		nodes = hierarchyService.list();
		assertFalse(nodes.isEmpty());
		for (ItemValue<ContainerHierarchyNode> iv : nodes) {
			System.err.println(" * v2: " + iv);
			assertFalse(hierIds.contains(iv.internalId));
		}
	}

}
