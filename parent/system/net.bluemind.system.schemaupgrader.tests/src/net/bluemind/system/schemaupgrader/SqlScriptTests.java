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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;

public class SqlScriptTests {

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
	public void testScriptsAreFound() {
		Map<Integer, Integer> expectedUpdaters = new HashMap<Integer, Integer>(); // key=major / value=build_number
		expectedUpdaters.put(31, 75);
		expectedUpdaters.put(31, 76);
		expectedUpdaters.put(0, 4);
		
		SqlScripts scriptsLoader = new SqlScripts(pool);
		List<Updater> updaters = scriptsLoader.getSqlScripts();
		assertTrue(updaters.size() >= 3);
		
		for (Updater u : updaters) {
			if (u.major() == 31 && u.build() == 76) {
				assertTrue(u.afterSchemaUpgrade());
			} else {
				assertFalse(u.afterSchemaUpgrade());
			}
		}

		List<Integer> majors = updaters.stream().map(u -> u.major()).collect(Collectors.toList());
		List<Integer> buildNumbers = updaters.stream().map(u -> u.build()).collect(Collectors.toList());
		
		for (Map.Entry<Integer, Integer> entry : expectedUpdaters.entrySet()) {
			assertTrue(majors.contains(entry.getKey()));
			assertTrue(buildNumbers.contains(entry.getValue()));
		}
	}

}
