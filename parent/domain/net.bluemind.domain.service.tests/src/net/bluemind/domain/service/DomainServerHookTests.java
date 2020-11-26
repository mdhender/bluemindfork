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
package net.bluemind.domain.service;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.service.internal.DomainServerHook;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.GroupSearchQuery;
import net.bluemind.group.api.IGroup;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DomainServerHookTests {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		ElasticsearchTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		PopulateHelper.initGlobalVirt();
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
	}

	@Test
	public void testUserAndAdminGroupsHaveADatalocation() throws ServerFault, Exception {
		// create domain
		final String domainUid = "bluemind.test.net";
		PopulateHelper.addDomain(domainUid);

		// execute hook (as if a imap server is created)
		final ItemValue<Server> server = new ItemValue<>();
		server.uid = "bm";
		final ItemValue<Domain> domain = new ItemValue<>();
		domain.uid = domainUid;
		new DomainServerHook().onServerAssigned(
				ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext(), server, domain,
				"mail/imap");

		// user and admin groups should have a datalocation
		final String userGroupDatalocation = this.retrieveGroupDatalocation("user", domainUid);
		final String adminGroupDatalocation = this.retrieveGroupDatalocation("admin", domainUid);
		final boolean bothGroupsHaveADatalocation = userGroupDatalocation != null && !userGroupDatalocation.isEmpty()
				&& adminGroupDatalocation != null && !adminGroupDatalocation.isEmpty();
		Assert.assertTrue(String.format(
				"Both 'user' and 'admin' groups should have a datalocation. userGroupDatalocation=%s, adminGroupDatalocation=%s",
				userGroupDatalocation, adminGroupDatalocation), bothGroupsHaveADatalocation);

	}

	private String retrieveGroupDatalocation(final String groupName, final String domainUid) {
		final IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domainUid);
		final GroupSearchQuery groupSearchQuery = GroupSearchQuery.matchProperty("is_profile", "true");
		groupSearchQuery.name = groupName;
		final List<ItemValue<Group>> groupResult = groupService.search(groupSearchQuery);
		Assert.assertEquals(1, groupResult.size());
		return groupResult.get(0).value.dataLocation;
	}

}
