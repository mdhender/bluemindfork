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
package net.bluemind.system.service.internal;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.system.api.IGlobalSettings;

public class GlobalSettingsServiceTests {

	IGlobalSettings service;

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGlobalSettings.class);

		Map<String, String> map = service.get();
		for (String key : map.keySet()) {
			service.delete(key);
		}

	}

	@After
	public void tearDown() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSetAndGet() throws Exception {
		Map<String, String> values = new HashMap<>();
		values.put("key1", "value1");
		values.put("key2", "value2");

		service.set(values);

		Map<String, String> map = service.get();

		Assert.assertEquals(2, map.size());

		for (String key : map.keySet()) {
			String value = map.get(key);
			Assert.assertEquals(value, values.get(key));
		}
	}

	@Test
	public void testSetAndGetAndMerge() throws Exception {
		Map<String, String> values = new HashMap<>();
		values.put("key1", "value1");
		values.put("key2", "value2");

		service.set(values);

		Map<String, String> map = service.get();

		Assert.assertEquals(2, map.size());

		values.put("key3", "value3");
		values.put("key2", "value2-updated");

		service.set(values);

		map = service.get();

		Assert.assertEquals(3, map.size());

		for (String key : map.keySet()) {
			String value = map.get(key);
			Assert.assertEquals(value, values.get(key));
		}

	}

	@Test
	public void testDelete() throws Exception {
		Map<String, String> values = new HashMap<>();
		values.put("key1", "value1");
		values.put("key2", "value2");

		service.set(values);

		Map<String, String> map = service.get();

		Assert.assertEquals(2, map.size());

		service.delete("key1");

		map = service.get();

		Assert.assertEquals(1, map.size());
	}

}
