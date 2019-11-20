/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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

import java.sql.SQLException;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.backend.mail.replica.persistence.DeletedMailboxesStore;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class SubtreeUidStoreTests {
	private DeletedMailboxesStore store;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		store = new DeletedMailboxesStore(JdbcTestHelper.getInstance().getDataSource());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCrudSimple() throws SQLException {
		Subtree s = new Subtree();
		s.ownerUid = UUID.randomUUID().toString();
		s.mailboxName = "mboxName-" + s.ownerUid;
		s.namespace = Namespace.users;
		s.domainUid = "bm.lan";

		store.store(s);

		Subtree created = store.getByMboxName(s.domainUid, s.mailboxName);
		assertNotNull(created);
		assertEquals(s.ownerUid, created.ownerUid);
		assertEquals(s.mailboxName, created.mailboxName);
		assertEquals(s.namespace, created.namespace);
		assertEquals(s.domainUid, created.domainUid);
	}
}
