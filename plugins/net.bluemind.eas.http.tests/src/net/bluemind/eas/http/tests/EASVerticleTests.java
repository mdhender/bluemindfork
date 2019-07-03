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
package net.bluemind.eas.http.tests;

import java.util.Set;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.json.impl.Base64;

import junit.framework.TestCase;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.config.Token;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.device.api.Device;
import net.bluemind.device.api.IDevice;
import net.bluemind.eas.busmods.DeviceValidationVerticle;
import net.bluemind.eas.config.global.GlobalConfig;
import net.bluemind.eas.http.EasHeaders;
import net.bluemind.eas.http.EasUrls;
import net.bluemind.eas.http.internal.EASHttpVerticle;
import net.bluemind.eas.http.tests.mocks.DummyEndpoint;
import net.bluemind.eas.http.tests.mocks.DummyFilter1;
import net.bluemind.eas.http.tests.mocks.DummyFilter2;
import net.bluemind.eas.http.tests.vertx.TestResponseHandler;
import net.bluemind.eas.testhelper.vertx.Deploy;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class EASVerticleTests extends TestCase {

	private Set<String> deploymentIDs;

	private String login = "admin";
	private String domain = "bm.lan";
	private String latd = login + "@" + domain;
	private String password = "admin";
	private String coreUrl = "http://core2.bm.lan:8090";

	private String[] verticlesClasses = new String[] { EASHttpVerticle.class.getCanonicalName() };
	private String[] workerClasses = new String[] { "net.bluemind.vertx.common.bus.CoreAuth",
			"net.bluemind.vertx.common.bus.Locator", DeviceValidationVerticle.class.getCanonicalName() };

	public void setUp() {
		GlobalConfig.DISABLE_POLICIES = true;
		deploymentIDs = Deploy.beforeTest(verticlesClasses, workerClasses);

	}

	public void testOptionsRequestNoAuth() {
		HttpClient client = client();
		try {
			TestResponseHandler options = new TestResponseHandler();
			HttpClientRequest request = client.options(EasUrls.ROOT, options);
			request.end();
			options.waitForIt();
			assertEquals(401, options.status());
		} finally {
			client.close();
		}
	}

	public void testOptionsRequestWithAuth() {
		HttpClient client = client();
		try {
			TestResponseHandler options = new TestResponseHandler();
			HttpClientRequest request = client.options(EasUrls.ROOT, options);
			addClientHeaders(request, false);
			request.end();
			options.waitForIt();
			assertEquals(200, options.status());
			MultiMap serverHeaders = options.headers();
			assertTrue("Missing protocol versions headers",
					serverHeaders.contains(EasHeaders.Server.PROTOCOL_VERSIONS));
			assertTrue("Missing ms server headers", serverHeaders.contains(EasHeaders.Server.MS_SERVER));
		} finally {
			client.close();
		}
	}

	public void testOptionsRequestWithAuthWindowsStyle() {
		HttpClient client = client();
		try {
			TestResponseHandler options = new TestResponseHandler();
			HttpClientRequest request = client.options(EasUrls.ROOT, options);
			addClientHeaders(request, true);
			request.end();
			options.waitForIt();
			assertEquals(200, options.status());
			MultiMap serverHeaders = options.headers();
			assertTrue("Missing protocol versions headers",
					serverHeaders.contains(EasHeaders.Server.PROTOCOL_VERSIONS));
			assertTrue("Missing ms server headers", serverHeaders.contains(EasHeaders.Server.MS_SERVER));
		} finally {
			client.close();
		}
	}

	private void doDummyRequest(boolean windowsStyle) throws Exception {
		assertTrue(DummyEndpoint.created);
		HttpClient client = client();
		try {
			TestResponseHandler rejected = new TestResponseHandler();
			String devId = "APPL" + System.currentTimeMillis();
			String devType = "iPhone";
			String cmd = EasUrls.ROOT + "?DeviceId=" + devId + "&DeviceType=" + devType + "&User=" + latd
					+ "&Cmd=Dummy";
			HttpClientRequest request = client.post(cmd, rejected);
			addClientHeaders(request, windowsStyle);
			request.end();
			rejected.waitForIt();
			assertEquals("Unknown device was not rejected", 403, rejected.status());

			addPartnership(devId);

			TestResponseHandler intoDummy = new TestResponseHandler();
			request = client.post(cmd, intoDummy);
			addClientHeaders(request, windowsStyle);
			request.end();
			intoDummy.waitForIt();
			assertTrue(DummyEndpoint.handled);
			assertEquals("Device should be accepted.", 200, intoDummy.status());

			MultiMap serverHeaders = intoDummy.headers();
			assertTrue("Missing dummy headers", serverHeaders.contains("DummyHeader"));

			assertTrue(DummyFilter1.executed);
			assertTrue(DummyFilter2.executed);

		} finally {
			client.close();
		}
	}

	public void testBrokenEndpoint() throws Exception {
		assertTrue(DummyEndpoint.created);
		HttpClient client = client();
		try {
			TestResponseHandler rejected = new TestResponseHandler();
			String devId = "APPL" + System.currentTimeMillis();
			String devType = "iPhone";
			String cmd = EasUrls.ROOT + "?DeviceId=" + devId + "&DeviceType=" + devType + "&User=" + latd
					+ "&Cmd=Broken";
			HttpClientRequest request = client.post(cmd, rejected);
			addClientHeaders(request, false);
			request.end();
			rejected.waitForIt();
			assertEquals("Unknown device was not rejected", 403, rejected.status());

			addPartnership(devId);

			TestResponseHandler intoBroken = new TestResponseHandler();
			request = client.post(cmd, intoBroken);
			addClientHeaders(request, false);
			request.end();
			intoBroken.waitForIt();
			assertEquals("Error 500 should be received", 500, intoBroken.status());
		} finally {
			client.close();
		}
	}

	public void testDummyRequest() throws Exception {
		doDummyRequest(false);
	}

	public void testDummyRequestWindowsStyleAuth() throws Exception {
		doDummyRequest(true);
	}

	private void addClientHeaders(HttpClientRequest req, boolean windowsStyle) {
		MultiMap headers = req.headers();
		String authHeader = "Basic " + Base64.encodeBytes(
				(windowsStyle ? domain + "\\" + login + ":" + password : latd + ":" + password).getBytes());
		headers.add("Authorization", authHeader);
		headers.add(EasHeaders.Client.PROTOCOL_VERSION, "14.1");
	}

	private HttpClient client() {
		HttpClient client = VertxPlatform.getVertx().createHttpClient();
		client.setHost("127.0.0.1").setPort(8082);
		return client;
	}

	public void tearDown() {
		Deploy.afterTest(deploymentIDs);
	}

	private void addPartnership(String devId) throws Exception {
		IAuthentication authService = ClientSideServiceProvider.getProvider(coreUrl, null)
				.instance(IAuthentication.class);
		LoginResponse admin0 = authService.login("admin0@global.virt", Token.admin0(), "eas-verticle-tests");

		IUser userService = ClientSideServiceProvider.getProvider(coreUrl, admin0.authKey).instance(IUser.class,
				domain);
		ItemValue<User> user = userService.byLogin(login);

		LoginResponse admin = authService.login(latd, password, "eas-verticle-tests");
		IDevice deviceService = ClientSideServiceProvider.getProvider(coreUrl, admin.authKey).instance(IDevice.class,
				user.uid);

		ItemValue<Device> device = deviceService.byIdentifier(devId);
		assertNotNull(device);

		deviceService = ClientSideServiceProvider.getProvider(coreUrl, admin0.authKey).instance(IDevice.class,
				user.uid);
		deviceService.setPartnership(device.uid);
	}
}
