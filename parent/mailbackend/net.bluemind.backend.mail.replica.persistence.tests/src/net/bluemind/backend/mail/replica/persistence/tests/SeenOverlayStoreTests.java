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
package net.bluemind.backend.mail.replica.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.mail.replica.api.SeenOverlay;
import net.bluemind.backend.mail.replica.persistence.SeenOverlayStore;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class SeenOverlayStoreTests {

	private SeenOverlayStore seenStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		seenStore = new SeenOverlayStore(JdbcTestHelper.getInstance().getDataSource());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCrudSimple() throws SQLException {
		int before = seenStore.byUser("user@test.lab").size();
		assertEquals(0, before);
		SeenOverlay seen = new SeenOverlay();
		seen.userId = "user@test.lab";
		seen.uniqueId = "1234";
		seen.lastChange = 1;
		seen.lastUid = 2;
		seen.lastRead = 3;
		seen.seenUids = "1,2,3";
		seenStore.store(seen);
		int after = seenStore.byUser("user@test.lab").size();
		assertEquals(1, after);

	}

	@Test
	public void testStoreOverwrites() throws SQLException {
		assertNull(seenStore.byUser("user@test.lab", "1234"));
		SeenOverlay seen = new SeenOverlay();
		seen.userId = "user@test.lab";
		seen.uniqueId = "1234";
		seen.lastChange = 1;
		seen.lastUid = 2;
		seen.lastRead = 3;
		seen.seenUids = "1,2,3";
		seenStore.store(seen);
		assertNotNull(seenStore.byUser("user@test.lab", "1234"));

		seen.seenUids = "4,5,6";
		seenStore.store(seen);
		int after = seenStore.byUser("user@test.lab").size();
		assertEquals(1, after);

	}

}
