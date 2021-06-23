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
package net.bluemind.core.container.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ContainersTests {
	private ContainerStore containerStore;
	private AclStore aclStore;
	private SecurityContext domainAdminSecurityContext;
	private SecurityContext user;
	private SecurityContext admin0SecurityContext;
	private String domainUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		PopulateHelper.initGlobalVirt();
		domainUid = "bmtest.lan";
		PopulateHelper.addDomain(domainUid);
		PopulateHelper.addUser("test", domainUid);

		admin0SecurityContext = new SecurityContext(Token.admin0(), "admin0", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_SYSTEM), "global.virt");
		user = new SecurityContext("testSessionId", "test", Arrays.<String>asList(), Arrays.<String>asList(),
				domainUid);

		domainAdminSecurityContext = new SecurityContext("testSessionId2", "admin", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_ADMIN), domainUid);

		Sessions.get().put(admin0SecurityContext.getSessionId(), admin0SecurityContext);
		Sessions.get().put(user.getSessionId(), user);
		Sessions.get().put(domainAdminSecurityContext.getSessionId(), domainAdminSecurityContext);

		containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), admin0SecurityContext);

		aclStore = new AclStore(JdbcTestHelper.getInstance().getDataSource());
		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreate() throws Exception {
		IContainers containers = getService(admin0SecurityContext);
		String uid = "junit-" + System.nanoTime();
		ContainerDescriptor descriptor = ContainerDescriptor.create(uid, "test", "test", "test", null, false);
		containers.create(uid, descriptor);
		Container container = containerStore.get(uid);
		assertNotNull(container);
		assertFalse(container.defaultContainer);
	}

	@Test
	public void testCreateDomainAdmin() throws Exception {
		IContainers containers = getService(domainAdminSecurityContext);
		String uid = "junit-" + System.nanoTime();
		ContainerDescriptor descriptor = ContainerDescriptor.create(uid, "test", "test", "test",
				domainAdminSecurityContext.getContainerUid(), false);
		try {
			containers.create(uid, descriptor);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		uid = "junit-" + System.nanoTime();
		descriptor = ContainerDescriptor.create(uid, "test", "test", "test", null, false);
		try {
			containers.create(uid, descriptor);
			fail("should fail");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.FORBIDDEN, e.getCode());
		}

	}

	@Test
	public void testCreateSimpleUser() throws Exception {
		IContainers containers = getService(user);
		String uid = "junit-" + System.nanoTime();
		ContainerDescriptor descriptor = ContainerDescriptor.create(uid, "test", user.getSubject(), "test",
				user.getContainerUid(), false);
		try {
			containers.create(uid, descriptor);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		uid = "junit-" + System.nanoTime();
		descriptor = ContainerDescriptor.create(uid, "test", "notMe", "test", user.getContainerUid(), false);
		try {
			containers.create(uid, descriptor);
			fail("should fail");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.FORBIDDEN, e.getCode());
		}

	}

	@Test
	public void testCreateAndDelete() throws Exception {
		IContainers containers = getService(admin0SecurityContext);
		String uid = "junit-" + System.nanoTime();
		ContainerDescriptor descriptor = ContainerDescriptor.create(uid, "test", "test", "test", null, false);
		containers.create(uid, descriptor);
		Container container = containerStore.get(uid);
		assertNotNull(container);
		assertFalse(container.defaultContainer);

		Map<String, String> settings = new HashMap<String, String>();
		settings.put("wat", "wat");
		getContainerManagementService(admin0SecurityContext, uid).setPersonalSettings(settings);

		containers.delete(uid);
		container = containerStore.get(uid);
		assertNull(container);
	}

	@Test
	public void testDeletingNonExistingContainerShouldNotFail() throws Exception {
		IContainers containers = getService(admin0SecurityContext);
		try {
			containers.delete("idontexist");
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void testCreateDefault() throws Exception {
		IContainers containers = getService(admin0SecurityContext);
		String uid = "junit-" + System.nanoTime();
		ContainerDescriptor descriptor = ContainerDescriptor.create(uid, "test", "test", "test", null, true);
		containers.create(uid, descriptor);
		ContainerDescriptor container = containers.get(uid);
		assertNotNull(container);
		assertTrue(container.defaultContainer);
		assertNull(container.datalocation);
	}

	@Test
	public void testCreateContainerShard() {
		IContainers containers = getService(user);
		String uid = "testCreateContainerShard-" + System.nanoTime();
		ContainerDescriptor descriptor = ContainerDescriptor.create(uid, "testCreateContainerShard", user.getSubject(),
				"calendar", domainUid, true);
		containers.create(uid, descriptor);
		ContainerDescriptor container = containers.get(uid);
		assertNotNull(container);
		assertTrue(container.defaultContainer);

		assertNotNull(container.datalocation);
	}

	@Test
	public void testAll() throws Exception {
		String uid1 = "junit-" + System.nanoTime();
		String uid2 = "junit-" + System.nanoTime();

		Container container1 = containerStore.create(Container.create(uid1, "testType", "container 1", "test", true));
		aclStore.store(container1, Arrays.asList(AccessControlEntry.create(user.getSubject(), Verb.All)));
		Container container2 = containerStore.create(Container.create(uid2, "test2Type", uid2, "test", true));
		aclStore.store(container2, Arrays.asList(AccessControlEntry.create(user.getSubject(), Verb.All)));

		ContainerQuery query = new ContainerQuery();
		query.type = "testType";

		List<ContainerDescriptor> result = getService(user).all(query);

		assertEquals(1, result.size());

		query.type = "test2Type";
		result = getService(user).all(query);

		assertEquals(1, result.size());

		query.type = "testType";
		query.name = "container 1";
		result = getService(user).all(query);
		assertEquals(1, result.size());

		query.type = "XXX";
		query.name = "container 1";
		result = getService(user).all(query);
		assertEquals(0, result.size());
	}

	@Test
	public void testMGet() throws Exception {
		String uid1 = "junit-" + System.nanoTime();
		String uid2 = "junit-" + System.nanoTime();

		Container container1 = containerStore.create(Container.create(uid1, "testType", "container 1", "test", true));
		aclStore.store(container1,
				Arrays.asList(AccessControlEntry.create(admin0SecurityContext.getSubject(), Verb.All)));
		Container container2 = containerStore.create(Container.create(uid2, "test2Type", uid2, "test", true));
		aclStore.store(container2,
				Arrays.asList(AccessControlEntry.create(admin0SecurityContext.getSubject(), Verb.All)));

		List<ContainerDescriptor> result = getService(admin0SecurityContext).getContainers(Arrays.asList(uid1, uid2));

		assertEquals(2, result.size());

	}

	@Test
	public void testMGet_ContainerDoesNotExist() throws Exception {
		String uid1 = "junit-" + System.nanoTime();
		String uid2 = "junit-" + System.nanoTime();

		Container container1 = containerStore.create(Container.create(uid1, "testType", "container 1", "test", true));
		aclStore.store(container1,
				Arrays.asList(AccessControlEntry.create(admin0SecurityContext.getSubject(), Verb.All)));
		Container container2 = containerStore.create(Container.create(uid2, "test2Type", uid2, "test", true));
		aclStore.store(container2,
				Arrays.asList(AccessControlEntry.create(admin0SecurityContext.getSubject(), Verb.All)));

		List<ContainerDescriptor> result = getService(admin0SecurityContext)
				.getContainers(Arrays.asList(uid1, uid2, "bang"));

		assertEquals(2, result.size());

	}

	@Test
	public void testMGetLight() throws Exception {
		String uid1 = "junit-" + System.nanoTime();
		String uid2 = "junit-" + System.nanoTime();

		Container container1 = containerStore.create(Container.create(uid1, "testType", "container 1", "test", true));
		aclStore.store(container1,
				Arrays.asList(AccessControlEntry.create(admin0SecurityContext.getSubject(), Verb.All)));
		Container container2 = containerStore.create(Container.create(uid2, "test2Type", uid2, "test", true));
		aclStore.store(container2,
				Arrays.asList(AccessControlEntry.create(admin0SecurityContext.getSubject(), Verb.All)));

		List<BaseContainerDescriptor> result = getService(admin0SecurityContext)
				.getContainersLight(Arrays.asList(uid1, uid2));

		assertEquals(2, result.size());

	}

	@Test
	public void testMGetLight_ContainerDoesNotExist() throws Exception {
		String uid1 = "junit-" + System.nanoTime();
		String uid2 = "junit-" + System.nanoTime();

		Container container1 = containerStore.create(Container.create(uid1, "testType", "container 1", "test", true));
		aclStore.store(container1,
				Arrays.asList(AccessControlEntry.create(admin0SecurityContext.getSubject(), Verb.All)));
		Container container2 = containerStore.create(Container.create(uid2, "test2Type", uid2, "test", true));
		aclStore.store(container2,
				Arrays.asList(AccessControlEntry.create(admin0SecurityContext.getSubject(), Verb.All)));

		List<BaseContainerDescriptor> result = getService(admin0SecurityContext)
				.getContainersLight(Arrays.asList(uid1, uid2, "bang"));

		assertEquals(2, result.size());

	}

	@Test
	public void testGetForUser() throws Exception {
		// Create user matching user security context
		String uid = "UserContainer";

		Container container = containerStore
				.create(Container.create(uid, "testType", "container 1", "admin", domainUid, true));
		aclStore.store(container,
				Arrays.asList(AccessControlEntry.create(admin0SecurityContext.getSubject(), Verb.All)));
		aclStore.store(container, Arrays.asList(AccessControlEntry.create(user.getSubject(), Verb.Read)));

		ContainerDescriptor result = getService(admin0SecurityContext).getForUser(domainUid, user.getSubject(), uid);

		assertNotNull(result);
		assertTrue(result.verbs.stream().noneMatch(v -> v.can(Verb.Write)));
		assertTrue(result.verbs.stream().anyMatch(v -> v.can(Verb.Read)));
	}

	@Test
	public void testDeleteInexistant() throws ServerFault, SQLException {
		try {
			getService(admin0SecurityContext).delete("fakeUid");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		} catch (Exception e) {
			e.printStackTrace();
			fail("should not happen");
		}
	}

	protected IContainers getService(SecurityContext securityContext) throws ServerFault {
		return new Containers(new BmTestContext(securityContext));
	}

	protected IContainerManagement getContainerManagementService(SecurityContext securityContext, String containerUid)
			throws ServerFault {
		return ServerSideServiceProvider.getProvider(securityContext).instance(IContainerManagement.class,
				containerUid);
	}
}
