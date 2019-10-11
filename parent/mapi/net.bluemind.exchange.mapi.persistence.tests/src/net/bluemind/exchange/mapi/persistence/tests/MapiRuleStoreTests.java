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
package net.bluemind.exchange.mapi.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Base64;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.exchange.mapi.api.MapiRule;
import net.bluemind.exchange.mapi.persistence.MapiRuleStore;

public class MapiRuleStoreTests {

	private MapiRuleStore mapiRuleStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		mapiRuleStore = new MapiRuleStore(JdbcTestHelper.getInstance().getDataSource(), "some_container");
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void ruleCrud() throws Exception {
		MapiRule rule = new MapiRule();
		rule.ruleId = 1L;
		rule.ruleBase64 = Base64.getEncoder().encodeToString("create".getBytes());
		mapiRuleStore.store(rule);
		assertEquals(1, mapiRuleStore.all().size());

		// ensure it does an update, not a create
		mapiRuleStore.store(rule);
		assertEquals(1, mapiRuleStore.all().size());

		MapiRule found = mapiRuleStore.all().get(0);
		assertEquals("create", new String(Base64.getDecoder().decode(found.ruleBase64)));
		assertEquals(rule.ruleId, found.ruleId);

		rule.ruleBase64 = Base64.getEncoder().encodeToString("update".getBytes());
		mapiRuleStore.store(rule);
		found = mapiRuleStore.all().get(0);
		assertNotNull(found);
		assertEquals("update", new String(Base64.getDecoder().decode(found.ruleBase64)));
		assertEquals(rule.ruleId, found.ruleId);

		mapiRuleStore.delete(rule.ruleId);
		assertTrue(mapiRuleStore.all().isEmpty());
	}

}
