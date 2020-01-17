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

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.eclipse.core.runtime.Platform;

import net.bluemind.proxy.http.config.ForwardedLocation;
import net.bluemind.proxy.http.config.HPSConfiguration;
import net.bluemind.proxy.http.impl.ExtensionConfigLoader;

public class ForwardedLocationTests extends ProxyTestCase {

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

	public void testGetACRoot() {
		BoundRequestBuilder getQuery = ahc.prepareGet("http://localhost:" + hps.getPort() + "/adminconsole/");
		try {
			ListenableFuture<Response> future = getQuery.execute();
			Response response = future.get();
			assertEquals(200, response.getStatusCode());
			String rContent = response.getResponseBody();
			assertTrue(rContent.contains("action=\"bluemind_sso_security\""));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error executing get query on /adminconsole");
		}
	}

	public void testExtensionConfigLoader() {
		ExtensionConfigLoader ecl = new ExtensionConfigLoader();
		HPSConfiguration conf = new HPSConfiguration();
		ecl.load(conf);

		assertTrue(conf.getForwardedLocations().size() == Platform.getExtensionRegistry()
				.getExtensionPoint("net.bluemind.proxy.http", "forward").getExtensions().length);

		for (ForwardedLocation loc : conf.getForwardedLocations()) {
			if (loc.getPathPrefix().equals("/is-recognized")) {
				assertTrue(loc.getTargetUrl().equals("locator://bm/is-recognized:8080/is-recognized"));
				assertTrue(loc.getRequiredAuthKind().equals("CORE2"));
				return;
			}
		}
		fail("No forward location with expected path /is-recognized found.");
	}

	public void testRecognizedByHPS() {
		BoundRequestBuilder getQuery = ahc.prepareGet("http://localhost:" + hps.getPort() + "/is-recognized");
		try {
			ListenableFuture<Response> future = getQuery.execute();
			assertFalse(future.get().getResponseBody().contains("Nothing here"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error executing get query on /is-recognized");
		}
	}

	// Bug ToFix : https://forge.bluemind.net/jira/browse/BM-12454
	public void testPathTargetDifferent() {
		BoundRequestBuilder getQuery = ahc.prepareGet("http://localhost:" + hps.getPort() + "/path");
		try {
			ListenableFuture<Response> future = getQuery.execute();
			assertFalse(future.get().getResponseBody().contains("Nothing here"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error executing get query on /path");
		}
	}

	// Bug ToFix : https://forge.bluemind.net/jira/browse/BM-12455
	public void testSubqueryMatching() {
		BoundRequestBuilder getQuery = ahc.prepareGet("http://localhost:" + hps.getPort() + "/match-subquery/test");
		try {
			ListenableFuture<Response> future = getQuery.execute();
			assertFalse(future.get().getResponseBody().contains("Nothing here"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error executing get query on /match-subquery/test");
		}
	}
}
