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
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class ContainerSyncStoreTests {

	private Container c;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ContainerStore cs = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM);
		String uid = UUID.randomUUID().toString();
		cs.create(Container.create(uid, "calendar", "osef", ""));
		c = cs.get(uid);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testGetAndSetSyncVersion() throws ServerFault {
		ContainerSyncStore store = new ContainerSyncStore(JdbcTestHelper.getInstance().getDataSource(), c);

		assertNull(store.getSyncStatus());
		ContainerSyncStatus ss = new ContainerSyncStatus();
		ss.syncTokens.put("Tata", "Suzanne");
		ss.nextSync = System.currentTimeMillis();
		ss.syncStatusInfo = "OK";
		store.initSync();
		store.setSyncStatus(ss);

		ContainerSyncStatus ret = store.getSyncStatus();
		assertEquals(ss.nextSync, ret.nextSync);
		assertEquals("Suzanne", ret.syncTokens.get("Tata"));
		assertEquals("OK", ret.syncStatusInfo);

		ss.nextSync = System.currentTimeMillis();
		store.setSyncStatus(ss);

		ret = store.getSyncStatus();
		assertEquals(ss.nextSync, ret.nextSync);
		assertEquals("Suzanne", ret.syncTokens.get("Tata"));
		assertEquals("OK", ret.syncStatusInfo);

		store = new ContainerSyncStore(JdbcTestHelper.getInstance().getDataSource(),
				Container.create(UUID.randomUUID().toString(), "calendar", "osef", ""));
		assertNull(store.getSyncStatus());

	}
}