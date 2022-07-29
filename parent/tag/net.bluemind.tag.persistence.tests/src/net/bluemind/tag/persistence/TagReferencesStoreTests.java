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
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemUri;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.tag.api.Tag;

public class TagReferencesStoreTests {
	private TagStore tagStore;
	private ItemStore tagItemStore;

	private ItemStore container1ItemStore;
	private ItemStore container2ItemStore;

	private TagRefStore container1tagRefStore;
	private TagRefStore container2tagRefStore;

	private Container tagContainer;

	private TagReferencesStore tagReferencesStore;

	private String tagUid;

	private Container cointainer1;
	private Container cointainer2;
	private String tag2Uid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		String containerId = "test_" + System.nanoTime();
		tagContainer = Container.create(containerId, "tag", "test", "me", true);
		tagContainer = containerHome.create(tagContainer);
		assertNotNull(tagContainer);

		tagItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), tagContainer, securityContext);

		tagStore = new TagStore(JdbcTestHelper.getInstance().getDataSource(), tagContainer);

		String refContainerId = "testref_" + System.nanoTime();

		cointainer1 = Container.create(refContainerId, "tagref", "test", "me", true);
		cointainer1 = containerHome.create(cointainer1);

		assertNotNull(cointainer1);

		refContainerId = "testref2_" + System.nanoTime();
		cointainer2 = Container.create(refContainerId, "tagref", "test", "me", true);
		cointainer2 = containerHome.create(cointainer2);

		assertNotNull(cointainer1);

		container1ItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), cointainer1, securityContext);

		container1tagRefStore = new TagRefStore(JdbcTestHelper.getInstance().getDataSource(), cointainer1);

		container2ItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), cointainer2, securityContext);

		container2tagRefStore = new TagRefStore(JdbcTestHelper.getInstance().getDataSource(), cointainer2);

		tagReferencesStore = new TagReferencesStore(JdbcTestHelper.getInstance().getDataSource());

		tagUid = "test";
		tagItemStore.create(Item.create(tagUid, null));
		Item item = tagItemStore.get(tagUid);
		Tag tag = new Tag();
		tag.label = "test";
		tag.color = "00ff00";
		tagStore.create(item, tag);

		tag2Uid = "tag2";
		tagItemStore.create(Item.create("tag2", null));
		item = tagItemStore.get("tag2");
		tag = new Tag();
		tag.label = "tag2";
		tag.color = "00ff00";
		tagStore.create(item, tag);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testReferencedBy() throws SQLException {
		ItemTagRef ref = new ItemTagRef();
		ref.containerUid = tagContainer.uid;
		ref.itemUid = tagUid;

		// create two references into container1
		container1ItemStore.create(Item.create("test", null));
		container1tagRefStore.create(container1ItemStore.get("test"), Arrays.asList(ref));

		container1ItemStore.create(Item.create("test2", null));
		container1tagRefStore.create(container1ItemStore.get("test2"), Arrays.asList(ref));

		// create one reference into container2
		container2ItemStore.create(Item.create("test", null));
		container2tagRefStore.create(container2ItemStore.get("test"), Arrays.asList(ref));

		List<ItemUri> found = tagReferencesStore.referencedBy("tagref", tagContainer.uid, tagUid);

		assertEquals(3, found.size());

		for (ItemUri uri : found) {
			assertNotNull(uri.containerUid, uri.itemUid);
		}
		found = tagReferencesStore.referencedBy("tagref", tagContainer.uid, tag2Uid);
		assertEquals(0, found.size());

	}

	@Test
	public void testDeleteReferences() throws SQLException {
		ItemTagRef ref = new ItemTagRef();
		ref.containerUid = tagContainer.uid;
		ref.itemUid = tagUid;

		// create two references into container1
		container1ItemStore.create(Item.create("test", null));
		container1tagRefStore.create(container1ItemStore.get("test"), Arrays.asList(ref));

		container1ItemStore.create(Item.create("test2", null));
		container1tagRefStore.create(container1ItemStore.get("test2"), Arrays.asList(ref));

		// create one reference into container2
		container2ItemStore.create(Item.create("test", null));
		container2tagRefStore.create(container2ItemStore.get("test"), Arrays.asList(ref));

		List<ItemUri> found = tagReferencesStore.referencedBy("tagref", tagContainer.uid, tagUid);

		assertEquals(3, found.size());

		tagReferencesStore.deleteReferences("tagref", tagContainer.uid, tagUid);
		found = tagReferencesStore.referencedBy("tagref", tagContainer.uid, tagUid);
		assertTrue(found.isEmpty());

	}

}
