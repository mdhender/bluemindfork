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
package net.bluemind.group.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.group.service.internal.ContainerGroupStoreService;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class GroupHookTests {

	private Container domainContainer;
	private String domainUid;
	private ItemValue<Server> dataLocation;
	private ItemValue<Domain> domain;

	@Before
	public void before() throws Exception {
		domainUid = "bm.lan";

		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		initDomain(domainUid);

		domainContainer = containerHome.get(domainUid);

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();
	}

	private void initDomain(String domainUid) throws Exception {

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		String cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, imapServer);

		domain = PopulateHelper.createTestDomain(domainUid, esServer, imapServer);

		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		dataLocation = serverService.getComplete(cyrusIp);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testHooksAreCalled() throws Exception {
		IGroup group = getService();

		String uid = UUID.randomUUID().toString();
		group.create(uid, defaultGroup(null));
		group.update(uid, defaultGroup(null));

		List<Member> members = getGroupsMembers();
		group.add(uid, members);
		group.remove(uid, members);

		TaskRef tr = group.delete(uid);
		TaskStatus status = TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), tr);
		assertEquals(TaskStatus.State.Success, status.state);

		assertTrue(TestHook.latch.await(15, TimeUnit.SECONDS));
	}

	private Group defaultGroup(String prefix) {
		Group group = new Group();
		group.dataLocation = dataLocation.uid;
		if (prefix == null || prefix.isEmpty()) {
			prefix = "group";
		}
		group.name = prefix + "-" + System.nanoTime();
		group.description = "Test group";

		Email e = new Email();
		e.address = group.name + "@test.foo";
		e.allAliases = true;
		e.isDefault = true;
		group.emails = new ArrayList<Email>(1);
		group.emails.add(e);

		return group;
	}

	private IGroup getService() throws Exception {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class, domainUid);
	}

	private List<Member> getGroupsMembers() throws ServerFault, SQLException {
		ArrayList<Member> members = new ArrayList<Member>(1);

		ItemValue<Group> group = createGroup();

		Member member = new Member();
		member.type = Member.Type.group;
		member.uid = group.uid;
		members.add(member);

		return members;
	}

	private ItemValue<Group> createGroup() throws ServerFault, SQLException {
		String uid = UUID.randomUUID().toString();
		Group group = defaultGroup(null);

		ContainerGroupStoreService groupStoreService = new ContainerGroupStoreService(
				ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext(),
				JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM, domainContainer, domain);
		groupStoreService.create(uid, group.name, group);

		ItemValue<DirEntryAndValue<Group>> itemValue = groupStoreService.get(uid, null);
		return ItemValue.create(itemValue, itemValue.value.value);
	}
}
