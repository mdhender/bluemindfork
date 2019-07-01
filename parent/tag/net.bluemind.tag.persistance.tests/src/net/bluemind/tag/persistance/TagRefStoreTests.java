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
package net.bluemind.tag.persistance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.tag.api.Tag;

public class TagRefStoreTests {
	private TagStore tagStore;
	private TagRefStore tagRefStore;
	private ItemStore tagItemStore;
	private ItemStore tagRefItemStore;
	private Container tagContainer;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime();
		tagContainer = Container.create(containerId, "tag", "test", "me", true);
		tagContainer = containerHome.create(tagContainer);

		assertNotNull(tagContainer);

		tagItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), tagContainer, securityContext);

		tagStore = new TagStore(JdbcTestHelper.getInstance().getDataSource(), tagContainer);

		String refContainerId = "testref_" + System.nanoTime();
		Container refContainer = Container.create(refContainerId, "tagref", "test", "me", true);
		refContainer = containerHome.create(refContainer);

		assertNotNull(refContainer);

		tagRefItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), refContainer, securityContext);

		tagRefStore = new TagRefStore(JdbcTestHelper.getInstance().getDataSource(), refContainer);

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
		tagItemStore.create(Item.create("test", null));
		Item item = tagItemStore.get("test");
		Tag tag = new Tag();
		tag.label = "test";
		tag.color = "00ff00";
		tagStore.create(item, tag);

		// test create
		tagRefItemStore.create(Item.create("testref", null));
		Item refTagItem = tagRefItemStore.get("testref");
		assertNotNull(refTagItem);

		ItemTagRef tagRef = new ItemTagRef();
		tagRef.containerUid = tagContainer.uid;
		tagRef.itemUid = item.uid;

		tagRefStore.create(refTagItem, Arrays.asList(tagRef));

		// test get
		List<ItemTagRef> values = tagRefStore.get(refTagItem);
		assertNotNull(values);
		assertEquals(1, values.size());
		ItemTagRef itemTagRef1 = values.get(0);
		Item item1 = tagItemStore.get(itemTagRef1.itemUid);
		assertNotNull(item1);
		Tag tag1 = tagStore.get(item1);
		assertNotNull(tag1);
		assertEquals(tag.label, tag1.label);
		assertEquals(tag.color, tag1.color);

		// test delete tag
		tagStore.delete(item);
		tagItemStore.delete(item);
		values = tagRefStore.get(refTagItem);
		assertNotNull(values);
		// soft link not deleted
		assertEquals(1, values.size());

		// test delete
		tagRefStore.delete(refTagItem);
		values = tagRefStore.get(refTagItem);
		assertNotNull(values);
		assertEquals(0, values.size());

	}
}
