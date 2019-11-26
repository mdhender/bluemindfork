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
package net.bluemind.tag.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.tag.api.Tag;

public class TagStoreTests {
	private TagStore tagStore;
	private ItemStore itemStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime();
		Container container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);

		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

		tagStore = new TagStore(JdbcTestHelper.getInstance().getDataSource(), container);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSchemaIsWellRegistered() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("tag-schema"));
	}

	@Test
	public void testCreateUpdateDeleteGet() throws SQLException {
		itemStore.create(Item.create("test", null));
		Item item = itemStore.get("test");
		Tag tag = new Tag();
		tag.label = "test";
		tag.color = "00ff00";

		// test create
		try {
			tagStore.create(item, tag);
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// test get
		Tag found = tagStore.get(item);
		assertNotNull(found);
		assertEquals(tag.label, found.label);
		assertEquals(tag.color, found.color);

		// test update
		tag.label = "updated";
		tag.color = "ff0000";
		tagStore.update(item, tag);
		found = tagStore.get(item);
		assertNotNull(found);
		assertEquals(tag.label, found.label);
		assertEquals(tag.color, found.color);

		// test delete
		tagStore.delete(item);
		found = tagStore.get(item);
		assertNull(found);
	}
}
