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
package net.bluemind.directory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.OrgUnitQuery;
import net.bluemind.directory.service.internal.OrgUnitContainerStoreService;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class OrgUnitsTests {

	private String domainUid;
	private SecurityContext domainAdminSC;
	private SecurityContext userSC;
	private OrgUnitContainerStoreService storeService;
	private String testUserUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		PopulateHelper.initGlobalVirt();

		domainUid = "test" + System.currentTimeMillis() + ".lan";
		ItemValue<Domain> domain = PopulateHelper.createTestDomain(domainUid);
		testUserUid = PopulateHelper.addUser("testuser", domain.uid);
		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		domainAdminSC = BmTestContext.contextWithSession("d1", "admin", domainUid, SecurityContext.ROLE_ADMIN)
				.getSecurityContext();

		userSC = BmTestContext.contextWithSession("u1", "u1", domainUid).getSecurityContext();

		storeService = new OrgUnitContainerStoreService(new BmTestContext(SecurityContext.SYSTEM),
				new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM)
						.get(domainUid),
				domain);
	}

	public IOrgUnits orgUnits(SecurityContext sec) {
		return ServerSideServiceProvider.getProvider(sec).instance(IOrgUnits.class, domainUid);
	}

	@Test
	public void testCreate() {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkThat éèà ()";
		orgUnits(domainAdminSC).create("ouTest", ou);

		try {
			ou.name = "checkThat2";
			orgUnits(userSC).create("ouTest4", ou);
			fail("should fail");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testCreate_invalidChar() {
		OrgUnit ou = new OrgUnit();
		ou.name = "/invalid";
		try {
			orgUnits(domainAdminSC).create("ouTest é ()", ou);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		ou = new OrgUnit();
		ou.name = "inva/lid";
		try {
			orgUnits(domainAdminSC).create("ouTest é ()", ou);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		ou = new OrgUnit();
		ou.name = "invalid/";
		try {
			orgUnits(domainAdminSC).create("ouTest é ()", ou);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void testCreateWithSamePath() {
		OrgUnit master = new OrgUnit();
		master.name = "MasterOfPuppets";
		orgUnits(domainAdminSC).create("master", master);

		OrgUnit chorus = new OrgUnit();
		chorus.name = "MasterOfPuppets";
		chorus.parentUid = "master";
		orgUnits(domainAdminSC).create("of", chorus);

		try {
			OrgUnit bis = new OrgUnit();
			bis.name = "MasterOfPuppets";
			bis.parentUid = "master";
			orgUnits(domainAdminSC).create("puppets", bis);
			fail("It should not be possible to create 2 OU with same parent and name");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.ALREADY_EXISTS, e.getCode());
		}
	}

	@Test
	public void testCreateWithNullParentAndSamePath() {
		OrgUnit master = new OrgUnit();
		master.name = "MasterOfPuppets";
		orgUnits(domainAdminSC).create("master", master);

		try {
			OrgUnit chorus = new OrgUnit();
			chorus.name = "MasterOfPuppets";
			orgUnits(domainAdminSC).create("of", chorus);
			fail("It should not be possible to create 2 OU with no parent and same name");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.ALREADY_EXISTS, e.getCode());
		}
	}

	@Test
	public void testCreateWithParent() {
		OrgUnit parent = new OrgUnit();
		parent.name = "parent";
		orgUnits(domainAdminSC).create("parent", parent);

		OrgUnit child = new OrgUnit();
		child.name = "child";
		child.parentUid = "parent";
		orgUnits(domainAdminSC).create("child", child);
	}

	@Test
	public void testUpdate() {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkThat";
		storeService.create("ouTest", ou);

		ou.name = "uCheckThat";
		orgUnits(domainAdminSC).update("ouTest", ou);

		try {
			orgUnits(userSC).update("ouTest", ou);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testUpdateInexistant() {
		OrgUnit ou = new OrgUnit();
		ou.name = "uCheckThat";
		try {
			orgUnits(domainAdminSC).update("ouTest", ou);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}
	}

	@Test
	public void testUpdateWithSamePath() {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkThat";
		storeService.create("father", ou);

		OrgUnit son = new OrgUnit();
		son.name = "checkThis";
		son.parentUid = "father";
		storeService.create("son", son);

		OrgUnit daughter = new OrgUnit();
		daughter.name = "checkThat";
		storeService.create("daughter", daughter);

		daughter.name = "checkThis";
		daughter.parentUid = "father";
		try {
			orgUnits(domainAdminSC).update("daughter", daughter);
			fail("It should not be possible to update OU with same parent and name as another one");
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testUpdateWithParent() {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkThat";
		storeService.create("ouTest", ou);

		// another possible parent for testing purpose
		ou = new OrgUnit();
		ou.name = "checkThat2";
		storeService.create("ouTest2", ou);

		OrgUnit child = new OrgUnit();
		child.name = "child";
		child.parentUid = "ouTest";
		storeService.create("child", child);

		child.name = "uchild";
		orgUnits(domainAdminSC).update("child", child);

		try {
			orgUnits(userSC).update("child", ou);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		child.name = "uchild";
		child.parentUid = "ouTest2";

		try {
			orgUnits(domainAdminSC).update("child", child);
			fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testGetChildren() {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkThat";
		storeService.create("ouTest", ou);

		OrgUnit ou2 = new OrgUnit();
		ou2.name = "parent2";
		storeService.create("parent2", ou2);

		OrgUnit child = new OrgUnit();
		child.name = "child1";
		child.parentUid = "ouTest";
		storeService.create("child1", child);

		child = new OrgUnit();
		child.name = "child2";
		child.parentUid = "ouTest";
		storeService.create("child2", child);

		child = new OrgUnit();
		child.name = "child3";
		child.parentUid = "parent2";
		storeService.create("child3", child);

		List<ItemValue<OrgUnit>> children1 = orgUnits(domainAdminSC).getChildren("ouTest");
		List<ItemValue<OrgUnit>> children2 = orgUnits(domainAdminSC).getChildren("parent2");

		assertEquals(2, children1.size());
		assertEquals(1, children2.size());

		int countOk = 0;
		for (ItemValue<OrgUnit> o : children1) {
			if (o.uid.equals("child1") || o.uid.equals("child2")) {
				assertEquals(o.uid, o.value.name);
				countOk++;
			}
		}
		assertEquals(2, countOk);

		countOk = 0;
		for (ItemValue<OrgUnit> o : children2) {
			if (o.uid.equals("child3")) {
				assertEquals(o.uid, o.value.name);
				countOk++;
			}
		}
		assertEquals(1, countOk);

	}

	@Test
	public void testDelete() {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkThat";
		storeService.create("ouTest", ou);

		orgUnits(domainAdminSC).delete("ouTest");

		storeService.create("ouTest", ou);
		try {
			orgUnits(userSC).delete("ouTest");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testDeleteWithChild() {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkThat";
		storeService.create("ouTest", ou);
		OrgUnit child = new OrgUnit();
		child.name = "child";
		child.parentUid = "ouTest";
		storeService.create("child", child);

		try {
			orgUnits(domainAdminSC).delete("ouTest");
			fail();
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testSetAdministratorRoles() {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkThat";
		storeService.create("ouTest", ou);
		orgUnits(domainAdminSC).setAdministratorRoles("ouTest", testUserUid,
				ImmutableSet.<String>builder().add("manageMailshare").build());

		Set<String> res = storeService.getAdministrators("ouTest");
		assertEquals(ImmutableSet.<String>builder().add(testUserUid).build(), res);

		orgUnits(domainAdminSC).setAdministratorRoles("ouTest", testUserUid, ImmutableSet.<String>builder().build());

		res = storeService.getAdministrators("ouTest");
		assertEquals(ImmutableSet.<String>builder().build(), res);

		try {
			orgUnits(domainAdminSC).setAdministratorRoles("ouTest", "fakeUid", ImmutableSet.<String>builder().build());
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}

		try {
			orgUnits(domainAdminSC).setAdministratorRoles("fakeOuUid", testUserUid,
					ImmutableSet.<String>builder().build());
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}
	}

	@Test
	public void testGetAdministratorRoles() {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkThat";
		storeService.create("ouTest", ou);
		storeService.setAdministratorRoles("ouTest", testUserUid, ImmutableSet.<String>builder().add("test1").build());

		Set<String> res = orgUnits(domainAdminSC).getAdministratorRoles("ouTest", testUserUid, Collections.emptyList());
		assertEquals(ImmutableSet.<String>builder().add("test1").build(), res);

		try {
			orgUnits(domainAdminSC).getAdministratorRoles("ouTest", "fakeUid", Collections.emptyList());
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}
	}

	@Test
	public void testGetAdministrators() {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkThat";
		storeService.create("ouTest", ou);
		storeService.setAdministratorRoles("ouTest", testUserUid, ImmutableSet.<String>builder().add("test1").build());

		Set<String> res = orgUnits(domainAdminSC).getAdministrators("ouTest");
		assertEquals(ImmutableSet.<String>builder().add(testUserUid).build(), res);
	}

	@Test
	public void testListByAdministrator() {
		OrgUnit ou = new OrgUnit();
		ou.name = "checkThat";
		storeService.create("ouTest", ou);
		OrgUnit child = new OrgUnit();
		child.name = "child";
		child.parentUid = "ouTest";
		storeService.create("child", child);

		storeService.setAdministratorRoles("ouTest", testUserUid, ImmutableSet.<String>builder().add("test1").build());
		storeService.setAdministratorRoles("child", testUserUid, ImmutableSet.<String>builder().add("test2").build());
		List<OrgUnitPath> res = orgUnits(domainAdminSC).listByAdministrator(testUserUid, Collections.emptyList());
		assertEquals(2, res.size());
	}

	@Test
	public void search() {
		OrgUnit ou = new OrgUnit();
		ou.name = "root";
		storeService.create("rootuid", ou);
		OrgUnit child = new OrgUnit();
		child.name = "child é ()";
		child.parentUid = "rootuid";
		storeService.create("child", child);

		OrgUnitQuery ouQuery = new OrgUnitQuery();
		ouQuery.query = "root/child é ()";
		List<OrgUnitPath> ouPath = orgUnits(domainAdminSC).search(ouQuery);

		assertEquals(1, ouPath.size());

		assertEquals("child", ouPath.get(0).uid);
		assertEquals("child é ()", ouPath.get(0).name);
		assertNotNull(ouPath.get(0).parent);

		assertEquals("rootuid", ouPath.get(0).parent.uid);
		assertEquals("root", ouPath.get(0).parent.name);
		assertNull(ouPath.get(0).parent.parent);
	}
}
