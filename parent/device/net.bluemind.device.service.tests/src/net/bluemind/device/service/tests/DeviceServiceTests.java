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
package net.bluemind.device.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.device.api.Device;
import net.bluemind.device.api.IDevice;
import net.bluemind.device.api.IDevices;
import net.bluemind.device.api.WipeMode;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DeviceServiceTests {

	private String userUid;
	protected SecurityContext context;
	protected SecurityContext system;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		PopulateHelper.initGlobalVirt();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		context = new SecurityContext("admin", "admin_bm.lan", Arrays.<String>asList(),
				Arrays.<String>asList(BasicRoles.ROLE_MANAGE_USER_DEVICE), "bm.lan");
		Sessions.get().put(context.getSessionId(), context);

		system = new SecurityContext("system", "system", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_SYSTEM), "global.virt");
		Sessions.get().put(system.getSessionId(), system);

		PopulateHelper.createTestDomain("bm.lan");
		PopulateHelper.addDomainAdmin("admin", "bm.lan");

		userUid = "admin";

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	public IDevices getDevicesService(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(IDevices.class);
	}

	public IDevice getDeviceService(SecurityContext context, String userUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IDevice.class, userUid);

	}

	@Test
	public void testCreate() throws ServerFault {
		Device device = defaultDevice();
		String uid = "test_" + System.nanoTime();

		try {
			getDeviceService(SecurityContext.ANONYMOUS, userUid).create(uid, device);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getDeviceService(context, userUid).create(uid, device);

		assertNotNull(getDeviceService(context, userUid).getComplete(uid));
		assertNotNull(getDeviceService(context, userUid).byIdentifier(device.identifier));
	}

	@Test
	public void testDelete() throws ServerFault {
		Device device = defaultDevice();
		String uid = "test_" + System.nanoTime();

		IDevice service = getDeviceService(context, userUid);

		service.create(uid, device);

		try {
			getDeviceService(SecurityContext.ANONYMOUS, userUid).delete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		service.delete(uid);
	}

	@Test
	public void testDeleteAll() throws ServerFault {
		IDevice service = getDeviceService(context, userUid);
		ListResult<ItemValue<Device>> list = service.list();

		assertEquals(0, list.total);

		service.create(UUID.randomUUID().toString(), defaultDevice());
		service.create(UUID.randomUUID().toString(), defaultDevice());

		list = service.list();
		assertEquals(2, list.total);

		service.deleteAll();

		list = service.list();
		assertEquals(0, list.total);
	}

	@Test
	public void testUpdate() throws ServerFault {
		Device device = defaultDevice();
		String uid = "test_" + System.nanoTime();

		getDeviceService(context, userUid).create(uid, device);

		ItemValue<Device> item = getDeviceService(context, userUid).getComplete(uid);
		assertFalse(item.value.hasPartnership);

		Device dev = item.value;
		dev.hasPartnership = true;

		try {
			getDeviceService(SecurityContext.ANONYMOUS, userUid).update(uid, dev);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getDeviceService(context, userUid).update(uid, dev);

		item = getDeviceService(context, userUid).getComplete(uid);

		assertTrue(item.value.hasPartnership);

	}

	@Test
	public void testList() throws ServerFault {

		ListResult<ItemValue<Device>> list = getDeviceService(context, userUid).list();
		assertNotNull(list);
		assertEquals(0, list.total);

		Device device = defaultDevice();
		String uid = "test_" + System.nanoTime();

		getDeviceService(context, userUid).create(uid, device);

		try {
			getDeviceService(SecurityContext.ANONYMOUS, userUid).list();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		list = getDeviceService(context, userUid).list();
		assertNotNull(list);
		assertEquals(1, list.total);

		device = defaultDevice();
		uid = "test_" + System.nanoTime();

		getDeviceService(context, userUid).create(uid, device);
		list = getDeviceService(context, userUid).list();
		assertNotNull(list);
		assertEquals(2, list.total);
	}

	@Test
	public void testWipe() throws ServerFault {
		List<Device> wiped = getDevicesService(system).listWiped();

		try {
			wiped = getDevicesService(context).listWiped();
			fail();
		} catch (Exception e) {

		}

		assertEquals(0, wiped.size());

		Device device = defaultDevice();
		String uid = "test_" + System.nanoTime();

		getDeviceService(context, userUid).create(uid, device);

		// WIPE
		try {
			getDeviceService(SecurityContext.ANONYMOUS, userUid).wipe(uid, WipeMode.RemoteWipe);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		// WIPE using version < 16.1
		device.protocolVersion = 14d;
		getDeviceService(context, userUid).update(uid, device);
		try {
			getDeviceService(context, userUid).wipe(uid, WipeMode.AccountOnlyRemoteWipe);
			fail();
		} catch (ServerFault e) {

		}
		device.protocolVersion = 16.1d;
		getDeviceService(context, userUid).update(uid, device);

		getDeviceService(context, userUid).wipe(uid, WipeMode.AccountOnlyRemoteWipe);

		ItemValue<Device> item = getDeviceService(context, userUid).getComplete(uid);

		assertTrue(item.value.isWiped);
		assertEquals(WipeMode.AccountOnlyRemoteWipe, item.value.wipeMode);
		assertNotNull(item.value.wipeBy);
		assertNotNull(item.value.wipeDate);
		assertNull(item.value.unwipeBy);
		assertNull(item.value.unwipeDate);

		wiped = getDevicesService(system).listWiped();
		assertEquals(1, wiped.size());

		// UNWIPE
		try {
			getDeviceService(SecurityContext.ANONYMOUS, userUid).unwipe(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
		getDeviceService(context, userUid).unwipe(uid);

		item = getDeviceService(context, userUid).getComplete(uid);

		assertFalse(item.value.isWiped);
		assertNull(item.value.wipeMode);
		assertNotNull(item.value.wipeBy);
		assertNotNull(item.value.wipeDate);
		assertNotNull(item.value.unwipeBy);
		assertNotNull(item.value.unwipeDate);
	}

	@Test
	public void testPartenership() throws ServerFault {

		Device device = defaultDevice();
		String uid = "test_" + System.nanoTime();

		getDeviceService(context, userUid).create(uid, device);

		// set
		try {
			getDeviceService(SecurityContext.ANONYMOUS, userUid).setPartnership(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
		getDeviceService(context, userUid).setPartnership(uid);

		ItemValue<Device> item = getDeviceService(context, userUid).getComplete(uid);

		assertTrue(item.value.hasPartnership);
		// unset
		try {
			getDeviceService(SecurityContext.ANONYMOUS, userUid).unsetPartnership(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
		getDeviceService(context, userUid).unsetPartnership(uid);

		item = getDeviceService(context, userUid).getComplete(uid);

		assertFalse(item.value.hasPartnership);
	}

	@Test
	public void testUpdateLastSync() throws ServerFault {
		Device device = defaultDevice();
		String uid = "test_" + System.nanoTime();

		getDeviceService(context, userUid).create(uid, device);

		ItemValue<Device> item = getDeviceService(context, userUid).getComplete(uid);
		assertNull(item.value.lastSync);

		Device dev = item.value;
		dev.lastSync = new Date();

		getDeviceService(context, userUid).update(uid, dev);

		item = getDeviceService(context, userUid).getComplete(uid);

		assertNotNull(item.value.lastSync);

	}

	@Test
	public void testGetWindowsPhoneByIdentifier() {
		String identifier = "4ze7Izo+DijxN/3xIMvepQ==";

		Device device = defaultDevice();
		device.identifier = identifier;
		String uid = "WP" + System.nanoTime();

		getDeviceService(context, userUid).create(uid, device);

		assertNotNull(getDeviceService(context, userUid).getComplete(uid));
		assertNotNull(getDeviceService(context, userUid).byIdentifier(device.identifier));
	}

	private Device defaultDevice() {
		Device ret = new Device();
		ret.identifier = "android" + UUID.randomUUID().toString();
		ret.type = "Android";
		ret.owner = UUID.randomUUID().toString();
		ret.protocolVersion = 16.1;

		return ret;
	}

}
