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
package net.bluemind.calendar.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class FreebusyStoreTests {

	private FreebusyStore store;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime();
		Container container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);

		assertNotNull(container);

		store = new FreebusyStore(JdbcTestHelper.getInstance().getDataSource(), container);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testFreebusy() throws SQLException {
		List<String> calendars = store.get();
		assertNotNull(calendars);
		assertTrue(calendars.isEmpty());

		store.add("this-is-calendar");
		calendars = store.get();
		assertEquals(1, calendars.size());

		store.add("this-is-calendar2");
		store.add("this-is-calendar3");
		calendars = store.get();
		assertEquals(3, calendars.size());

		store.remove("this-is-calendar");
		calendars = store.get();
		assertEquals(2, calendars.size());

		store.remove("this-is-wtf");
		calendars = store.get();
		assertEquals(2, calendars.size());

		calendars.clear();
		calendars.add("toto");
		calendars.add("titi");
		store.set(calendars);

		calendars = store.get();
		assertEquals(2, calendars.size());
		assertTrue(calendars.contains("toto"));
		assertTrue(calendars.contains("titi"));

		store.delete();
		calendars = store.get();
		assertNotNull(calendars);
		assertTrue(calendars.isEmpty());
	}

}
