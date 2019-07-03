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

import net.bluemind.backend.mail.replica.api.MailboxSub;
import net.bluemind.backend.mail.replica.persistence.MailboxSubStore;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class MailboxSubStoreTests {

	private MailboxSubStore subStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		subStore = new MailboxSubStore(JdbcTestHelper.getInstance().getDataSource());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCrudSimple() throws SQLException {
		int before = subStore.byUser("user@test.lab").size();
		assertEquals(0, before);

		MailboxSub sub = new MailboxSub("user@test.lab", "tralala");
		subStore.store(sub);

		int after = subStore.byUser("user@test.lab").size();
		assertEquals(before + 1, after);

		subStore.delete(sub);
		after = subStore.byUser("user@test.lab").size();
		assertEquals(after, before);
	}

}
