/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.videoconferencing.saas.persistence.tests;

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
import net.bluemind.videoconferencing.saas.api.BlueMindVideoRoom;
import net.bluemind.videoconferencing.saas.persistence.RoomStore;

public class RoomStoreTests {

	private ItemStore itemStore;
	private RoomStore roomStore;

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
		roomStore = new RoomStore(JdbcTestHelper.getInstance().getDataSource(), container);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSchemaIsWellRegistered() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("room-schema"));
	}

	@Test
	public void testCreate() throws SQLException {
		BlueMindVideoRoom r = defaultRoom();

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		roomStore.create(item, r);

		BlueMindVideoRoom room = roomStore.get(item);
		assertNotNull(room);
		assertEquals(r.identifier, room.identifier);
		assertEquals(r.owner, room.owner);
		assertEquals(r.title, room.title);

		item.id = new Random().nextInt();
		room = roomStore.get(item);
		assertNull(room);

		String itemUid = roomStore.byIdentifier(r.identifier);
		assertNotNull(itemUid);
	}

	@Test
	public void testDelete() throws SQLException {
		BlueMindVideoRoom r = defaultRoom();

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		roomStore.create(item, r);

		BlueMindVideoRoom room = roomStore.get(item);
		assertNotNull(room);
		roomStore.delete(item);
		room = roomStore.get(item);
		assertNull(room);
	}

	@Test
	public void testUpdate() throws SQLException {
		BlueMindVideoRoom r = defaultRoom();

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		roomStore.create(item, r);

		BlueMindVideoRoom room = roomStore.get(item);
		room.title = "Titre modifié";

		roomStore.update(item, room);
		room = roomStore.get(item);
		assertEquals("Titre modifié", room.title);

		String itemUid = roomStore.byIdentifier(r.identifier);
		assertNotNull(itemUid);
	}

	@Test
	public void testInsertDuplicate() throws SQLException {
		BlueMindVideoRoom r = defaultRoom();

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		roomStore.create(item, r);

		try {
			roomStore.create(item, r);
			fail("Should not be possible to create duplicate device");
		} catch (Exception e) {
		}

	}

	private BlueMindVideoRoom defaultRoom() {
		BlueMindVideoRoom room = new BlueMindVideoRoom();
		room.identifier = "a8e15893-1a0a-40b2-9443-4f4d5396f844";
		room.owner = "deviceOwner";
		room.title = "ma salle";
		return room;
	}

}
