/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.webappdata.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.webappdata.api.WebAppData;
import net.bluemind.webappdata.persistence.WebAppDataStore;

public class WebAppDataStoreTests {

	private ItemStore itemStore;
	private WebAppDataStore webAppDataStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime();
		Container container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);
		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);
		webAppDataStore = new WebAppDataStore(JdbcTestHelper.getInstance().getDataSource(), container);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSchemaIsWellRegistered() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("webappdata-schema"));
	}

	@Test
	public void testCreate() throws SQLException {
		WebAppData created = exampleWebAppData();
		Item item = createItem();

		webAppDataStore.create(item, created);
		WebAppData retrieved = webAppDataStore.get(item);
		assertNotNull(retrieved);
		assertEquals(created.key, retrieved.key);
		assertEquals(created.value, retrieved.value);

		item.id = new Random().nextInt();
		retrieved = webAppDataStore.get(item);
		assertNull(retrieved);
	}

	@Test
	public void testDelete() throws SQLException {
		WebAppData created = exampleWebAppData();
		Item item = createItem();

		webAppDataStore.create(item, created);
		webAppDataStore.delete(item);
		WebAppData deleted = webAppDataStore.get(item);

		assertNull(deleted);
	}

	@Test
	public void testUpdate() throws SQLException {
		WebAppData created = exampleWebAppData();
		Item item = createItem();

		webAppDataStore.create(item, created);
		WebAppData updated = exampleWebAppData();
		updated.value = "updated-value";
		webAppDataStore.update(item, updated);
		updated = webAppDataStore.get(item);

		assertEquals(updated.value, "updated-value");
	}

	@Test
	public void testInsertDuplicate() throws SQLException {
		WebAppData created = exampleWebAppData();
		Item item = createItem();

		webAppDataStore.create(item, created);

		try {
			webAppDataStore.create(item, created);
			fail("Should not be possible to create duplicate webappdata");
		} catch (Exception e) {

		}

	}

	private WebAppData exampleWebAppData() {
		WebAppData data = new WebAppData();
		data.key = "mailapp:right_panel_size";
		data.value = "400px";
		return data;
	}

	private Item createItem() throws SQLException {
		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		return itemStore.get(uid);
	}

}
