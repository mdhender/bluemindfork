/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.sentry.settings.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.sentry.settings.SentryProperties;

public class SentryPropertiesTests {

	@Before
	public void before() {
	}

	@Test
	public void testLoadThenUpdate() throws IOException {
		SentryProperties.checkOrCreateFolders();
		assertNotNull(SentryProperties.getConfigurationPath());

		SentryProperties sp = new SentryProperties();
		try {
			sp.update();
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		System.err.println("update a second time...");
		try {
			sp.update();
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
