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
package net.bluemind.core.container.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class OfflineMgmtServiceTests {

	private SecurityContext user;
	private String domainUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		PopulateHelper.initGlobalVirt();
		domainUid = "bmtest.lan";
		PopulateHelper.createTestDomain(domainUid);

		user = new SecurityContext("testSessionId", "test", Arrays.<String>asList(), Arrays.<String>asList(),
				domainUid);

		Sessions.get().put(user.getSessionId(), user);

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IOfflineMgmt getService(SecurityContext securityContext) throws ServerFault {
		return new OfflineMgmtService(new BmTestContext(securityContext), securityContext.getSubject(),
				securityContext.getContainerUid());
	}

	@Test
	public void testGetService() {
		IOfflineMgmt service = getService(user);
		assertNotNull(service);
	}

	@Test
	public void testAllocateIds() {
		IOfflineMgmt service = getService(user);
		IdRange range = service.allocateOfflineIds(2);
		assertNotNull(range);
		long start = range.globalCounter;
		IdRange secondRange = service.allocateOfflineIds(2);
		System.out.println("start: " + start + " " + (start + range.count) + ", next: " + secondRange.globalCounter
				+ ", " + (secondRange.globalCounter - range.globalCounter));
		assertEquals(secondRange.globalCounter, start + range.count);
	}

}
