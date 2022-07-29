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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;

import io.netty.handler.codec.http.cookie.Cookie;
import net.bluemind.proxy.http.HttpProxyServer;

public class AppLoginHelper {

	private AsyncHttpClient ahc;
	private HttpProxyServer hps;
	private String appUrl;
	private String testPass;
	private String testLogin;
	private LinkedHashMap<String, Cookie> cm;

	public AppLoginHelper(HttpProxyServer hps, String testLogin, String testPass) {
		this.hps = hps;
		this.testLogin = testLogin;
		this.testPass = testPass;
	}

	/**
	 * Log into BM application at /app/ (ie. settings, cal, etc)
	 * 
	 * @param app
	 * @throws Exception
	 */
	public String initApp(String app) throws Exception {
		this.ahc = AHCHelper.get();

		this.appUrl = "http://localhost:" + hps.getPort() + "/" + app + "/";
		BoundRequestBuilder getQuery = ahc.prepareGet(appUrl);

		// get a request id in the /settings url
		ListenableFuture<Response> future = getQuery.execute();
		Response response = future.get();
		if (200 != response.getStatusCode()) {
			throw new Exception("Status code != 200");
		}
		String storedRequestId = response.getHeader("BMStoredRequestId");
		System.err.println("storedRequestId: " + storedRequestId);
		if (storedRequestId == null) {
			throw new Exception("storedRequestId must not be null");
		}

		// post auth data with request id
		BoundRequestBuilder post = ahc
				.preparePost("http://localhost:" + hps.getPort() + "/" + app + "/bluemind_sso_security");
		post.addFormParam("login", testLogin);
		post.addFormParam("password", testPass);
		post.addFormParam("priv", "priv");
		post.addFormParam("storedRequestId", storedRequestId);
		future = post.execute();
		response = future.get();
		List<Cookie> cookies = response.getCookies();
		this.cm = new LinkedHashMap<>();
		System.err.println("Cookies count is " + cookies.size());
		for (Cookie c : cookies) {
			System.err.println("S: cookie " + c.name() + " = " + c.value());
			cm.put(c.name(), c);
		}
		String location = response.getHeader("Location");
		System.err.println("Location: " + location);
		while (location != null) {
			String nurl = "http://localhost:" + hps.getPort() + "/" + app + "/" + location.substring(2);
			post = ahc.preparePost(nurl);
			for (Cookie c : cm.values()) {
				post.addCookie(c);
			}
			System.err.println("Redirect " + nurl);
			future = post.execute();
			response = future.get();
			List<Cookie> rc = response.getCookies();
			for (Cookie c : rc) {
				cm.put(c.name(), c);
			}
			location = response.getHeader("Location");
		}
		String body = response.getResponseBody();
		return body;
	}

	public String executeGet(String inAppUrl) throws Exception {
		BoundRequestBuilder getQuery = ahc.prepareGet(inAppUrl);
		for (Cookie c : cm.values()) {
			getQuery.addCookie(c);
		}

		System.err.println("=========== GET ========");
		ListenableFuture<Response> future = getQuery.execute();
		Response response = future.get();
		String rContent = response.getResponseBody();
		System.err.println("statusCode: " + response.getStatusCode() + " for " + inAppUrl);
		System.err.println("ContentType: " + response.getContentType() + " for " + inAppUrl);
		if (200 != response.getStatusCode()) {
			throw new Exception("statusCode != 200");
		}
		return rContent;
	}

	public long executeGetSize(String inAppUrl) throws Exception {
		BoundRequestBuilder getQuery = ahc.prepareGet(inAppUrl);
		for (Cookie c : cm.values()) {
			getQuery.addCookie(c);
		}

		System.err.println("=========== GET ========");
		ListenableFuture<Long> future = getQuery.execute(new SizeHandler());
		return future.get();

	}

	public LinkedHashMap<String, Cookie> getCm() {
		return cm;
	}

	public AsyncHttpClient getAhc() {
		return ahc;
	}

	public void dispose() {
		try {
			this.ahc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.ahc = null;
	}

	public String getAppUrl() {
		return appUrl;
	}
}
