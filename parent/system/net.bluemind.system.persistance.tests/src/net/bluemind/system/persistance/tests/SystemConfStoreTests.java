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
package net.bluemind.system.persistance.tests;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.system.persistance.SystemConfStore;

public class SystemConfStoreTests {
	private static Logger logger = LoggerFactory.getLogger(SystemConfStoreTests.class);
	private SystemConfStore systemConfStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		

		systemConfStore = new SystemConfStore(JdbcTestHelper.getInstance().getDataSource());

		logger.debug("stores: {}", systemConfStore);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testUpdate() throws Exception {
		Map<String, String> values = new HashMap<String, String>();
		values.put("test1", "test1value");
		values.put("test2", "test2value");
		systemConfStore.update(values);

		Map<String, String> actual = systemConfStore.get();
		assertEquals(values.get("test1"), actual.get("test1"));
		assertEquals(values.get("test2"), actual.get("test2"));
	}

}
