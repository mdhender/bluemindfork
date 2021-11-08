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
package net.bluemind.server.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.node.NodeTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.server.persistence.ServerStore;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ServerDomainHookTests {

	protected SecurityContext defaultSecurityContext;
	protected Container installation;
	private ServerStore serverStore;
	private ItemStore itemStore;
	private String domainUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		defaultSecurityContext = new SecurityContext("testId", "testSubject", Arrays.<String>asList(),
				Arrays.<String>asList(BasicRoles.ROLE_MANAGE_SERVER), "testDomainUid");

		Sessions.get().put("testId", defaultSecurityContext);

		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		PopulateHelper.initGlobalVirt(esServer);
		domainUid = "test.lan";
		PopulateHelper.createTestDomain("test.lan");

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				defaultSecurityContext);

		installation = containerHome.get(InstallationId.getIdentifier());

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), installation, defaultSecurityContext);
		serverStore = new ServerStore(JdbcTestHelper.getInstance().getDataSource(), installation);
		AclStore aclStore = new AclStore(JdbcTestHelper.getInstance().getDataSource());
		aclStore.store(installation,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));
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
	public void testDeletingADomainShouldDeleteAllServerAssignments()
			throws ServerFault, InterruptedException, SQLException {
		Server srv = defaultServer("pre-update" + System.nanoTime() + ".bm.lan", NodeTestHelper.getHost());
		String uid = create(srv);
		assertNotNull(uid);

		getAdmin0Service().assign(uid, domainUid, srv.tags.get(0));
		getAdmin0Service().assign(uid, domainUid, srv.tags.get(1));

		assertEquals(4, getService().getAssignments(domainUid).size());

		IDomains domainService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		CountDownLatch latch = new CountDownLatch(1);
		TaskRef deleteDomainItems = domainService.deleteDomainItems(domainUid);
		trackDeletion(deleteDomainItems, latch);
		latch.await();
		domainService.delete(domainUid);
		assertEquals(0, getService().getAssignments(domainUid).size());
	}

	private void trackDeletion(final TaskRef taskRef, final CountDownLatch latch) throws ServerFault {
		final ITask taskService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class,
				taskRef.id);
		Runnable tracker = () -> {
			while (!taskService.status().state.ended) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}

			latch.countDown();
		};
		new Thread(tracker).start();
	}

	private String create(Server srv) {
		String uid = UUID.randomUUID().toString();

		try {
			getService().create(uid, srv);
			return uid;
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
			return null;
		}
	}

	/**
	 * @param fqdn
	 * @param ip   TODO
	 * @return
	 */
	private Server defaultServer(String fqdn, String ip) {
		Server server = new Server();
		server.fqdn = ip;
		server.ip = ip;
		server.name = fqdn;
		server.tags = Lists.newArrayList("blue/job", "big/bang");
		return server;
	}

	protected IServer getAdmin0Service() throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class, installation.uid);
	}

	protected IServer getService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(defaultSecurityContext).instance(IServer.class, installation.uid);
	}

}
