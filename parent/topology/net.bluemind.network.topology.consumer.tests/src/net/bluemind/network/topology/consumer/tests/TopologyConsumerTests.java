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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.network.topology.consumer.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.network.topology.TopologyException;
import net.bluemind.network.topology.dto.TopologyPayload;
import net.bluemind.server.api.Server;

public class TopologyConsumerTests {

	private Producer producer;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		this.producer = MQ.init().thenApply(v -> MQ.registerProducer("topology.updates")).get(30, TimeUnit.SECONDS);
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		System.err.println("-------- init completed ------");
	}

	@After
	public void after() throws Exception {
		if (producer != null) {
			producer.close();
		}
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void ensureChangesAreConsumed() {
		try {
			Topology.get();
			fail("Topology should timeout before receiving any update");
		} catch (TopologyException te) {
			assertTrue(te.getMessage().contains("in time"));
		}

		Server dumb = new Server();
		ItemValue<Server> item = ItemValue.create("dumb", dumb);
		dumb.ip = "172.16.17.1";
		dumb.tags = Arrays.asList("bm/core");
		List<ItemValue<Server>> fullList = Arrays.asList(item);
		JsonObject js = new JsonObject(JsonUtils.asString(TopologyPayload.of(fullList)));
		System.err.println("------- send new server's list");
		producer.send(js);

		IServiceTopology fresh = Topology.get();
		assertNotNull(fresh);
		assertTrue("dumb".equals(fresh.core().uid));

	}

}
