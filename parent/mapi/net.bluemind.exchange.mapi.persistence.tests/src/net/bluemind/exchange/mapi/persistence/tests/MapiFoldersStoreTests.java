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
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.exchange.mapi.api.MapiFolder;
import net.bluemind.exchange.mapi.persistence.MapiFoldersStore;

public class MapiFoldersStoreTests {

	private MapiFoldersStore foldersStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		foldersStore = new MapiFoldersStore(JdbcTestHelper.getInstance().getDataSource());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void foldersCrud() throws Exception {

		MapiFolder replica = new MapiFolder();
		replica.replicaGuid = UUID.randomUUID().toString();
		replica.containerUid = "mapi_root_" + replica.replicaGuid;
		replica.displayName = "ROOT";
		replica.parentContainerUid = replica.containerUid;
		replica.pidTagContainerClass = "IPF.Note";
		foldersStore.store(replica);
		MapiFolder found = foldersStore.get(replica.containerUid);
		assertNotNull(found);
		assertEquals(replica.replicaGuid, found.replicaGuid);
		assertEquals("ROOT", found.displayName);

		replica.displayName = "toto";
		foldersStore.store(replica);
		found = foldersStore.get(replica.containerUid);
		assertNotNull(found);
		assertEquals("toto", found.displayName);

		foldersStore.delete(replica.containerUid);
		found = foldersStore.get(replica.containerUid);
		assertNull(found);
	}

}
