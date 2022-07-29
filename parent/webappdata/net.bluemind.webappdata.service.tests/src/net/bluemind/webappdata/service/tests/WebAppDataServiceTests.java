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
package net.bluemind.webappdata.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.webappdata.api.IWebAppData;
import net.bluemind.webappdata.api.IWebAppDataUids;
import net.bluemind.webappdata.api.WebAppData;

public class WebAppDataServiceTests {

	private String domain = "bm.lan";
	private String userUid = "user1";
	private String containerUid = IWebAppDataUids.containerUid(userUid);
	private SecurityContext context = new SecurityContext("testUser", userUid, Arrays.<String>asList(),
			Arrays.<String>asList(), domain);

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		PopulateHelper.initGlobalVirt();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		PopulateHelper.createTestDomain(domain);
		PopulateHelper.addUser(userUid, domain);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	public IWebAppData getAnonymousService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.ANONYMOUS).instance(IWebAppData.class,
				containerUid);
	}

	public IWebAppData getService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IWebAppData.class, containerUid);
	}

	@Test
	public void testNoDuplicateKeyForSameUser() throws ServerFault {
		String uid = "test_" + System.nanoTime();
		WebAppData webAppData = WebAppDataUserHookTests.exampleWebAppData();
		getService().create(uid, webAppData);

		String uid2 = "test_" + System.nanoTime();
		try {
			getService().create(uid2, webAppData);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.ALREADY_EXISTS, e.getCode());
		}

		WebAppData webAppData2 = new WebAppData();
		webAppData2.key = "mail-app:my_feat:au pif";
		webAppData2.value = "plop";
		getService().create(uid2, webAppData2);

		webAppData2.key = webAppData.key;
		try {
			getService().update(uid2, webAppData2);
			fail();
		} catch (ServerFault e) {
			// can't have duplicate key with update because not authorized to change key for
			// a same item
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}
	}

	@Test
	public void testKeyOrWebAppDataCantBeNull() throws ServerFault {
		String uid = "test_" + System.nanoTime();
		WebAppData webAppData = new WebAppData();

		try {
			getService().create(uid, webAppData);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		try {
			getService().create(uid, null);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}
	}

	@Test
	public void testKeyNameValidity() throws ServerFault {
		WebAppData webAppData = new WebAppData();
		webAppData.key = "blabla";

		try {
			getService().create("test_" + System.nanoTime(), webAppData);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		try {
			webAppData.key = "one-app:butnofeat";
			getService().create("test_" + System.nanoTime(), webAppData);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		webAppData.key = "mail-app:my_feat:dezdez";
		getService().create("test_" + System.nanoTime(), webAppData);
	}

	@Test
	public void testCreate() throws ServerFault {
		String uid = "test_" + System.nanoTime();
		WebAppData webAppData = WebAppDataUserHookTests.exampleWebAppData();

		try {
			getAnonymousService().create(uid, webAppData);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService().create(uid, webAppData);
		assertNotNull(getService().getComplete(uid));
	}

	@Test
	public void testCreateWithSameItemUid() throws ServerFault {
		String uid = "test_" + System.nanoTime();
		WebAppData webAppData = WebAppDataUserHookTests.exampleWebAppData();
		getService().create(uid, webAppData);

		String oldValue = webAppData.value;
		webAppData.value = "recreated";
		try {
			getService().create(uid, webAppData);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.ALREADY_EXISTS, e.getCode());
		}
		assertEquals(getService().get(uid).value, oldValue);
	}

	@Test
	public void testDelete() throws ServerFault {
		String uid = "test_" + System.nanoTime();
		WebAppData webAppData = WebAppDataUserHookTests.exampleWebAppData();
		getService().create(uid, webAppData);

		try {
			getAnonymousService().delete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService().delete(uid);
		assertNull(getService().getComplete(uid));

		try {
			getService().delete("not-existing-uid");
			fail();
		} catch (ServerFault e) {
			// deleting an unknown uid fails
		}
	}

	@Test
	public void testDeleteAll() throws ServerFault {
		WebAppData webAppData = WebAppDataUserHookTests.exampleWebAppData();
		getService().create("test_" + System.nanoTime(), webAppData);
		webAppData.key = "mail:zone:feat";
		getService().create("test_" + System.nanoTime(), webAppData);

		try {
			getAnonymousService().deleteAll();
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService().deleteAll();
		assertEquals(getService().allUids().size(), 0);
	}

	@Test
	public void testUpdate() throws ServerFault {
		String uid = "test_" + System.nanoTime();
		WebAppData webAppData = WebAppDataUserHookTests.exampleWebAppData();
		getService().create(uid, webAppData);

		String oldValue = webAppData.value;
		webAppData.value = "updated";

		try {
			getAnonymousService().update(uid, webAppData);
			fail();
		} catch (ServerFault e) {
			assertEquals(getService().get(uid).value, oldValue);
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		getService().update(uid, webAppData);
		assertEquals(getService().get(uid).value, "updated");

		try {
			webAppData.key = "mail:feat:changed";
			getService().update(uid, webAppData);
			fail();
		} catch (ServerFault e) {
			// can't change key for a same item uid
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		// updating an unknown uid fallback on create
		webAppData.key = "mail-app:my_feat:another-one";
		getService().update("not-existing-uid", webAppData);
		assertNotNull(getService().get("not-existing-uid"));
	}

	@Test
	public void testGet() throws ServerFault {
		String uid = "test_" + System.nanoTime();
		WebAppData webAppData = WebAppDataUserHookTests.exampleWebAppData();
		getService().create(uid, webAppData);

		try {
			getAnonymousService().get(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		WebAppData retrieved = getService().get(uid);
		assertEquals(retrieved.key, webAppData.key);
		assertEquals(retrieved.value, webAppData.value);
	}

	@Test
	public void testGetComplete() throws ServerFault {
		String uid = "test_" + System.nanoTime();
		WebAppData webAppData = WebAppDataUserHookTests.exampleWebAppData();
		getService().create(uid, webAppData);

		try {
			getAnonymousService().getComplete(uid);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ItemValue<WebAppData> retrieved = getService().getComplete(uid);
		assertEquals(retrieved.value.key, webAppData.key);
		assertEquals(retrieved.value.value, webAppData.value);
	}

	@Test
	public void testGetCompleteById() throws ServerFault {
		String uid = "test_" + System.nanoTime();
		WebAppData webAppData = WebAppDataUserHookTests.exampleWebAppData();
		getService().create(uid, webAppData);
		long itemId = getService().getComplete(uid).internalId;

		try {
			getAnonymousService().getCompleteById(itemId);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		ItemValue<WebAppData> retrieved = getService().getCompleteById(itemId);
		assertEquals(retrieved.value.key, webAppData.key);
		assertEquals(retrieved.value.value, webAppData.value);
	}

	@Test
	public void testMultipleGet() throws ServerFault {
		String uid = "test_" + System.nanoTime();
		WebAppData webAppData = WebAppDataUserHookTests.exampleWebAppData();
		getService().create(uid, webAppData);

		String uid2 = "test_" + System.nanoTime();
		WebAppData webAppData2 = new WebAppData();
		webAppData2.key = "mail-app:my_feat:another";
		getService().create(uid2, webAppData2);

		List<String> uids = Arrays.asList(uid, uid2);

		try {
			getAnonymousService().multipleGet(uids);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		List<ItemValue<WebAppData>> retrieved = getService().multipleGet(uids);
		assertEquals(retrieved.get(0).value.key, webAppData.key);
		assertEquals(retrieved.get(0).value.value, webAppData.value);
		assertEquals(retrieved.get(1).value.key, webAppData2.key);
		assertEquals(retrieved.get(1).value.value, webAppData2.value);
	}

	@Test
	public void testMultipleGetById() throws ServerFault {
		String uid = "test_" + System.nanoTime();
		WebAppData webAppData = WebAppDataUserHookTests.exampleWebAppData();
		getService().create(uid, webAppData);

		String uid2 = "test_" + System.nanoTime();
		WebAppData webAppData2 = new WebAppData();
		webAppData2.key = "mail-app:my_feat:another";
		getService().create(uid2, webAppData2);

		List<Long> itemIds = Arrays.asList(getService().getComplete(uid).internalId,
				getService().getComplete(uid2).internalId);

		try {
			getAnonymousService().multipleGetById(itemIds);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		List<ItemValue<WebAppData>> retrieved = getService().multipleGetById(itemIds);
		assertEquals(retrieved.get(0).value.key, webAppData.key);
		assertEquals(retrieved.get(0).value.value, webAppData.value);
		assertEquals(retrieved.get(1).value.key, webAppData2.key);
		assertEquals(retrieved.get(1).value.value, webAppData2.value);
	}

	@Test
	public void testGetByKey() throws ServerFault {
		String uid = "test_" + System.nanoTime();
		WebAppData webAppData = WebAppDataUserHookTests.exampleWebAppData();
		getService().create(uid, webAppData);

		try {
			getAnonymousService().getByKey(webAppData.key);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		WebAppData retrieved = getService().getByKey(webAppData.key);
		assertEquals(retrieved.key, webAppData.key);
		assertEquals(retrieved.value, webAppData.value);

		assertNull(getService().getByKey("unknown-key"));
	}

	@Test
	public void testChangeset() throws ServerFault {
		ContainerChangeset<String> changeset = getService().changeset(0L);

		String itemUid = "test_" + System.nanoTime();
		getService().create(itemUid, WebAppDataUserHookTests.exampleWebAppData());

		changeset = getService().changeset(0L);
		assertEquals(changeset.version, 1);
		assertEquals(changeset.created.size(), 1);
		assertEquals(changeset.created.get(0), itemUid);

		getService().delete(itemUid);
		changeset = getService().changeset(1L);
		assertEquals(changeset.version, 2);
		assertEquals(changeset.created.size(), 0); // no created has been done since version 1
		assertEquals(changeset.deleted.size(), 1);
		assertEquals(changeset.deleted.get(0), itemUid);
	}

	@Test
	public void testChangesetById() throws ServerFault {
		ContainerChangeset<Long> changeset = getService().changesetById(0L);

		String itemUid = "test_" + System.nanoTime();
		getService().create(itemUid, WebAppDataUserHookTests.exampleWebAppData());
		Long itemId = getService().getComplete(itemUid).internalId;

		changeset = getService().changesetById(0L);
		assertEquals(changeset.version, 1);
		assertEquals(changeset.created.size(), 1);
		assertEquals(changeset.created.get(0), itemId);

		getService().delete(itemUid);
		changeset = getService().changesetById(1L);
		assertEquals(changeset.version, 2);
		assertEquals(changeset.created.size(), 0); // no created has been done since version 1
		assertEquals(changeset.deleted.size(), 1);
		assertEquals(changeset.deleted.get(0), itemId);
	}

	@Test
	public void testGetVersion() throws ServerFault {
		WebAppData webAppData = WebAppDataUserHookTests.exampleWebAppData();
		assertEquals(getService().getVersion(), 0);
		String uid = "test_" + System.nanoTime();
		getService().create(uid, webAppData);
		getService().update(uid, webAppData);
		getService().delete(uid);
		getService().create("test_" + System.nanoTime(), webAppData);
		webAppData.key = "mail-app:message_list:dezdez";
		getService().create("test_" + System.nanoTime(), webAppData);
		assertEquals(getService().getVersion(), 5);
	}

}
