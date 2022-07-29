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
package net.bluemind.eas.persistence.tests.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.eas.api.Account;
import net.bluemind.eas.api.Heartbeat;
import net.bluemind.eas.persistence.EasStore;

public class EasStoreTests {

	public EasStore store;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		store = new EasStore(JdbcTestHelper.getInstance().getDataSource());

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testHeartbeat() throws Exception {
		String deviceUid = UUID.randomUUID().toString();
		Heartbeat heartbeat = store.getHeartbeat(deviceUid);
		assertNull(heartbeat);

		heartbeat = new Heartbeat();
		heartbeat.deviceUid = deviceUid;
		heartbeat.value = Long.valueOf(1);

		Heartbeat heartbeat2 = new Heartbeat();
		heartbeat2.deviceUid = "device2";
		heartbeat2.value = Long.valueOf(2);
		store.setHeartbeat(heartbeat);
		// insert
		store.setHeartbeat(heartbeat2);
		// update
		store.setHeartbeat(heartbeat2);

		// device is not impacted by update of device2
		heartbeat = store.getHeartbeat(deviceUid);
		assertEquals(deviceUid, heartbeat.deviceUid);
		assertEquals(Long.valueOf(1), heartbeat.value);

		heartbeat.value = Long.valueOf(42);
		store.setHeartbeat(heartbeat);
		heartbeat = store.getHeartbeat(deviceUid);
		assertEquals(deviceUid, heartbeat.deviceUid);
		assertEquals(Long.valueOf(42), heartbeat.value);
	}

	@Test
	public void testReset() throws Exception {
		Account account = Account.create("david@bm.lan", "device");
		assertFalse(store.needReset(account));
		store.insertPendingReset(account);
		assertTrue(store.needReset(account));
		store.deletePendingReset(account);
		assertFalse(store.needReset(account));
	}

	@Test
	public void testClientIds() throws SQLException {
		String cid = "cid-junit-" + System.currentTimeMillis();
		boolean exists = store.isKnownClientId(cid);
		assertFalse(exists);
		store.insertClientId(cid);
		exists = store.isKnownClientId(cid);
		assertTrue(exists);
	}

	// Folder Sync
	@Test
	public void testFolderSync() throws Exception {
		Account account = Account.create("david@bm.lan", "device");
		assertNull(store.getFolderSyncVersions(account));

		Map<String, String> versions = new HashMap<String, String>();
		versions.put("root", "4");
		versions.put("mailbox1", "42");
		versions.put("mailbox2", "24");
		versions.put("mailbox34", "13");

		store.setFolderSyncVersions(account, versions);

		Map<String, String> fs = store.getFolderSyncVersions(account);
		assertNotNull(fs);
		assertEquals(4, fs.size());
		assertEquals("42", fs.get("mailbox1"));
		assertEquals("24", fs.get("mailbox2"));
		assertEquals("13", fs.get("mailbox34"));

		fs.put("mailbox3", "3");
		fs.put("mailbox34", "333");
		store.setFolderSyncVersions(account, fs);

		fs = store.getFolderSyncVersions(account);

		assertNotNull(fs);
		assertEquals(5, fs.size());
		assertEquals("42", fs.get("mailbox1"));
		assertEquals("24", fs.get("mailbox2"));
		assertEquals("3", fs.get("mailbox3"));
		assertEquals("333", fs.get("mailbox34"));
	}

}
