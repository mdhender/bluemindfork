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
package net.bluemind.system.persistence.tests;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.system.api.Database;
import net.bluemind.system.persistence.Upgrader;
import net.bluemind.system.persistence.Upgrader.UpgradePhase;
import net.bluemind.system.persistence.UpgraderStore;

public class UpgraderStoreTests {
	private static Logger logger = LoggerFactory.getLogger(UpgraderStoreTests.class);
	private UpgraderStore schemaVersionStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		schemaVersionStore = new UpgraderStore(JdbcTestHelper.getInstance().getDataSource());
		schemaVersionStore.needsMigration();

		logger.debug("stores: {}", schemaVersionStore);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testAddingUpgrader() throws Exception {
		Upgrader value1 = new Upgrader();
		value1.database = Database.DIRECTORY;
		value1.server = "bm-master";
		value1.upgraderId(new Date(), 1);
		value1.phase = UpgradePhase.POST_SCHEMA_UPGRADE;
		value1.success = true;
		schemaVersionStore.store(value1);

		Upgrader value2 = new Upgrader();
		value2.database = Database.DIRECTORY;
		value2.server = "bm-master";
		value2.upgraderId(new Date(), 2);
		value2.phase = UpgradePhase.POST_SCHEMA_UPGRADE;
		value2.success = false;
		schemaVersionStore.store(value2);

		assertEquals(true, schemaVersionStore.upgraderCompleted(value1.upgraderId, value1.server, value1.database));
		assertEquals(false, schemaVersionStore.upgraderCompleted(value2.upgraderId, value2.server, value2.database));
	}

	@Test
	public void testAddingUpgraderOnDifferentServer() throws Exception {
		Upgrader value1 = new Upgrader();
		value1.database = Database.DIRECTORY;
		value1.server = "bm-master1";
		value1.upgraderId(new Date(), 1);
		value1.phase = UpgradePhase.POST_SCHEMA_UPGRADE;
		value1.success = true;
		schemaVersionStore.store(value1);

		Upgrader value2 = new Upgrader();
		value2.database = Database.DIRECTORY;
		value2.server = "bm-master2";
		value2.upgraderId(new Date(), 1);
		value2.phase = UpgradePhase.POST_SCHEMA_UPGRADE;
		value2.success = false;
		schemaVersionStore.store(value2);

		assertEquals(true, schemaVersionStore.upgraderCompleted(value1.upgraderId, value1.server, value1.database));
		assertEquals(false, schemaVersionStore.upgraderCompleted(value2.upgraderId, value2.server, value2.database));
	}

	@Test
	public void testAddingUpgraderOnDifferentDatabase() throws Exception {
		Upgrader value1 = new Upgrader();
		value1.database = Database.DIRECTORY;
		value1.server = "bm-master";
		value1.upgraderId(new Date(), 1);
		value1.phase = UpgradePhase.POST_SCHEMA_UPGRADE;
		value1.success = true;
		schemaVersionStore.store(value1);

		Upgrader value2 = new Upgrader();
		value2.database = Database.SHARD;
		value2.server = "bm-master";
		value2.upgraderId(new Date(), 1);
		value2.phase = UpgradePhase.POST_SCHEMA_UPGRADE;
		value2.success = false;
		schemaVersionStore.store(value2);

		assertEquals(true, schemaVersionStore.upgraderCompleted(value1.upgraderId, value1.server, value1.database));
		assertEquals(false, schemaVersionStore.upgraderCompleted(value2.upgraderId, value2.server, value2.database));
	}

	@Test
	public void testUpdatingUpgrader() throws Exception {
		Upgrader value1 = new Upgrader();
		value1.database = Database.DIRECTORY;
		value1.server = "bm-master";
		value1.upgraderId(new Date(), 1);
		value1.phase = UpgradePhase.POST_SCHEMA_UPGRADE;
		value1.success = false;
		schemaVersionStore.store(value1);

		Upgrader value2 = new Upgrader();
		value2.database = Database.DIRECTORY;
		value2.server = "bm-master";
		value2.upgraderId(new Date(), 2);
		value2.phase = UpgradePhase.POST_SCHEMA_UPGRADE;
		value2.success = false;
		schemaVersionStore.store(value2);

		assertEquals(false, schemaVersionStore.upgraderCompleted(value2.upgraderId, value2.server, value2.database));
		assertEquals(false, schemaVersionStore.upgraderCompleted(value1.upgraderId, value1.server, value1.database));

		value1.success = true;
		schemaVersionStore.store(value1);

		assertEquals(true, schemaVersionStore.upgraderCompleted(value1.upgraderId, value1.server, value1.database));
		assertEquals(false, schemaVersionStore.upgraderCompleted(value2.upgraderId, value2.server, value2.database));

	}

	@Test
	public void testCheckingNonExistentUpgrader() throws Exception {
		assertEquals(false, schemaVersionStore.upgraderCompleted("202001200001", "srv", Database.DIRECTORY));
	}

}
