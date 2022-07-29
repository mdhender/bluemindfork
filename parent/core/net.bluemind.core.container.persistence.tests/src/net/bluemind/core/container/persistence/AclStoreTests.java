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
package net.bluemind.core.container.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.tests.TestHook;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class AclStoreTests {
	private static Logger logger = LoggerFactory.getLogger(ChangelogStoreTests.class);
	private ContainerStore containerHome;
	private String containerId;
	private Container container;
	private AclStore aclStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(), securityContext);

		containerId = "test_" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);
		assertNotNull(container);

		aclStore = new AclStore(null, JdbcTestHelper.getInstance().getDataSource());

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testStore() {
		int prevCount = TestHook.count;
		try {
			aclStore.store(container, Arrays.asList(AccessControlEntry.create("/test", Verb.All),
					AccessControlEntry.create("/toto", Verb.Read)));
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
			fail(e1.getMessage());
		}

		try {
			List<AccessControlEntry> acl = aclStore.get(container);
			assertEquals(2, acl.size());
			assertEquals(//
					ImmutableSet.of( //
							AccessControlEntry.create("/test", Verb.All), //
							AccessControlEntry.create("/toto", Verb.Read)),
					ImmutableSet.copyOf(acl));
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		try {
			aclStore.store(container, Arrays.<AccessControlEntry>asList());
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
			fail(e1.getMessage());
		}

		try {
			List<AccessControlEntry> acl = aclStore.get(container);
			assertEquals(0, acl.size());
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
			fail(e.getMessage());
		}
		assertEquals(prevCount, TestHook.count);
	}

	@Test
	public void storingShouldDeduplicateValues() {
		try {
			AccessControlEntry test = AccessControlEntry.create("test", Verb.All);
			AccessControlEntry testDuplicate = AccessControlEntry.create("test", Verb.All);
			AccessControlEntry toto = AccessControlEntry.create("toto", Verb.Read);
			aclStore.store(container, Arrays.asList(test, toto, testDuplicate));
			List<AccessControlEntry> actual = aclStore.get(container);
			assertEquals(2, actual.size());
			assertTrue(actual.contains(test));
			assertTrue(actual.contains(toto));
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);
			fail(e1.getMessage());
		}

	}

}
