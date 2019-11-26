/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class OfflineIdsStoreTests {

	private ContainerStore containerHome;
	private String containerId;
	private Container container;
	private OfflineMgmtStore offline;

	@Before
	public void before() throws Exception {

		SecurityContext securityContext = new SecurityContext(null, "system", Arrays.<String>asList(),
				Arrays.<String>asList(), null);
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(), securityContext);

		containerId = "test_" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "test", true);
		container = containerHome.create(container);
		assertNotNull(container);

		offline = new OfflineMgmtStore(JdbcTestHelper.getInstance().getDataSource());

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testGrabRanges() throws SQLException {
		long tenRange = offline.reserveItemIds(10);
		System.err.println("10-range: " + tenRange);
		assertTrue(tenRange > 0);
		long fiveRange = offline.reserveItemIds(5);
		System.err.println("5-range: " + fiveRange);
		assertEquals(tenRange + 10, fiveRange);
		long fifteenRange = offline.reserveItemIds(15);
		System.err.println("15-range: " + fifteenRange);
		assertEquals(fiveRange + 5, fifteenRange);
	}

}
