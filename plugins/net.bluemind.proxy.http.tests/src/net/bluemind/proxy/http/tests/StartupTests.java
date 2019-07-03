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

import java.net.MalformedURLException;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class StartupTests extends ProxyTestCase {

	private AsyncHttpClient ahc;

	public void protectedSetUp() throws Exception {
		this.ahc = AHCHelper.get();
	}

	public void tearDown() throws Exception {
		ahc.close();
		super.tearDown();
	}

	public void testStartStop() {

	}

	public void testGetSlash() {
		String url = "http://localhost:" + hps.getPort() + "/";
		try {
			Response resp = ahc.prepareGet(url).execute().get();
			String content = resp.getResponseBody();
			assertNotNull(content);
			assertEquals(200, resp.getStatusCode());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error connecting to hps: " + e.getMessage());
		}

	}

	public void test2Queries() throws MalformedURLException {
		String url = "http://localhost:" + hps.getPort() + "/";
		try {
			Response resp = ahc.prepareGet(url).execute().get();
			String content = resp.getResponseBody();
			assertNotNull(content);
			assertEquals(200, resp.getStatusCode());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error connecting to hps: " + e.getMessage());
		}

		url = "http://127.0.0.1:" + hps.getPort() + "/";
		try {
			Response resp = ahc.prepareGet(url).execute().get();
			String content = resp.getResponseBody();
			assertNotNull(content);
			assertEquals(200, resp.getStatusCode());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error connecting to hps: " + e.getMessage());
		}

	}
}
