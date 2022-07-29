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
package net.bluemind.network.topology.producer.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class TopologyProducerTests {

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void ensureTopologyIsDispatched() throws Exception {

		BmConfIni ini = new BmConfIni();

		Server imapServer = new Server();
		imapServer.ip = ini.get("imap-role");
		imapServer.tags = Arrays.asList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		IServiceTopology currentTopology = Topology.get();
		assertNotNull(currentTopology);

		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IServer serverApi = prov.instance(IServer.class, "default");
		Optional<ItemValue<Server>> anyServer = serverApi.allComplete().stream().findAny();
		assertTrue(anyServer.isPresent());
		List<String> newTags = new ArrayList<>(anyServer.get().value.tags);
		newTags.add("fake/news");

		CompletableFuture<Void> latch = new CompletableFuture<>();
		LatchHook.currentPromise = latch;
		System.err.println("---------- Before changing tags ---------");
		serverApi.setTags(anyServer.get().uid, newTags);
		latch.get(5, TimeUnit.SECONDS);
		System.err.println("---------- latch unlocks ---------");
		IServiceTopology refreshed = Topology.get();
		boolean hasFreshTag = refreshed.nodes().stream().anyMatch(iv -> iv.value.tags.contains("fake/news"));
		assertTrue(hasFreshTag);
	}

}
