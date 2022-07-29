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
package net.bluemind.eas.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.eas.api.Account;
import net.bluemind.eas.api.FolderSyncVersions;
import net.bluemind.eas.api.Heartbeat;
import net.bluemind.eas.api.IEas;
import net.bluemind.lib.vertx.VertxPlatform;

public class EasServiceTests {

	private SecurityContext adminSC;
	private SecurityContext userSC;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		adminSC = new SecurityContext(UUID.randomUUID().toString(), "system", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_SYSTEM), Collections.emptyMap(), "global.virt", "en",
				"junit");
		Sessions.get().put(adminSC.getSessionId(), adminSC);

		userSC = new SecurityContext(UUID.randomUUID().toString(), "user", Arrays.<String>asList(),
				Arrays.<String>asList(), Collections.emptyMap(), "bm.lan", "en", "junit");
		Sessions.get().put(userSC.getSessionId(), userSC);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	public IEas getService(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(IEas.class);
	}

	@Test
	public void testClientIds() throws ServerFault {
		IEas store = getService(adminSC);
		String cid = "cid-junit-" + System.currentTimeMillis();
		boolean exists = store.isKnownClientId(cid);
		assertFalse(exists);
		store.insertClientId(cid);
		exists = store.isKnownClientId(cid);
		assertTrue(exists);

		try {
			getService(userSC).insertClientId(cid);
			fail();
		} catch (Exception e) {

		}
	}

	@Test
	public void testHeartbeat() throws ServerFault {
		String deviceUid = UUID.randomUUID().toString();
		Heartbeat heartbeat = getService(adminSC).getHeartbeat(deviceUid);

		assertNull(heartbeat);
		heartbeat = new Heartbeat();
		heartbeat.deviceUid = deviceUid;
		heartbeat.value = Long.valueOf(1);

		getService(adminSC).setHeartbeat(heartbeat);
		heartbeat = getService(adminSC).getHeartbeat(deviceUid);
		assertNotNull(heartbeat);
		assertEquals(deviceUid, heartbeat.deviceUid);
		assertEquals(Long.valueOf(1), heartbeat.value);

		heartbeat.value = Long.valueOf(42);
		getService(adminSC).setHeartbeat(heartbeat);
		heartbeat = getService(adminSC).getHeartbeat(deviceUid);
		assertEquals(deviceUid, heartbeat.deviceUid);
		assertEquals(Long.valueOf(42), heartbeat.value);

		try {
			getService(userSC).setHeartbeat(heartbeat);
			fail();
		} catch (Exception e) {
		}
		try {
			getService(userSC).getHeartbeat(heartbeat.deviceUid);
			fail();
		} catch (Exception e) {
		}
	}

	@Test
	public void testFolderState() throws ServerFault {
		IEas service = getService(adminSC);
		Account account = Account.create("david@bm.lan", "device");
		assertFalse(service.needReset(account));

		try {
			getService(userSC).needReset(account);
			fail();
		} catch (Exception e) {
		}
	}

	// FolderSync
	@Test
	public void testFolderSync() throws ServerFault {
		IEas service = getService(adminSC);
		Account account = Account.create("david@bm.lan", "device");

		Map<String, String> versions = new HashMap<String, String>();
		versions.put("root", "4");
		versions.put("mailbox1", "42");
		versions.put("mailbox2", "24");
		versions.put("mailbox34", "13");
		FolderSyncVersions fsv = FolderSyncVersions.create(account, versions);
		service.setFolderSyncVersions(fsv);

		Map<String, String> fs = service.getFolderSyncVersions(account);
		assertNotNull(fs);
		assertEquals(4, fs.size());
		assertEquals("4", fs.get("root"));
		assertEquals("42", fs.get("mailbox1"));
		assertEquals("24", fs.get("mailbox2"));
		assertEquals("13", fs.get("mailbox34"));

		fs.put("mailbox3", "3");
		fs.put("mailbox34", "333");
		fsv.versions = fs;
		service.setFolderSyncVersions(fsv);

		fs = service.getFolderSyncVersions(account);

		assertNotNull(fs);
		assertEquals(5, fs.size());
		assertEquals("4", fs.get("root"));
		assertEquals("42", fs.get("mailbox1"));
		assertEquals("24", fs.get("mailbox2"));
		assertEquals("3", fs.get("mailbox3"));
		assertEquals("333", fs.get("mailbox34"));

		try {
			getService(userSC).setFolderSyncVersions(fsv);
			fail();
		} catch (Exception e) {
		}

		try {
			getService(userSC).getFolderSyncVersions(account);
			fail();
		} catch (Exception e) {
		}

	}
}
