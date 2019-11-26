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
package net.bluemind.document.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.document.api.DocumentMetadata;
import net.bluemind.document.persistence.DocumentMetadataStore;

public class DocumentStoreTests {

	private Item item;
	private DocumentMetadataStore store;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime();
		Container container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);

		ItemStore itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

		String uid = UUID.randomUUID().toString();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		item = itemStore.get(uid);

		store = new DocumentMetadataStore(JdbcTestHelper.getInstance().getDataSource());

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testStoreAndRetreive() throws ServerFault {
		DocumentMetadata dm = new DocumentMetadata();
		dm.uid = UUID.randomUUID().toString();
		dm.name = "Top secret file à 4€";
		dm.filename = "filename.png";
		dm.mime = "image/png";
		dm.description = "this is description";
		store.create(item, dm);

		DocumentMetadata fetched = store.get(dm.uid);
		assertNotNull(fetched);

		assertEquals(dm, fetched);
	}

	@Test
	public void testUpdateAndRetrive() throws ServerFault {
		DocumentMetadata dm = new DocumentMetadata();
		dm.uid = UUID.randomUUID().toString();
		dm.name = "docName";
		dm.filename = "filename.png";
		dm.mime = "image/png";
		dm.description = "this is description";
		store.create(item, dm);

		dm.filename = "another.filename.png";
		store.update(item, dm);

		DocumentMetadata fetched = store.get(dm.uid);
		assertNotNull(fetched);
		assertEquals(dm, fetched);
	}

	@Test
	public void testStoreAndDelete() throws ServerFault {
		DocumentMetadata dm = new DocumentMetadata();
		dm.uid = UUID.randomUUID().toString();
		dm.name = "docName";
		dm.filename = "filename.png";
		dm.mime = "image/png";
		dm.description = "this is description";
		store.create(item, dm);

		store.delete(dm.uid);

		DocumentMetadata fetched = store.get(dm.uid);
		assertNull(fetched);
	}

	@Test
	public void testDeleteAll() throws ServerFault {
		DocumentMetadata dm = new DocumentMetadata();
		dm.uid = UUID.randomUUID().toString();
		dm.name = "docName";
		dm.filename = "filename.png";
		dm.mime = "image/png";
		dm.description = "this is description";
		store.create(item, dm);

		DocumentMetadata dm2 = new DocumentMetadata();
		dm2.uid = UUID.randomUUID().toString();
		dm2.name = "docName";
		dm2.filename = "another-filename.png";
		dm2.mime = "image/png";
		dm2.description = "this is description";
		store.create(item, dm2);

		store.deleteAll(item);
		DocumentMetadata fetched = store.get(dm.uid);
		assertNull(fetched);

		DocumentMetadata fetched2 = store.get(dm2.uid);
		assertNull(fetched2);

	}

	@Test
	public void testGetAll() throws ServerFault {
		DocumentMetadata dm = new DocumentMetadata();
		dm.uid = UUID.randomUUID().toString();
		dm.name = "docName";
		dm.filename = "filename.png";
		dm.mime = "image/png";
		dm.description = "this is description";
		store.create(item, dm);

		DocumentMetadata dm2 = new DocumentMetadata();
		dm2.uid = UUID.randomUUID().toString();
		dm2.name = "docName";
		dm2.filename = "another-filename.png";
		dm2.mime = "image/png";
		dm2.description = "this is description";
		store.create(item, dm2);

		List<DocumentMetadata> all = store.getAll(item);
		assertEquals(2, all.size());

		int found = 0;
		for (DocumentMetadata meta : all) {
			if (dm.equals(meta)) {
				found++;
			}

			if (dm2.equals(meta)) {
				found++;
			}
		}

		assertEquals(2, found);

		store.delete(dm.uid);
		all = store.getAll(item);
		assertEquals(1, all.size());
		assertEquals(dm2, all.get(0));

		store.delete(dm2.uid);
		all = store.getAll(item);
		assertEquals(0, all.size());

	}
}
