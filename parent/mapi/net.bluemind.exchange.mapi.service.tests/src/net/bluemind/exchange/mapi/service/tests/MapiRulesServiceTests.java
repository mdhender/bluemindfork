/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.exchange.mapi.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.config.InstallationId;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.exchange.mapi.api.IMapiRules;
import net.bluemind.exchange.mapi.api.MapiRule;
import net.bluemind.exchange.mapi.api.MapiRuleChanges;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.locator.LocatorVerticle;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.vertx.testhelper.Deploy;

public class MapiRulesServiceTests {

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		Deploy.verticles(false, LocatorVerticle::new).get(5, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt();
		Sessions.get().put("toto", SecurityContext.SYSTEM);
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IMapiRules mapiRulesApi() {
		// we just need a valid container
		IMapiRules service = ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", "toto")
				.instance(IMapiRules.class, InstallationId.getIdentifier());
		return service;
	}

	@Test
	public void testCrud() {
		IMapiRules api = mapiRulesApi();
		assertNotNull(api);

		MapiRule r1 = new MapiRule();
		r1.ruleId = 1;
		r1.ruleBase64 = Base64.getEncoder().encodeToString("r1".getBytes());

		MapiRule r2 = new MapiRule();
		r2.ruleId = 2;
		r2.ruleBase64 = Base64.getEncoder().encodeToString("r2".getBytes());

		MapiRuleChanges changes = new MapiRuleChanges();
		changes.created = Arrays.asList(r1, r2);

		api.updates(changes);

		List<MapiRule> fetched = api.all();
		assertEquals(2, fetched.size());

		changes = new MapiRuleChanges();
		changes.updated = Arrays.asList(r1, r2);
		api.updates(changes);

		fetched = api.all();
		assertEquals(2, fetched.size());

		changes = new MapiRuleChanges();
		changes.deleted = Arrays.asList(r1.ruleId, r2.ruleId);
		api.updates(changes);

		fetched = api.all();
		assertEquals(0, fetched.size());

	}
}
