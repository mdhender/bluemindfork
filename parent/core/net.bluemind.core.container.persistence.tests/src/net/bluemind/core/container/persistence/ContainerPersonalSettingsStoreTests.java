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
package net.bluemind.core.container.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class ContainerPersonalSettingsStoreTests {

	private ContainerPersonalSettingsStore store;
	private String containerId;
	private Container container;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		containerId = "test_" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);
		assertNotNull(container);

		store = new ContainerPersonalSettingsStore(JdbcTestHelper.getInstance().getDataSource(), securityContext,
				container);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testGetSet() throws Exception {
		Map<String, String> map = store.get();
		assertNotNull(map);
		assertTrue(map.isEmpty());

		map = new HashMap<String, String>();
		map.put("string", "blah");
		map.put("color", "blue");
		store.set(map);

		map = store.get();
		assertEquals(2, map.size());
		assertEquals("blah", map.get("string"));
		assertEquals("blue", map.get("color"));

		map = new HashMap<String, String>();
		map.put("t1", "v1test");
		map.put("t3", "v3");
		store.set(map);
		map = store.get();
		assertEquals(2, map.size());
		assertEquals("v1test", map.get("t1"));
		assertNull(map.get("t2"));
		assertEquals("v3", map.get("t3"));

	}

}
