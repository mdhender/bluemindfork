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

import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.system.api.Database;
import net.bluemind.system.schemaupgrader.runner.SchemaUpgrade;

public class SchemaUpgraderTests {
	private DataSource pool;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		this.pool = JdbcActivator.getInstance().getDataSource();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testComputeUpgradePath() throws ServerFault {
		List<Updater> path = SchemaUpgrade.getUpgradePath();
		List<String[]> expected = new ArrayList<>();
		expected.add(new String[] { "20200428", "75", "ALL" });
		expected.add(new String[] { "20200429", "76", "ALL" });
		expected.add(new String[] { "20200429", "77", "DIRECTORY" });
		expected.add(new String[] { "20200415", "4", "ALL" });
		expected.add(new String[] { "20200429", "78", "SHARD" });
		assertTrue(upgradersFound(expected, path));
	}

	private boolean upgradersFound(List<String[]> expected, List<Updater> path) {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
		for (String[] expectedUpgrader : expected) {
			Database db = Database.valueOf(expectedUpgrader[2]);
			int seq = Integer.parseInt(expectedUpgrader[1]);
			boolean found = false;
			for (Updater lookup : path) {
				if (lookup.database() == db && lookup.sequence() == seq
						&& df.format(lookup.date()).equals(expectedUpgrader[0])) {
					found = true;
					break;
				}
			}
			if (!found) {
				LoggerFactory.getLogger(this.getClass()).info("Cannot find upgrader {}-{}", expectedUpgrader[0],
						expectedUpgrader[1]);
				return false;
			}
		}
		return true;
	}

}
