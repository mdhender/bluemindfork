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

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.system.persistance.SchemaVersion;
import net.bluemind.system.persistance.SchemaVersion.UpgradePhase;
import net.bluemind.system.schemaupgrader.ComponentVersion;
import net.bluemind.system.persistance.SchemaVersionStore;

public class SchemaVersionStoreTests {
	private static Logger logger = LoggerFactory.getLogger(SchemaVersionStoreTests.class);
	private SchemaVersionStore schemaVersionStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		

		schemaVersionStore = new SchemaVersionStore(JdbcTestHelper.getInstance().getDataSource());

		logger.debug("stores: {}", schemaVersionStore);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testAddingUpgraders() throws Exception {
		assertEquals(0, schemaVersionStore.get(1, 0).size());

		SchemaVersion value1 = new SchemaVersion("3.9545");
		value1.component = "bm/core";
		value1.phase = UpgradePhase.POST_SCHEMA_UPGRADE;
		value1.success = true;
		schemaVersionStore.add(value1);
		schemaVersionStore.add(value1);
		schemaVersionStore.add(value1);

		assertEquals(3, schemaVersionStore.get(1, 0).size());
	}

	@Test
	public void testGettingUpgrader() throws Exception {
		SchemaVersion value1 = new SchemaVersion(3, 9543);
		value1.component = "bm/core";
		value1.phase = UpgradePhase.POST_SCHEMA_UPGRADE;
		value1.success = true;
		schemaVersionStore.add(value1);

		SchemaVersion schemaVersion = schemaVersionStore.get(1, 0).get(0);

		assertEquals(schemaVersion, value1);
	}

	@Test
	public void testGetSpecificUpgraders() throws Exception {
		SchemaVersion value1 = get("3.3", UpgradePhase.POST_SCHEMA_UPGRADE, false);
		SchemaVersion value2 = get("3.920", UpgradePhase.POST_SCHEMA_UPGRADE, false);
		SchemaVersion value3 = get("3.9100", UpgradePhase.SCHEMA_UPGRADE, false);
		SchemaVersion value4 = get("3.9220", UpgradePhase.POST_SCHEMA_UPGRADE, true);
		SchemaVersion value5 = get("3.9220", UpgradePhase.SCHEMA_UPGRADE, true);
		SchemaVersion value6 = get("3.9259", UpgradePhase.POST_SCHEMA_UPGRADE, true);
		SchemaVersion value7 = get("3.9260", UpgradePhase.POST_SCHEMA_UPGRADE, false);
		SchemaVersion value8 = get("4.1", UpgradePhase.POST_SCHEMA_UPGRADE, true);
		SchemaVersion value9 = get("4.112", UpgradePhase.POST_SCHEMA_UPGRADE, false);
		SchemaVersion value10 = get("4.9856", UpgradePhase.POST_SCHEMA_UPGRADE, true);

		// add in random order
		schemaVersionStore.add(value3);
		schemaVersionStore.add(value5);
		schemaVersionStore.add(value1);
		schemaVersionStore.add(value9);
		schemaVersionStore.add(value10);
		schemaVersionStore.add(value2);
		schemaVersionStore.add(value6);
		schemaVersionStore.add(value7);
		schemaVersionStore.add(value8);
		schemaVersionStore.add(value4);

		List<SchemaVersion> upgraders = schemaVersionStore.get(0, 0);
		assertEquals(10, upgraders.size());

		// check sorting

		// phase1
		assertEquals(value3, upgraders.get(0));
		assertEquals(value5, upgraders.get(1));
		// phase2
		assertEquals(value1, upgraders.get(2));
		assertEquals(value2, upgraders.get(3));
		assertEquals(value4, upgraders.get(4));
		assertEquals(value6, upgraders.get(5));
		assertEquals(value7, upgraders.get(6));
		assertEquals(value8, upgraders.get(7));
		assertEquals(value9, upgraders.get(8));
		assertEquals(value10, upgraders.get(9));

		upgraders = schemaVersionStore.get(3, 9120);
		assertEquals(7, upgraders.size());

		// phase1
		assertEquals(value5, upgraders.get(0));
		// phase2
		assertEquals(value4, upgraders.get(1));
		assertEquals(value6, upgraders.get(2));
		assertEquals(value7, upgraders.get(3));
		assertEquals(value8, upgraders.get(4));
		assertEquals(value9, upgraders.get(5));
		assertEquals(value10, upgraders.get(6));

		upgraders = schemaVersionStore.get(5, 3);
		assertEquals(0, upgraders.size());

	}

	@Test
	public void testSelectAndUpdateComponentVersion() throws Exception {
		schemaVersionStore.updateComponentVersion("test1", "1");
		schemaVersionStore.updateComponentVersion("test2", "1");
		schemaVersionStore.updateComponentVersion("test1", "2");
		schemaVersionStore.updateComponentVersion("test1", "3");

		List<ComponentVersion> comps = schemaVersionStore.getComponentsVersion();
		assertEquals(2, comps.size());

		String test1Version = null;
		String test2Version = null;
		if (comps.get(0).identifier.equals("test1")) {
			test1Version = comps.get(0).version;
			assertEquals("test2", comps.get(1).identifier);
			test2Version = comps.get(1).version;
		} else {
			assertEquals("test2", comps.get(0).identifier);
			test2Version = comps.get(0).version;
			assertEquals("test1", comps.get(1).identifier);
			test1Version = comps.get(1).version;
		}

		assertEquals("3", test1Version);
		assertEquals("1", test2Version);
	}

	private SchemaVersion get(String build, UpgradePhase phase, boolean success) throws ServerFault {
		SchemaVersion value = new SchemaVersion(build);
		value.component = "bm/core";
		value.phase = phase;
		value.success = success;
		return value;
	}

}
