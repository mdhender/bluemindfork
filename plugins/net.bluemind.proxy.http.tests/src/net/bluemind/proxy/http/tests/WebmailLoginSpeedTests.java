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
package net.bluemind.proxy.http.tests;

import java.util.concurrent.TimeUnit;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;

public class WebmailLoginSpeedTests extends ProxyTestCase {

	private AsyncHttpClient ahc;

	@Override
	public void protectedSetUp() throws Exception {
		this.ahc = AHCHelper.get();
	}

	@Override
	public void tearDown() throws Exception {
		this.ahc.close();
		this.ahc = null;
		super.tearDown();
	}

	public void testLoggedInWebmail() {
		BoundRequestBuilder getQuery = ahc.prepareGet("http://localhost:" + hps.getPort() + "/webmail/");
		try {
			ListenableFuture<Response> future = getQuery.execute();
			Response response = future.get(30, TimeUnit.SECONDS);
			assertEquals(200, response.getStatusCode());
			String storedRequestId = response.getHeader("BMStoredRequestId");
			System.err.println("storedRequestId: " + storedRequestId);
			assertNotNull(storedRequestId);

			// post auth data with request id
			long time = System.currentTimeMillis();
			BoundRequestBuilder post = ahc
					.preparePost("http://localhost:" + hps.getPort() + "/webmail/bluemind_sso_security");
			post.addFormParam("login", testLogin);
			post.addFormParam("password", testPass);
			post.addFormParam("priv", "priv");
			post.addFormParam("storedRequestId", storedRequestId);
			future = post.execute();
			response = future.get(30, TimeUnit.SECONDS);
			String ssoCookie = response.getHeader("BMSsoCookie");
			System.err.println("sso cookie: " + ssoCookie);
			assertNotNull(ssoCookie);
			long auth = System.currentTimeMillis() - time;
			System.err.println("First time: " + auth + "ms.");
			assertTrue(auth < 3000);
			String theBody = response.getResponseBody();
			assertNotNull(theBody);

			System.out.println("============================ GET ========");
			getQuery = ahc.prepareGet("http://localhost:" + hps.getPort() + "/webmail/");
			getQuery.addQueryParam("BMHPS", ssoCookie.trim());
			future = getQuery.execute();
			response = future.get(30, TimeUnit.SECONDS);
			System.err.println("statusCode: " + response.getStatusCode());
			assertEquals(302, response.getStatusCode());
			time = System.currentTimeMillis() - time;
			assertTrue(time < 4000);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error executing get query on /webmail");
		}
	}
}
