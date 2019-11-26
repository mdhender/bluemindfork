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
package net.bluemind.globalsettings.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.globalsettings.persistence.GlobalSettingsStore;

public class SettingsStoreTests {
	private static Logger logger = LoggerFactory.getLogger(SettingsStoreTests.class);
	private GlobalSettingsStore settingsStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		

		settingsStore = new GlobalSettingsStore(JdbcTestHelper.getInstance().getDataSource());

		logger.debug("stores: {}", settingsStore);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testGetById() throws Exception {
		// Test get settings by item ID
		Map<String, String> loadedDomainSettings = settingsStore.get();
		assertNotNull(loadedDomainSettings);
		assertTrue(loadedDomainSettings.size() > 0);

		for (String k : loadedDomainSettings.keySet()) {
			System.err.println(k + ": " + loadedDomainSettings.get(k));
		}

	}

	@Test
	public void testSet() throws Exception {
		// Test get settings by item ID

		settingsStore.set(ImmutableMap.<String, String> builder().put("test", "1").build());
		Map<String, String> loadedDomainSettings = settingsStore.get();
		assertNotNull(loadedDomainSettings);
		assertEquals(1, loadedDomainSettings.size());

	}

}
