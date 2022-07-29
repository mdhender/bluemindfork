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
import static org.junit.Assert.assertNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class ContainerHierarchyStoreTests {

	private String faiContainerId;
	private ContainersHierarchyNodeStore hierStore;
	private ItemStore nodeItemStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		faiContainerId = IFlatHierarchyUids.getIdentifier("fake_owner", "fake_domain");
		Container fais = Container.create(faiContainerId, IFlatHierarchyUids.TYPE, faiContainerId, "me", true);
		fais = containerStore.create(fais);

		hierStore = new ContainersHierarchyNodeStore(JdbcTestHelper.getInstance().getDataSource(), fais);
		nodeItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), fais, securityContext);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void createThenGetNode() throws Exception {
		String uid = "" + System.currentTimeMillis();
		Item item = nodeItemStore.create(Item.create(uid, null));
		ContainerHierarchyNode node = new ContainerHierarchyNode();
		node.containerType = "cont_type";
		node.containerUid = "cont_uid";
		node.name = "cont_name";
		hierStore.create(item, node);
		ContainerHierarchyNode got = hierStore.get(item);
		assertEquals(node.containerType, got.containerType);
		assertEquals(node.containerUid, got.containerUid);
		assertEquals(node.name, got.name);
	}

	@Test
	public void createThenDeleteNode() throws Exception {
		String uid = "" + System.currentTimeMillis();
		Item item = nodeItemStore.create(Item.create(uid, null));
		ContainerHierarchyNode node = new ContainerHierarchyNode();
		node.containerType = "cont_type";
		node.containerUid = "cont_uid";
		node.name = "Name";
		hierStore.create(item, node);
		ContainerHierarchyNode got = hierStore.get(item);
		assertNotNull(got);
		hierStore.delete(item);
		got = hierStore.get(item);
		assertNull(got);
	}

	@Test
	public void updateNode() throws Exception {
		String uid = "" + System.currentTimeMillis();
		Item item = nodeItemStore.create(Item.create(uid, null));
		ContainerHierarchyNode node = new ContainerHierarchyNode();
		node.containerType = "cont_type";
		node.containerUid = "cont_uid";
		node.name = "cont_name";
		hierStore.create(item, node);

		node.containerUid = "cont_uid2";
		node.containerType = "cont_type2";
		node.name = "cont_name2";
		hierStore.update(item, node);
		ContainerHierarchyNode got = hierStore.get(item);
		assertEquals("cont_uid2", got.containerUid);
		assertEquals("cont_type2", got.containerType);
		assertEquals("cont_name2", got.name);
	}

	@Test
	public void testRemovingExpiredNodes() throws Exception {
		String uid1 = "" + System.currentTimeMillis();
		Item item = nodeItemStore.create(Item.create(uid1, null));
		ContainerHierarchyNode node = new ContainerHierarchyNode();
		node.containerType = IMailReplicaUids.MAILBOX_RECORDS;
		node.containerUid = "cont_uid1";
		node.name = "Name1";
		hierStore.create(item, node);
		assertNotNull(hierStore.get(item));

		String uid2 = "" + System.currentTimeMillis();
		Item item2 = Item.create(uid2, null);
		item2.flags = Arrays.asList(ItemFlag.Deleted);
		item2 = nodeItemStore.create(item2);
		setLastUpdated(item2.uid, 1000);
		ContainerHierarchyNode node2 = new ContainerHierarchyNode();
		node2.containerType = IMailReplicaUids.MAILBOX_RECORDS;
		node2.containerUid = "cont_uid2";
		node2.name = "Name2";
		hierStore.create(item2, node2);
		assertNotNull(hierStore.get(item2));

		hierStore.removeDeletedRecords(7);

		assertNotNull(hierStore.get(item));
		assertNull(hierStore.get(item2));

		assertNotNull(nodeItemStore.get(item.uid));
		assertNull(nodeItemStore.get(item2.uid));
	}

	private void setLastUpdated(String uid, int days) throws SQLException {
		String update = "UPDATE t_container_item set updated = (now() - interval '" + days + " days') where uid = '"
				+ uid + "'";
		try (Connection con = JdbcTestHelper.getInstance().getDataSource().getConnection();
				Statement st = con.createStatement()) {
			st.executeUpdate(update);
		}
	}

}
