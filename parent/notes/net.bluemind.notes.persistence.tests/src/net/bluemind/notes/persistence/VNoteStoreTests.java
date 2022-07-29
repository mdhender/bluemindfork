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
package net.bluemind.notes.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.api.VNote.Color;

public class VNoteStoreTests extends AbstractStoreTests {
	private static Logger logger = LoggerFactory.getLogger(VNoteStoreTests.class);
	private VNoteStore vNoteStore;
	private ItemStore itemStore;

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

		vNoteStore = new VNoteStore(JdbcTestHelper.getInstance().getDataSource(), container);

	}

	@Test
	public void testSchemaIsWellRegistered() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("vnote-schema"));
	}

	@Test
	public void testStoreAndRetrieveWithUid() throws SQLException {
		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(uid);
		assertNotNull(item);

		VNote note = defaultVNote();
		vNoteStore.create(item, note);

		VNote td = vNoteStore.get(item);

		assertNotNull(td);
		assertEquals(note.subject, td.subject);
		assertEquals(note.body, td.body);
		assertEquals(note.posX.intValue(), td.posX.intValue());
		assertEquals(note.posY.intValue(), td.posY.intValue());
		assertEquals(note.height.intValue(), td.height.intValue());
		assertEquals(note.width.intValue(), td.width.intValue());
		assertEquals(note.color, td.color);
		assertEquals(Color.YELLOW, td.color);

		item.id = new Random().nextInt();
		td = vNoteStore.get(item);
		assertNull(td);
	}

	@Test
	public void testStoreRetrieveAndUpdate() throws SQLException {

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(uid);
		assertNotNull(item);

		VNote note = defaultVNote();
		note.color = Color.BLUE;
		vNoteStore.create(item, note);

		VNote td = vNoteStore.get(item);
		assertNotNull(td);
		assertEquals(note.subject, td.subject);
		assertEquals(note.body, td.body);
		assertEquals(note.posX.intValue(), td.posX.intValue());
		assertEquals(note.posY.intValue(), td.posY.intValue());
		assertEquals(note.height.intValue(), td.height.intValue());
		assertEquals(note.width.intValue(), td.width.intValue());
		assertEquals(note.color, td.color);

		td.subject = "updated subject";
		td.body = "updated content";
		td.color = Color.PINK;

		vNoteStore.update(item, td);
		VNote updated = vNoteStore.get(item);
		assertNotNull(updated);

		assertNotNull(td);
		assertEquals(updated.subject, td.subject);
		assertEquals(updated.body, td.body);
		assertEquals(updated.posX.intValue(), td.posX.intValue());
		assertEquals(updated.posY.intValue(), td.posY.intValue());
		assertEquals(updated.height.intValue(), td.height.intValue());
		assertEquals(updated.width.intValue(), td.width.intValue());
		assertEquals(updated.color, td.color);
	}

	@Test
	public void testDelete() throws SQLException {
		String uid = "test_" + System.nanoTime();
		VNote note = defaultVNote();

		createAndGet(uid, note);
		Item item = itemStore.get(uid);
		assertNotNull(item);

		vNoteStore.delete(item);
		assertNull(vNoteStore.get(item));
	}

	@Test
	public void testDeleteAll() throws SQLException {
		String uid = "test_" + System.nanoTime();
		VNote todo = defaultVNote();
		createAndGet(uid, todo);
		Item item = itemStore.get(uid);
		assertNotNull(item);

		String uid2 = "test_" + System.nanoTime();
		VNote todo2 = defaultVNote();
		createAndGet(uid2, todo2);
		Item item2 = itemStore.get(uid2);
		assertNotNull(item2);

		vNoteStore.deleteAll();
		assertNull(vNoteStore.get(item));
		assertNull(vNoteStore.get(item2));
	}

	private VNote createAndGet(String uid, VNote todo) {
		try {
			itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
			Item item = itemStore.get(uid);

			vNoteStore.create(item, todo);

			return vNoteStore.get(item);

		} catch (SQLException e) {
			logger.error("error during vnote persistence call", e);
			fail(e.getMessage());
			return null;
		}

	}

}
