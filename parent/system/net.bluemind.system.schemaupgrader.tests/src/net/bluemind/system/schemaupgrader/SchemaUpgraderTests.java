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
package net.bluemind.system.schemaupgrader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.VersionInfo;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.system.schemaupgrader.internal.ClassUpdater;
import net.bluemind.system.schemaupgrader.internal.SqlUpdater;
import net.bluemind.system.schemaupgrader.tests.internal.TestMonitor;

public class SchemaUpgraderTests {
	private DataSource pool;
	private IServerTaskMonitor monitor;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		this.pool = JdbcActivator.getInstance().getDataSource();
		this.monitor = new TestMonitor();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testComputeUpgradePath() throws ServerFault {
		SchemaUpgrade su = new SchemaUpgrade(pool);
		List<Updater> path = su.getUpgradePath(VersionInfo.create("30.0.1234"), VersionInfo.create("88.0.4567"),
				"bm/core");
		assertNotNull(path);
		assertFalse(path.isEmpty());
		assertEquals(2, path.size());
		int javaFound = 0;
		int sqlFound = 0;
		for (Updater u : path) {
			if (u instanceof SqlUpdater) {
				sqlFound++;
			} else if (u instanceof ClassUpdater) {
				javaFound++;
			}
		}
		assertEquals(1, sqlFound);
		assertEquals(1, javaFound);
	}

}
