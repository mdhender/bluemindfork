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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;

import io.netty.handler.codec.http.cookie.Cookie;

public class WebmailAutocompleteTests extends ProxyTestCase {

	private AsyncHttpClient ahc;
	private List<Cookie> cookies;
	private LinkedHashMap<String, Cookie> cm;
	private String rcRequestToken;

	@Override
	public void protectedSetUp() throws Exception {
	}

	private void setupHttpClient() {
		this.ahc = AHCHelper.get();

		BoundRequestBuilder getQuery = ahc.prepareGet("http://localhost:" + hps.getPort() + "/webmail/");
		try {
			// get a request id in the /cal url
			ListenableFuture<Response> future = getQuery.execute();
			Response response = future.get(30, TimeUnit.SECONDS);
			assertEquals(200, response.getStatusCode());
			String storedRequestId = response.getHeader("BMStoredRequestId");
			System.err.println("storedRequestId: " + storedRequestId);
			assertNotNull(storedRequestId);

			// post auth data with request id
			BoundRequestBuilder post = ahc
					.preparePost("http://localhost:" + hps.getPort() + "/webmail/bluemind_sso_security");
			post.addFormParam("login", testLogin);
			post.addFormParam("password", testPass);
			post.addFormParam("priv", "priv");
			post.addFormParam("storedRequestId", storedRequestId);
			future = post.execute();
			response = future.get(30, TimeUnit.SECONDS);
			this.cookies = response.getCookies();
			this.cm = new LinkedHashMap<String, Cookie>();
			System.err.println("Cookies count is " + cookies.size());
			for (Cookie c : cookies) {
				System.err.println("S: cookie " + c.name() + " = " + c.value());
				cm.put(c.name(), c);
			}
			String location = response.getHeader("Location");
			System.err.println("Location: " + location);
			while (location != null) {
				String nurl = "http://localhost:" + hps.getPort() + "/webmail/" + location.substring(2);
				post = ahc.preparePost(nurl);
				for (Cookie c : cm.values()) {
					// System.err.println(" ==> " + c.getName() + ": "
					// + c.getValue());
					post.addCookie(c);
				}
				System.err.println("Redirect " + nurl);
				future = post.execute();
				response = future.get(30, TimeUnit.SECONDS);
				List<Cookie> rc = response.getCookies();
				for (Cookie c : rc) {
					// System.err.println("rc: " + c.getName() + " "
					// + c.getValue());
					cm.put(c.name(), c);
				}
				location = response.getHeader("Location");
			}
			String body = response.getResponseBody();
			String token = "\"request_token\":\"";
			int idx = body.indexOf(token);
			if (idx > 0) {
				idx += token.length();
				int end = body.indexOf("\"", idx);
				this.rcRequestToken = body.substring(idx, end);
				System.err.println("request_token: " + rcRequestToken);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error executing getting sso cookie");
		}
	}

	@Override
	public void tearDown() throws Exception {
		if (ahc != null) {
			ahc.close();
			ahc = null;
		}
		super.tearDown();
	}

	public void testLoggedInWebmail() {
		try {
			setupHttpClient();
			BoundRequestBuilder getQuery = ahc.prepareGet("http://localhost:" + hps.getPort() + "/webmail/");
			for (Cookie c : cm.values()) {
				getQuery.addCookie(c);
			}

			System.err.println("=========== GET ========");
			ListenableFuture<Response> future = getQuery.execute();
			Response response = future.get(30, TimeUnit.SECONDS);
			String rContent = response.getResponseBody();
			System.err.println("statusCode: " + response.getStatusCode());
			assertEquals(200, response.getStatusCode());
			assertNotNull(rContent);
			assertTrue(rContent.contains("rcmail.init()"));
			List<Cookie> rc = response.getCookies();
			System.err.println("Received cookies: " + rc.size());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error executing get query on /webmail");
		}
	}

	public void testAutocomplete() {
		try {
			setupHttpClient();
			BoundRequestBuilder pc = ahc
					.preparePost("http://localhost:" + hps.getPort() + "/webmail/?_task=mail&_action=autocomplete");
			for (Cookie c : cm.values()) {
				pc.addCookie(c);
			}
			String id = System.currentTimeMillis() + "";
			pc.addFormParam("_search", "a");
			pc.addFormParam("_id", id);
			pc.addFormParam("_remote", "1");
			pc.addFormParam("_unlock", "loading" + id);

			pc.addHeader("X-Requested-With", "XMLHttpRequest");
			pc.addHeader("X-Roundcube-Request", rcRequestToken);

			System.err.println("=========== POST ========");
			ListenableFuture<Response> future = pc.execute();
			Response response = future.get(30, TimeUnit.SECONDS);
			String rContent = response.getResponseBody();
			System.err.println("statusCode: " + response.getStatusCode());
			System.err.println("ContentType: " + response.getContentType());
			assertEquals(200, response.getStatusCode());
			// assertNotNull(rContent);
			assertFalse(rContent.contains("rcmail.init"));
			System.err.println("content: " + rContent);
			assertTrue(rContent.startsWith("{\"action\":\"autocomplete"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error executing get query on /webmail");
		}
	}
}
