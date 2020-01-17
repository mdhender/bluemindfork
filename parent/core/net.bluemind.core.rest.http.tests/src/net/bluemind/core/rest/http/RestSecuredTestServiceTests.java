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
package net.bluemind.core.rest.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.tests.services.IRestSecuredTestService;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.lib.vertx.VertxPlatform;

public class RestSecuredTestServiceTests {
	private AsyncHttpClient httpClient;

	private SecurityContext masterSC = new SecurityContext("master", "master", Arrays.<String>asList(),
			Arrays.asList("master", "role1"), "gg");

	private SecurityContext slaveSC = new SecurityContext("slave", "slave", Arrays.<String>asList(),
			Arrays.asList("slave", "role1"), "gg");

	private SecurityContext simpelSC = new SecurityContext("simple", "simple", Arrays.<String>asList(),
			Arrays.asList("role1"), "gg");

	@Before
	public void setup() throws Exception {

		VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);

		httpClient = new DefaultAsyncHttpClient();
		Sessions.get().put(masterSC.getSessionId(), masterSC);
		Sessions.get().put(slaveSC.getSessionId(), slaveSC);
		Sessions.get().put(simpelSC.getSessionId(), simpelSC);
	}

	@After
	public void after() throws Exception {
		httpClient.close();
	}

	public IRestSecuredTestService getRestSecuredTestServide(SecurityContext context) {
		return HttpClientFactory.create(IRestSecuredTestService.class, null, "http://localhost:8090", httpClient)
				.syncClient(context.getSessionId());
	}

	@Test
	public void testMaster() {
		try {
			getRestSecuredTestServide(masterSC).helloMaster();
		} catch (Exception e) {
			fail(e.getMessage());
		}

		try {
			getRestSecuredTestServide(slaveSC).helloMaster();
			fail("should not succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.FORBIDDEN, e.getCode());
		}

		try {
			getRestSecuredTestServide(simpelSC).helloMaster();
			fail("should not succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.FORBIDDEN, e.getCode());
		}
	}

	@Test
	public void testSlave() {
		try {
			getRestSecuredTestServide(slaveSC).helloSlave();
		} catch (Exception e) {
			fail(e.getMessage());
		}

		try {
			getRestSecuredTestServide(masterSC).helloSlave();
			fail("should not succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.FORBIDDEN, e.getCode());
		}

		try {
			getRestSecuredTestServide(simpelSC).helloSlave();
			fail("should not succeed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.FORBIDDEN, e.getCode());
		}
	}

	@Test
	public void testSimple() {
		try {
			getRestSecuredTestServide(slaveSC).helloSimple();
		} catch (Exception e) {
			fail(e.getMessage());
		}

		try {
			getRestSecuredTestServide(masterSC).helloSimple();

		} catch (ServerFault e) {
			fail(e.getMessage());
		}

		try {
			getRestSecuredTestServide(simpelSC).helloSimple();

		} catch (ServerFault e) {
			fail(e.getMessage());
		}

		try {
			getRestSecuredTestServide(SecurityContext.ANONYMOUS).helloSimple();
			fail("should not suceed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.FORBIDDEN, e.getCode());
		}

	}
}
