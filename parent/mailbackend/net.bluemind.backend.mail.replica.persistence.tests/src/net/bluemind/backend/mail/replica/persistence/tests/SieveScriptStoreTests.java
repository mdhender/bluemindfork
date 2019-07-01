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

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.mail.replica.api.SieveScript;
import net.bluemind.backend.mail.replica.persistence.SieveScriptStore;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class SieveScriptStoreTests {

	private SieveScriptStore ssStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		ssStore = new SieveScriptStore(JdbcTestHelper.getInstance().getDataSource());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCrudSimple() throws SQLException {
		SieveScript seen = new SieveScript("user@test.lab", "toto.sieve", 123, false);
		int count = ssStore.byUser("user@test.lab").size();
		ssStore.store(seen);
		int reCount = ssStore.byUser("user@test.lab").size();
		assertEquals(count + 1, reCount);
		ssStore.store(seen);
		int thirdCount = ssStore.byUser("user@test.lab").size();
		assertEquals(reCount, thirdCount);
	}

}
