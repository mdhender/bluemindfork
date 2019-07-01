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

public class SettingsHpsTests extends InApplicationHPSTests {

	private String body;

	public void testLoggedIn() {
		try {
			this.body = app.initApp("settings");
			assertNotNull(app.getAppUrl());
			System.out.println("appUrl: " + app.getAppUrl());
			assertTrue(body.contains("settings.nocache.js"));
		} catch (Throwable t) {
			t.printStackTrace();
			fail("Test thrown an exception");
		}
	}

	public void testDownloadTbirdConnector() {
		try {
			this.body = app.initApp("settings");
			String downloadUrl = app.getAppUrl() + "settings/download?file=bm-connector-tb.xpi";
			String xpiContent = app.executeGet(downloadUrl);
			assertNotNull(xpiContent);
		} catch (Throwable t) {
			t.printStackTrace();
			fail("Test thrown an exception");
		}
	}

	public void testDownloadOutlookConnector() {
		try {
			this.body = app.initApp("settings");
			String downloadUrl = app.getAppUrl() + "settings/download/outlookx86";
			long size = app.executeGetSize(downloadUrl);
			// check that we fetched more than 50MB
			System.err.println("Outlook connector size : " + size);
			assertTrue("Size was not > 50meg (" + size + ")", size > 50000000);
		} catch (Throwable t) {
			t.printStackTrace();
			fail("Test thrown an exception");
		}
	}

	public void testOutlookDLLoop() {
		for (int i = 0; i < 25; i++) {
			testDownloadOutlookConnector();
		}
	}

	public void testDownloadOutlookMsiX86() throws Exception {
		app.initApp("settings");
		String downloadUrl = app.getAppUrl() + "settings/download/outlookx86?file=SetupAddin.msi";
		long size = app.executeGetSize(downloadUrl);
		// check that we fetched more than 1MB
		System.err.println("MSI of outlook connector size : " + size);
		assertTrue("Size was not > 1 meg (" + size + ")", size > 1000000);
	}

	public void testDownloadOutlookMsiX64() throws Exception {
		app.initApp("settings");
		String downloadUrl = app.getAppUrl() + "settings/download/outlookx64?file=SetupAddin.msi";
		long size = app.executeGetSize(downloadUrl);
		// check that we fetched more than 1MB
		System.err.println("MSI of outlook connector size : " + size);
		assertTrue("Size was not > 1 meg (" + size + ")", size > 1000000);
	}

	@Override
	public void protectedSetUp() throws Exception {
		super.protectedSetUp();
	}

	@Override
	public void tearDown() throws Exception {
		body = null;
		super.tearDown();
	}

}
