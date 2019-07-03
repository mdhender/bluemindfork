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
package net.bluemind.authentication.mgmt.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.authentication.mgmt.api.ISessionsMgmt;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class SessionsMgmtTests {

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		PopulateHelper.initGlobalVirt();

		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		PopulateHelper.createTestDomain("bm.lan");
		PopulateHelper.addDomainAdmin("admin", "bm.lan", Routing.none);
		PopulateHelper.addUser("toto", "bm.lan", Routing.none);
		PopulateHelper.addUser("simple", "bm.lan", Routing.none);

		StateContext.setState("reset");
		StateContext.setState("core.started");
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	private void initState() {
		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
	}

	private IAuthentication getAutenticationService(String sessionId) throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", sessionId)
				.instance(IAuthentication.class);
	}

	private ISessionsMgmt getSessionsMgmtService(String sessionId) throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", sessionId).instance(ISessionsMgmt.class);
	}

	@Test
	public void testLogoutLatd() throws Exception {
		initState();
		IAuthentication authentication = getAutenticationService(null);
		LoginResponse response1 = authentication.login("admin@bm.lan", "admin", "junit");
		assertEquals(Status.Ok, response1.status);

		LoginResponse response2 = authentication.login("admin@bm.lan", "admin", "junit");
		assertEquals(Status.Ok, response2.status);

		LoginResponse response3 = authentication.login("admin0@global.virt", "admin", "junit");
		assertEquals(Status.Ok, response3.status);

		assertEquals(Status.Ok, authentication.login("admin@bm.lan", response1.authKey, "junit").status);
		assertEquals(Status.Ok, authentication.login("admin@bm.lan", response2.authKey, "junit").status);
		assertEquals(Status.Ok, authentication.login("admin0@global.virt", response3.authKey, "junit").status);

		try {
			getSessionsMgmtService(null).logoutUser("admin@bm.lan");
			fail("Test must throw an exception");
		} catch (ServerFault sf) {
			sf.printStackTrace();
			assertEquals(ErrorCode.PERMISSION_DENIED, sf.getCode());
		}

		try {
			ISessionsMgmt sessionsMgmt = getSessionsMgmtService(
					authentication.login("toto@bm.lan", "toto", "junit").authKey);
			sessionsMgmt.logoutUser("admin@bm.lan");
			fail("Test must throw an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.PERMISSION_DENIED, sf.getCode());
			assertTrue(sf.getMessage().contains("toto@bm.lan"));
		}

		try {
			ISessionsMgmt sessionsMgmt = getSessionsMgmtService(
					authentication.login("admin@bm.lan", "admin", "junit").authKey);
			sessionsMgmt.logoutUser("admin@bm.lan");
			fail("Test must throw an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.PERMISSION_DENIED, sf.getCode());
			assertTrue(sf.getMessage().contains("admin@bm.lan"));
		}

		ISessionsMgmt sessionsMgmt = getSessionsMgmtService(
				authentication.login("admin0@global.virt", "admin", "junit").authKey);
		sessionsMgmt.logoutUser("admin@bm.lan");
		assertEquals(Status.Bad, authentication.login("admin@bm.lan", response1.authKey, "junit").status);
		assertEquals(Status.Bad, authentication.login("admin@bm.lan", response2.authKey, "junit").status);
		assertEquals(Status.Ok, authentication.login("admin0@global.virt", response3.authKey, "junit").status);
	}
}
