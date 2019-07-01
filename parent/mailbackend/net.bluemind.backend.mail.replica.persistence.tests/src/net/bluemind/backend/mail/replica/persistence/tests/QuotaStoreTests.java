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
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.mail.replica.api.QuotaRoot;
import net.bluemind.backend.mail.replica.persistence.QuotaStore;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class QuotaStoreTests {

	private QuotaStore subStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		subStore = new QuotaStore(JdbcTestHelper.getInstance().getDataSource());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCrudSimple() throws SQLException {
		int before = subStore.byUser("user@test.lab").size();
		assertEquals(0, before);

		QuotaRoot sub = new QuotaRoot("test.lab!user.user", 102400);
		subStore.store(sub);

		List<QuotaRoot> found = subStore.byUser("user@test.lab");
		int after = found.size();
		assertEquals(before + 1, after);
		QuotaRoot fetched = found.get(0);
		assertEquals("test.lab!user.user", fetched.root);
		assertEquals(102400, fetched.limit);

		subStore.delete(sub);

		after = subStore.byUser("user@test.lab").size();
		assertEquals(after, before);
	}

}
