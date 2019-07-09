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

public class SimpleUserSettingsTests extends AsUserApplicationTests {

	private String body;

	public void testLoggedIn() {
		assertNotNull(app.getAppUrl());
		System.out.println("appUrl: " + app.getAppUrl());
		assertTrue(body.contains("settings.nocache.js"));
	}

	@Override
	public void protectedSetUp() throws Exception {
		super.protectedSetUp();
		this.body = app.initApp("settings");
	}

	@Override
	public void tearDown() throws Exception {
		body = null;
		super.tearDown();
	}

}
