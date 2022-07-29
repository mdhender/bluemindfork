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
package net.bluemind.device.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
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
import net.bluemind.device.api.Device;
import net.bluemind.device.persistence.DeviceStore;

public class DeviceStoreTests {

	private ItemStore itemStore;
	private DeviceStore deviceStore;

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

		deviceStore = new DeviceStore(JdbcTestHelper.getInstance().getDataSource(), container);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSchemaIsWellRegistered() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("device-schema"));
	}

	@Test
	public void testCreate() throws SQLException {
		Device d = defaultDevice();

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		deviceStore.create(item, d);

		Device dev = deviceStore.get(item);
		assertNotNull(dev);
		assertEquals(d.identifier, dev.identifier);
		assertEquals(d.type, dev.type);
		assertEquals(d.owner, dev.owner);
		assertEquals(d.policy, dev.policy);
		assertFalse(dev.hasPartnership);

		item.id = new Random().nextInt();
		dev = deviceStore.get(item);
		assertNull(dev);

		String itemUid = deviceStore.byIdentifier(d.identifier);
		assertNotNull(itemUid);
	}

	@Test
	public void testDelete() throws SQLException {
		Device d = defaultDevice();

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		deviceStore.create(item, d);

		Device dev = deviceStore.get(item);
		assertNotNull(dev);

		deviceStore.delete(item);

		dev = deviceStore.get(item);
		assertNull(dev);
	}

	@Test
	public void testUpdate() throws SQLException {
		Device d = defaultDevice();

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		deviceStore.create(item, d);

		Device dev = deviceStore.get(item);
		dev.hasPartnership = true;

		deviceStore.update(item, dev);
		dev = deviceStore.get(item);
		assertTrue(dev.hasPartnership);

		String itemUid = deviceStore.byIdentifier(d.identifier);
		assertNotNull(itemUid);
	}

	@Test
	public void testWipe() throws SQLException {
		List<Device> wiped = deviceStore.getWipedDevice();
		assertEquals(0, wiped.size());

		Device d = defaultDevice();
		d.isWipe = true;
		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(uid);
		deviceStore.create(item, d);

		wiped = deviceStore.getWipedDevice();
		assertEquals(1, wiped.size());

	}

	@Test
	public void testUpdateLastSync() throws SQLException {
		Device d = defaultDevice();

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		deviceStore.create(item, d);

		Device dev = deviceStore.get(item);
		assertNull(dev.lastSync);

		dev.lastSync = new Date();

		deviceStore.update(item, dev);
		dev = deviceStore.get(item);
		assertNotNull(dev.lastSync);

	}

	@Test
	public void testInsertDuplicate() throws SQLException {
		Device d = defaultDevice();

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));

		Item item = itemStore.get(uid);
		deviceStore.create(item, d);

		try {
			deviceStore.create(item, d);
			fail("Should not be possible to create duplicate device");
		} catch (Exception e) {

		}

	}

	private Device defaultDevice() {
		Device ret = new Device();
		ret.identifier = "androidc259148960";
		ret.type = "Android";
		ret.owner = "deviceOwner";
		ret.policy = 42;

		return ret;
	}

}
