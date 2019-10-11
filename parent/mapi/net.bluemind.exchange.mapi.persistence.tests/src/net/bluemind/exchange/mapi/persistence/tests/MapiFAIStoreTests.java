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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.exchange.mapi.api.MapiFAI;
import net.bluemind.exchange.mapi.api.MapiFAIContainer;
import net.bluemind.exchange.mapi.persistence.MapiFAIStore;

public class MapiFAIStoreTests {

	private String faiContainerId;
	private MapiFAIStore mapiFAIStore;
	private ItemStore faiItemStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		faiContainerId = MapiFAIContainer.getIdentifier("fake_replica");
		Container fais = Container.create(faiContainerId, MapiFAIContainer.TYPE, faiContainerId, "me", true);
		fais = containerStore.create(fais);

		mapiFAIStore = new MapiFAIStore(JdbcTestHelper.getInstance().getDataSource(), fais);
		faiItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), fais, securityContext);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void createFAI() throws Exception {
		String uid = "" + System.currentTimeMillis();
		Item item = Item.create(uid, null);
		item = faiItemStore.create(item);
		MapiFAI fai = new MapiFAI();
		fai.folderId = "42";
		fai.faiJson = "{}";
		mapiFAIStore.create(item, fai);
		MapiFAI got = mapiFAIStore.get(item);
		assertEquals(fai.faiJson, got.faiJson);
		assertEquals(fai.folderId, got.folderId);
	}

	@Test
	public void updateFAI() throws Exception {
		String uid = "" + System.currentTimeMillis();
		Item item = Item.create(uid, null);
		item = faiItemStore.create(item);
		MapiFAI fai = new MapiFAI();
		fai.folderId = "42";
		fai.faiJson = "{}";
		mapiFAIStore.create(item, fai);
		String jsContent = new JsonObject().putString("toto", "titi").putNumber("poum", 42).encode();
		fai.faiJson = jsContent;
		mapiFAIStore.update(item, fai);
		MapiFAI got = mapiFAIStore.get(item);
		JsonObject parsed = new JsonObject(got.faiJson);
		assertEquals("titi", parsed.getString("toto"));
		assertEquals(42, parsed.getNumber("poum").intValue());
	}

}
