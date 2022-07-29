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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class ItemStoreTests {

	private ItemStore home;
	private ContainerStore containerHome;
	private String containerId;
	private Container container;

	@Before
	public void before() throws Exception {

		SecurityContext securityContext = new SecurityContext(null, "system", Arrays.<String>asList(),
				Arrays.<String>asList(), null);
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(), securityContext);

		containerId = "test_" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "test", true);
		container = containerHome.create(container);
		assertNotNull(container);

		home = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreateAndGet() throws SQLException {
		Item item = new Item();
		item.uid = "test_" + System.nanoTime();
		item.externalId = "externalId";
		item.displayName = "test";
		try {
			home.create(item);
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Item result = home.get(item.uid);
		assertNotNull(result);
		assertEquals(result.uid, item.uid);
		assertEquals(result.externalId, item.externalId);
		assertEquals(result.displayName, item.displayName);
		assertNotNull(result.created);
		assertNotNull(result.updated);
		assertEquals(result.createdBy, "system");
		assertEquals(result.updatedBy, "system");
	}

	@Test
	public void testCreateWithNullUid() throws SQLException {
		Item item = new Item();
		item.externalId = "externalId";
		item.displayName = "test";
		Item res = null;
		try {
			res = home.createWithUidNull(item);
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertNotNull(res);
		assertEquals("" + res.id, res.uid);
	}

	@Test
	public void testCreateAndGetByExtId() throws SQLException {
		Item item = new Item();
		item.uid = "test_" + System.nanoTime();
		item.externalId = "externalId" + System.nanoTime();
		item.displayName = "test";
		item.flags = EnumSet.of(ItemFlag.Seen);
		try {
			home.create(item);
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Item result = home.getByExtId(item.externalId);
		assertNotNull(result);
		assertEquals(result.uid, item.uid);
		assertEquals(result.externalId, item.externalId);
		assertEquals(result.displayName, item.displayName);
		assertNotNull(result.created);
		assertNotNull(result.updated);
		assertEquals(result.createdBy, "system");
		assertEquals(result.updatedBy, "system");
		assertEquals(EnumSet.of(ItemFlag.Seen), result.flags);
	}

	@Test
	public void testExtIdUnicity() throws SQLException {
		Item item = new Item();
		item.uid = "test_" + System.nanoTime();
		item.externalId = "externalId" + System.nanoTime();
		item.displayName = "test";
		try {
			home.create(item);
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		// there is not constraint on uniqness of external id
		Item item2 = new Item();
		item2.uid = "test_" + System.nanoTime();
		item2.externalId = item.externalId;
		item2.displayName = "test2";
		try {
			home.create(item2);
		} catch (SQLException e) {
			fail("Must not throw an exception");
		}
	}

	@Test
	public void testCreateAndGetMultiple() throws SQLException {

		List<String> uids = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Item item = new Item();
			item.uid = "test_" + i + "_" + System.nanoTime();
			item.displayName = "test";
			try {
				home.create(item);
			} catch (SQLException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
			uids.add(item.uid);
		}

		List<Item> result = home.getMultiple(uids);
		assertNotNull(result);
		assertEquals(10, result.size());
		for (int i = 0; i < result.size(); i++) {
			assertEquals(i, uids.indexOf(result.get(i).uid));
		}

		List<String> withHole = new ArrayList<>(uids);
		withHole.add(4, "toto");
		withHole.add(9, "toto");
		result = home.getMultiple(withHole);
		assertNotNull(result);
		assertEquals(10, result.size());
	}

	@Test
	public void testCreateAndGetMultipleById() throws SQLException {

		List<Long> ids = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Item item = new Item();
			item.uid = "test_" + i + "_" + System.nanoTime();
			item.displayName = "test";
			try {
				item = home.create(item);
			} catch (SQLException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
			ids.add(item.id);
		}

		List<Item> result = home.getMultipleById(ids);
		assertNotNull(result);
		assertEquals(10, result.size());
		for (int i = 0; i < result.size(); i++) {
			assertEquals(i, ids.indexOf(result.get(i).id));
		}
	}

	@Test
	public void testCreateAndAll() throws SQLException {

		List<String> uids = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Item item = new Item();
			item.uid = "test_" + i + "_" + System.nanoTime();
			item.displayName = "test";
			try {
				home.create(item);
			} catch (SQLException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
			uids.add(item.uid);
		}

		List<Item> result = home.all();
		assertNotNull(result);
		assertEquals(uids.size(), result.size());
	}

	@Test
	public void testSetExtId() throws SQLException {
		Item item = new Item();
		item.uid = "test_" + System.nanoTime();
		item.externalId = "externalId" + System.nanoTime();
		item.displayName = "test";
		try {
			item = home.create(item);
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		long version = item.version;
		try {
			home.setExtId(item.uid, "myExtId");
		} catch (SQLException e) {
			fail("Must not throw an exception");
		}

		item = home.get(item.uid);
		assertEquals("myExtId", item.externalId);
		assertTrue(version < item.version);
	}

	@Test
	public void testCreateById() throws Exception {
		long currentSeq = currentId();
		restartSequence(currentSeq + 10);
		long myId = ++currentSeq;

		Item item = new Item();
		item.uid = "test_" + myId;
		item.id = myId;
		item.displayName = "test";
		try {
			item = home.create(item);
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		item = home.get(item.uid);
		assertEquals(item.id, myId);
	}

	@Test
	public void testCreateByIdUsingIdGreaterThanSequenceShouldFail() throws Exception {
		long currentSeq = currentId();
		long myId = currentSeq + 1;

		Item item = new Item();
		item.uid = "test_" + myId;
		item.id = myId;
		item.displayName = "test";
		try {
			item = home.create(item);
			fail();
		} catch (Exception e) {

		}
	}

	@Test
	public void testCreateWithItem() throws SQLException, ParseException {
		Item item = new Item();
		item.id = 73;
		item.uid = "test_" + System.nanoTime();
		item.externalId = "externalId" + System.nanoTime();
		item.displayName = "test";
		item.created = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:44:21");
		item.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		item.version = 17;
		try {
			home.create(item);
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		Item result = home.get(item.uid);
		assertEquals(item.id, result.id);
		assertEquals(item.uid, result.uid);
		assertEquals(item.externalId, result.externalId);
		assertEquals(item.displayName, result.displayName);
		assertEquals(item.created, result.created);
		assertEquals(item.version, result.version);
	}

	@Test
	public void testUpdateWithItem() throws SQLException, ParseException {
		Item item = new Item();
		item.id = 73;
		item.uid = "test_" + System.nanoTime();
		item.externalId = "externalId" + System.nanoTime();
		item.displayName = "test";
		item.created = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:44:21");
		item.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 11:46:00");
		item.version = 17;
		try {
			home.create(item);
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Item created = home.get(item.uid);
		created.version = item.version + 1;
		created.updated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2021-07-26 12:46:00");
		try {
			home.update(created, created.displayName, created.flags);
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Item updated = home.get(created.uid);
		assertEquals(created.id, updated.id);
		assertEquals(created.uid, updated.uid);
		assertEquals(created.externalId, updated.externalId);
		assertEquals(created.displayName, updated.displayName);
		assertEquals(created.updated, updated.updated);
		assertEquals(created.version, updated.version);
	}

	private long currentId() throws Exception {
		String sql = "select last_value from t_container_item_id_seq";
		try (Connection con = JdbcTestHelper.getInstance().getDataSource().getConnection();
				Statement stm = con.createStatement();
				ResultSet rs = stm.executeQuery(sql)) {
			rs.next();
			return rs.getLong(1);
		}
	}

	private void restartSequence(long value) throws Exception {
		String sql = "ALTER SEQUENCE t_container_item_id_seq RESTART with " + value;
		try (Connection con = JdbcTestHelper.getInstance().getDataSource().getConnection();
				Statement stm = con.createStatement()) {
			stm.execute(sql);
		}
	}

}
