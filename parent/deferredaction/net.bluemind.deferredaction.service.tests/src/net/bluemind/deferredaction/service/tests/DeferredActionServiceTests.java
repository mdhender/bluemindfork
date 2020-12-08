/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.deferredaction.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.deferredaction.api.DeferredAction;
import net.bluemind.deferredaction.api.IDeferredAction;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DeferredActionServiceTests {
	IDeferredAction service;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(evt -> launched.countDown());
		launched.await();
		PopulateHelper.initGlobalVirt();
		String domainUid = "dom" + System.currentTimeMillis() + ".test";
		PopulateHelper.addDomain(domainUid);
		Thread.sleep(2000);
		service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDeferredAction.class,
				IDeferredActionContainerUids.uidForDomain(domainUid));
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	private DeferredAction createFixture() {
		return createFixture("actionid");
	}

	private DeferredAction createFixture(String actionId) {
		DeferredAction expected = new DeferredAction();
		expected.actionId = actionId;
		expected.executionDate = new Date();
		expected.reference = "reference";
		expected.configuration = new HashMap<String, String>();
		expected.configuration.put("key1", "value1");
		return expected;
	}

	@Test
	public void getVersionDeferredAction() {
		Long version = service.getVersion();
		System.err.println(version);
	}

	@Test
	public void getbackDeferredAction() {
		DeferredAction expected = createFixture();
		String sampleUID = "uid";
		service.create(sampleUID, expected);
		ItemValue<DeferredAction> actual = service.getComplete(sampleUID);
		assertEquals(expected.actionId, actual.value.actionId);
		assertEquals(expected.executionDate, actual.value.executionDate);
		assertEquals(expected.reference, actual.value.reference);
		assertEquals(expected.configuration, actual.value.configuration);
	}

	@Test
	public void updateDeferredAction() {
		DeferredAction expected = createFixture();
		String sampleUID = "uid";
		service.create(sampleUID, expected);
		expected.reference = "updatedreference";
		ItemValue<DeferredAction> notUpdated = service.getComplete(sampleUID);
		assertNotEquals(expected.reference, notUpdated.value.reference);
		service.update(sampleUID, expected);
		ItemValue<DeferredAction> updated = service.getComplete(sampleUID);
		assertEquals(expected.reference, updated.value.reference);
	}

	@Test
	public void deleteDeferredAction() {
		DeferredAction expected = createFixture();
		String sampleUID = "uid";
		service.create(sampleUID, expected);
		assertNotNull(service.getComplete(sampleUID));
		service.delete(sampleUID);
		assertNull(service.getComplete(sampleUID));
	}

	@Test
	public void deleteAllDeferredAction() {
		DeferredAction expected = createFixture();
		for (int i = 0; i < 10; i++) {
			service.create("uid" + i, expected);
		}
		for (int i = 0; i < 10; i++) {
			assertNotNull(service.getComplete("uid" + i));
		}
		service.deleteAll();
		for (int i = 0; i < 10; i++) {
			assertNull(service.getComplete("uid" + i));
		}
	}

	@Test
	public void getByActionIdDeferredAction() {
		String actionId = "MyActionId";
		DeferredAction expected = createFixture(actionId);
		String uid = "uid";
		service.create(uid, expected);
		List<ItemValue<DeferredAction>> actuals = service.getByActionId(actionId, new Date().getTime());
		assertEquals(1, actuals.size());
		actuals.forEach(actual -> {
			assertEquals(expected.actionId, actual.value.actionId);
			assertEquals(uid, actual.uid);
		});
	}

	@Test
	public void getByReferenceDeferredAction() {
		String actionId = "actionId";
		String reference = "sameReference";
		for (int i = 0; i < 10; i++) {
			DeferredAction sameRef = createFixture(actionId);
			sameRef.reference = reference;
			service.create("uid-" + i, sameRef);
		}
		DeferredAction anotherRef = createFixture(actionId);
		anotherRef.reference = "anotherReference";
		service.create("uid-11", anotherRef);

		List<ItemValue<DeferredAction>> allActionId = service.getByActionId(actionId, new Date().getTime());
		assertEquals(11, allActionId.size());
		List<ItemValue<DeferredAction>> actuals = service.getByReference(reference);
		assertEquals(10, actuals.size());
		actuals.forEach(actual -> {
			assertEquals(reference, actual.value.reference);
		});
	}

	@Test
	public void multipleGetDeferredAction() {
		String actionId = "actionId";
		String reference = "sameReference";
		List<String> uids = new ArrayList<String>();
		for (int i = 0; i < 10; i++) {
			DeferredAction sameRef = createFixture(actionId);
			sameRef.reference = reference;
			String uid = "uid-" + i;
			service.create(uid, sameRef);
			uids.add(uid);
		}
		List<ItemValue<DeferredAction>> actuals = service.multipleGet(uids);
		assertEquals(10, actuals.size());
	}
}
