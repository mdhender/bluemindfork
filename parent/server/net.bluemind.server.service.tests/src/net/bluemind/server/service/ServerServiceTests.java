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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.config.InstallationId;
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
import net.bluemind.core.node.NodeTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.CommandStatus;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.persistence.ServerStore;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ServerServiceTests {

	protected SecurityContext serverManagerSecurityContext;
	private SecurityContext domainAdminSecurityContext;

	protected Container installation;
	private ServerStore serverStore;
	private ItemStore itemStore;
	private String domainUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		serverManagerSecurityContext = new SecurityContext("testId", "testSubject", Arrays.<String>asList(),
				Arrays.<String>asList(BasicRoles.ROLE_MANAGE_SERVER), "testDomainUid");
		Sessions.get().put("testId", serverManagerSecurityContext);

		domainAdminSecurityContext = BmTestContext
				.contextWithSession("testId2", "testSubject2", domainUid, BasicRoles.ROLE_ADMIN).getSecurityContext();

		PopulateHelper.initGlobalVirt();
		domainUid = "test.lan";
		PopulateHelper.createTestDomain("test.lan");

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				serverManagerSecurityContext);

		installation = containerHome.get(InstallationId.getIdentifier());

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), installation,
				serverManagerSecurityContext);
		serverStore = new ServerStore(JdbcTestHelper.getInstance().getDataSource(), installation);
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	private void waitForTaskRef(TaskRef taskRef) throws ServerFault {
		ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, taskRef.id);
		while (!task.status().state.ended) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		TaskStatus status = task.status();
		if (status.state == State.InError) {
			throw new ServerFault("tr error");
		}
	}

	@Test
	public void testCreate() throws ServerFault, InterruptedException, SQLException {
		String uid = UUID.randomUUID().toString();
		String fqdn = "server" + uid + ".bm.lan";
		Server srv = defaultServer(fqdn, NodeTestHelper.getHost());
		TaskRef tr = getService(serverManagerSecurityContext).create(uid, srv);
		waitForTaskRef(tr);

		Item item = itemStore.get(uid);
		assertNotNull(item);
		Server found = serverStore.get(item);
		assertNotNull(found);

		// test already exist
		try {
			tr = getService(serverManagerSecurityContext).create(uid, srv);
			waitForTaskRef(tr);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.ALREADY_EXISTS, e.getCode());
		}
	}

	@Test
	public void testCreateInvalidServer() throws ServerFault, InterruptedException, SQLException {
		String uid = UUID.randomUUID().toString();
		String fqdn = "server" + uid + ".bm.lan";
		Server srv = defaultServer(fqdn, "8.8.8.8");
		try {
			// CheckServerAvailability should be called and fail
			getService(serverManagerSecurityContext).create(uid, srv);
			fail("should fail because google doesnt have node installed on its DNS");
		} catch (ServerFault e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testAssignements() throws ServerFault, InterruptedException, SQLException {
		Server srv = defaultServer("pre-update" + System.nanoTime() + ".bm.lan", NodeTestHelper.getHost());
		String uid = create(srv);
		assertNotNull(uid);

		List<Assignment> assignments = getService(serverManagerSecurityContext).getAssignments(domainUid);
		int initialAssignments = assignments.size();

		getAdmin0Service().assign(uid, domainUid, srv.tags.get(0));
		assignments = getService(serverManagerSecurityContext).getAssignments(domainUid);
		assertEquals(1 + initialAssignments, assignments.size());

		getAdmin0Service().unassign(uid, domainUid, srv.tags.get(0));
		assignments = getService(serverManagerSecurityContext).getAssignments(domainUid);
		assertEquals(0 + initialAssignments, assignments.size());

		try {
			getService(domainAdminSecurityContext).assign(uid, domainUid, srv.tags.get(0));
			fail("should not success. You're not admin0");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			getService(domainAdminSecurityContext).unassign(uid, domainUid, srv.tags.get(0));
			fail("should not success. You're not admin0");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			getAdmin0Service().assign(uid, "fakelan", srv.tags.get(0));
			fail("should fail, domain doesnt exists");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}

		try {
			getAdmin0Service().assign("fake server", domainUid, srv.tags.get(0));
			fail("should fail, server doesnt exists");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}

		try {
			getAdmin0Service().unassign("fake server", domainUid, srv.tags.get(0));
			fail("should fail, server doesnt exists");
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testUpdate() throws ServerFault, SQLException {
		Server srv = defaultServer("pre-update" + System.nanoTime() + ".bm.lan", NodeTestHelper.getHost());
		String uid = create(srv);
		assertNotNull(uid);

		srv.fqdn = "post-update" + System.nanoTime() + ".bm.lan";

		TaskRef tr = getService(serverManagerSecurityContext).update(uid, srv);
		waitForTaskRef(tr);

		Item item = itemStore.get(uid);
		assertNotNull(item);
		Server found = serverStore.get(item);
		assertNotNull(found);
		assertEquals(srv.fqdn, found.fqdn);

		try {
			tr = getService(serverManagerSecurityContext).update("fake-id", srv);
			waitForTaskRef(tr);
			fail("should throw excetpion");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}
	}

	@Test
	public void testDelete() throws SQLException, ServerFault {
		Server srv = defaultServer("delete" + System.nanoTime() + ".bm.lan", NodeTestHelper.getHost());
		String uid = create(srv);
		assertNotNull(uid);

		getAdmin0Service().assign(uid, domainUid, srv.tags.get(0));

		try {
			getService(serverManagerSecurityContext).delete(uid);
			fail("should fail because server is assigned");
		} catch (ServerFault e) {

		}

		getAdmin0Service().unassign(uid, domainUid, srv.tags.get(0));

		getService(serverManagerSecurityContext).delete(uid);

		Item item = itemStore.get(uid);
		assertNull(item);

		try {
			getService(serverManagerSecurityContext).delete("fake-id");
			fail("should throw excetpion");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}
	}

	@Test
	public void testGetComplete() throws ServerFault {
		Server srv = defaultServer("get" + System.nanoTime() + ".bm.lan", NodeTestHelper.getHost());
		String uid = create(srv);
		assertNotNull(uid);

		ItemValue<Server> srvItem = getService(serverManagerSecurityContext).getComplete(uid);
		assertNotNull(srvItem);
		assertEquals(uid, srvItem.uid);
		assertNotNull(srvItem.value);
		srv = srvItem.value;
		assertNotNull(srv.fqdn);
		assertNotNull(srv.tags);

		srvItem = getService(serverManagerSecurityContext).getComplete("nonExistant");
		assertNull(srvItem);
	}

	@Test
	public void testExecute() throws ServerFault {
		IServer service = getService(serverManagerSecurityContext);
		String host = NodeTestHelper.getHost();
		assertNotNull(host);
		Server srv = defaultServer(host, host);
		service.create(host, srv);
		String ref = service.submit(host, "ls /");
		System.out.println("COMMAND REF: '" + ref + "'");
		StringBuilder sb = new StringBuilder();
		CommandStatus status = null;
		do {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}
			status = service.getStatus(host, ref);
			for (String s : status.output) {
				sb.append(s).append('\n');
			}
		} while (!status.complete);
		assertTrue(status.successful);
		System.err.println(sb.toString());
		assertTrue(sb.toString().contains("sbin"));
		service.delete(host);
	}

	@Test
	public void testSubmitAndWait() throws ServerFault {
		IServer service = getService(serverManagerSecurityContext);
		String host = NodeTestHelper.getHost();
		assertNotNull(host);
		Server srv = defaultServer(host, host);
		service.create(host, srv);
		CommandStatus status = service.submitAndWait(host, "/bin/sleep 1");
		assertTrue(status.successful);
		service.delete(host);
	}

	@Test
	public void testWriteRead() throws ServerFault {
		IServer service = getService(serverManagerSecurityContext);
		String host = NodeTestHelper.getHost();
		assertNotNull(host);
		Server srv = defaultServer(host, host);
		service.create(host, srv);

		String value = "€uro symbolü";
		String fn = "/tmp/file." + System.nanoTime() + ".txt";
		service.writeFile(host, fn, value.getBytes());
		String reRead = new String(service.readFile(host, fn));
		assertEquals(value, reRead);

		service.delete(host);
	}

	@Test
	public void testGetServerAssignments() throws Exception {
		Server srv = defaultServer("testGetServerAssignments" + System.nanoTime() + ".bm.lan",
				NodeTestHelper.getHost());
		String uid = create(srv);

		PopulateHelper.createTestDomain("test2.lan");

		List<Assignment> assignments = getService(serverManagerSecurityContext).getServerAssignments(uid);
		assertEquals(0, assignments.size());

		getAdmin0Service().assign(uid, domainUid, srv.tags.get(0));
		getAdmin0Service().assign(uid, domainUid, srv.tags.get(1));
		getAdmin0Service().assign(uid, "test2.lan", srv.tags.get(1));

		assignments = getService(serverManagerSecurityContext).getServerAssignments(uid);
		assertEquals(3, assignments.size());

	}

	@Test
	public void testFactory() {
		try {
			new ServerServiceFactory().getService(new BmTestContext(SecurityContext.SYSTEM), "fake");
			fail("should throw notFound");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}

		// container exists but it's not "installation" kind
		try {
			new ServerServiceFactory().getService(new BmTestContext(SecurityContext.SYSTEM), domainUid);
			fail("should throw notFound");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}
	}

	@Test
	public void testCreateEmptyIp() {
		String uid = UUID.randomUUID().toString();

		Server server = new Server();
		server.fqdn = NodeTestHelper.getHost();
		server.ip = "";
		server.name = uid;

		try {
			TaskRef tr = getService(serverManagerSecurityContext).create(uid, server);
			waitForTaskRef(tr);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testCreateEmptyFqdn() {
		String uid = UUID.randomUUID().toString();

		Server server = new Server();
		server.fqdn = "";
		server.ip = NodeTestHelper.getHost();
		server.name = uid;

		TaskRef tr = getService(serverManagerSecurityContext).create(uid, server);
		waitForTaskRef(tr);

		ItemValue<Server> srv = getService(serverManagerSecurityContext).getComplete(uid);
		assertNull(srv.value.fqdn);
	}

	private String create(Server srv) {
		String uid = UUID.randomUUID().toString();

		try {
			TaskRef tr = getService(serverManagerSecurityContext).create(uid, srv);
			waitForTaskRef(tr);
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

	protected IServer getService(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(IServer.class, installation.uid);
	}

}
