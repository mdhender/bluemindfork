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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.deferredaction.api.DeferredAction;
import net.bluemind.deferredaction.api.IDeferredAction;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DeferredActionHookTests {

	private String domainUid;

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		domainUid = "dom" + System.currentTimeMillis() + ".test";
		PopulateHelper.initGlobalVirt();
		domainUid = PopulateHelper.createTestDomain(domainUid).uid;

	}

	@After
	public void teardown() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testUserAndDomainActionHook() throws Exception {
		PopulateHelper.addUser("user1", domainUid);

		Thread.sleep(2000);

		IContainers containerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainers.class);
		assertNotNull(containerService.get(IDeferredActionContainerUids.uidForDomain(domainUid)));
		assertNotNull(containerService.get(IDeferredActionContainerUids.uidForUser("user1")));

		IDeferredAction domainActionService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDeferredAction.class, IDeferredActionContainerUids.uidForDomain(domainUid));
		IDeferredAction userActionService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDeferredAction.class, IDeferredActionContainerUids.uidForUser("user1"));

		domainActionService.create("1", deferredAction());
		userActionService.create("2", deferredAction());

		assertNotNull(domainActionService.getComplete("1"));
		assertNotNull(userActionService.getComplete("2"));

		IDomains domainService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM),
				domainService.deleteDomainItems(domainUid));
		domainService.delete(domainUid);

		assertNull(containerService.getIfPresent(IDeferredActionContainerUids.uidForDomain(domainUid)));
		assertNull(containerService.getIfPresent(IDeferredActionContainerUids.uidForUser("user1")));
	}

	private DeferredAction deferredAction() {
		DeferredAction action = new DeferredAction();
		action.actionId = "action";
		action.reference = "ref";
		action.executionDate = new Date();
		action.configuration = new HashMap<>();
		return action;
	}

}
