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

import java.text.SimpleDateFormat;
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
	private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

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
		Map<String, Integer> expectedUpdaters = new HashMap<>(); // key=major / value=build_number
		expectedUpdaters.put("20200428", 75);
		expectedUpdaters.put("20200429", 76);
		expectedUpdaters.put("20200415", 4);

		SqlScripts scriptsLoader = new SqlScripts();
		List<Updater> updaters = scriptsLoader.getSqlScripts();
		assertTrue(updaters.size() >= 3);

		for (Updater u : updaters) {
			if (u.sequence() == 76) {
				assertTrue(u.afterSchemaUpgrade());
			} else if (u.sequence() == 75 || u.sequence() == 4) {
				assertFalse(u.afterSchemaUpgrade());
			}
		}

		List<String> dates = updaters.stream().map(u -> df.format(u.date())).collect(Collectors.toList());
		List<Integer> sequences = updaters.stream().map(u -> u.sequence()).collect(Collectors.toList());

		for (Map.Entry<String, Integer> entry : expectedUpdaters.entrySet()) {
			assertTrue(dates.contains(entry.getKey()));
			assertTrue(sequences.contains(entry.getValue()));
		}
	}

}
