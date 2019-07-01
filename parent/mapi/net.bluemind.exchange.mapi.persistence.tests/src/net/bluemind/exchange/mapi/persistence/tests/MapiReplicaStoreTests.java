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
package net.bluemind.exchange.mapi.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.exchange.mapi.persistence.MapiReplicaStore;

public class MapiReplicaStoreTests {

	private MapiReplicaStore mapiReplicaStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		mapiReplicaStore = new MapiReplicaStore(JdbcTestHelper.getInstance().getDataSource());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void replicaCrud() throws Exception {

		MapiReplica replica = new MapiReplica();
		replica.localReplicaGuid = UUID.randomUUID().toString();
		replica.logonReplicaGuid = UUID.randomUUID().toString();
		replica.mailboxGuid = UUID.randomUUID().toString();
		replica.mailboxUid = "mbox";
		mapiReplicaStore.store(replica);
		MapiReplica found = mapiReplicaStore.get(replica.mailboxUid);
		assertNotNull(found);
		assertEquals(replica.localReplicaGuid, found.localReplicaGuid);
		replica.localReplicaGuid = "local";
		mapiReplicaStore.store(replica);
		found = mapiReplicaStore.get(replica.mailboxUid);
		assertNotNull(found);
		assertEquals("local", found.localReplicaGuid);
	}

}
