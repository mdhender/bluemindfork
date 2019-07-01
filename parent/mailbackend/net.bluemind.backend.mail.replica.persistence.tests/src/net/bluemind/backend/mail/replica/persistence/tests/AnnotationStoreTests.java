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

import net.bluemind.backend.mail.replica.api.MailboxAnnotation;
import net.bluemind.backend.mail.replica.persistence.AnnotationStore;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class AnnotationStoreTests {

	private AnnotationStore subStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		subStore = new AnnotationStore(JdbcTestHelper.getInstance().getDataSource());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCrudSimple() throws SQLException {
		MailboxAnnotation sub = new MailboxAnnotation();
		sub.mailbox = "test.lab!user.user";
		sub.userId = "user@test.lab";
		sub.entry = "the_key";
		sub.value = "id";
		List<MailboxAnnotation> before = subStore.byMailbox(sub.mailbox);
		subStore.store(sub);
		List<MailboxAnnotation> after = subStore.byMailbox(sub.mailbox);
		assertEquals(before.size() + 1, after.size());
		subStore.store(sub);
		after = subStore.byMailbox(sub.mailbox);
		assertEquals("store duplicated an entry instead of updating it", before.size() + 1, after.size());

		subStore.delete(sub);
		after = subStore.byMailbox(sub.mailbox);
		assertEquals(before.size(), after.size());

	}

}
