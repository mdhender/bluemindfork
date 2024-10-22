/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.elastic.topology.service.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.common.task.Tasks;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.elastic.topology.service.TopologyChangePlan;
import net.bluemind.elastic.topology.service.TopologyChangePlanner;
import net.bluemind.elastic.topology.service.impl.EsTopologyService;
import net.bluemind.elastic.topology.service.tests.TopologySpawn.ContainerBasedTopology;
import net.bluemind.elastic.topology.service.tests.TopologySpawn.EsSpawnedNode;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;

public class SpawnElasticNodesTests {

	@Test
	public void planFor_2_data_1_master() throws URISyntaxException, IOException, InterruptedException {

		try (TopologySpawn ts = new TopologySpawn()) {
			ContainerBasedTopology topo = ts//
					.addNode(TagDescriptor.bm_es.getTag())//
					.addNode(TagDescriptor.bm_es_data.getTag())//
					.addNode(TagDescriptor.bm_es_data.getTag())//
					.build();
			assertNotNull(topo);

			configureCluster(topo);

			try {
				checkCluster(ts);
			} catch (Throwable t) {
				System.err.println("joining to debug ES states....");
				Thread.currentThread().join();
			}

		}

	}

	@Test
	public void planFor_1_multipurpose() throws URISyntaxException, IOException, InterruptedException {

		try (TopologySpawn ts = new TopologySpawn()) {
			ContainerBasedTopology topo = ts//
					.addNode(TagDescriptor.bm_es.getTag(), TagDescriptor.bm_es_data.getTag())//
					.build();
			assertNotNull(topo);

			configureCluster(topo);

			checkCluster(ts);

		}

	}

	@Test
	public void planFor_3_multipurpose() throws InterruptedException, URISyntaxException, IOException {

		try (TopologySpawn ts = new TopologySpawn()) {
			ContainerBasedTopology topo = ts//
					.addNode(TagDescriptor.bm_es.getTag(), TagDescriptor.bm_es_data.getTag())//
					.addNode(TagDescriptor.bm_es.getTag(), TagDescriptor.bm_es_data.getTag())//
					.addNode(TagDescriptor.bm_es.getTag(), TagDescriptor.bm_es_data.getTag())//
					.build();
			assertNotNull(topo);

			configureCluster(topo);

			Awaitility.await().atMost(1, TimeUnit.MINUTES).until(() -> {
				try {
					checkCluster(ts);
				} catch (Exception e) {
					e.printStackTrace(System.err);
					return false;
				}
				return true;
			});

		}

	}

	@Test
	public void planFor_2_multipurpose_then_reconfigure() throws InterruptedException, URISyntaxException, IOException {

		try (TopologySpawn ts = new TopologySpawn()) {
			ContainerBasedTopology topo = ts//
					.addNode(TagDescriptor.bm_es.getTag(), TagDescriptor.bm_es_data.getTag())//
					.addNode(TagDescriptor.bm_es.getTag(), TagDescriptor.bm_es_data.getTag())//
					.build();
			assertNotNull(topo);

			configureCluster(topo);

			Awaitility.await().atMost(1, TimeUnit.MINUTES).until(() -> {
				try {
					checkCluster(ts);
				} catch (Exception e) {
					e.printStackTrace(System.err);
					return false;
				}
				return true;
			});

			System.err.println("**************** RECONFIG ************* ");
			configureCluster(topo);

			System.err.println("**************** RECONFIG WITH API ************* ");
			List<ItemValue<Server>> withCore = new ArrayList<>();
			ItemValue<Server> core = ItemValue.create("core", Server.tagged("127.0.0.1", "bm/core"));
			withCore.add(core);
			withCore.addAll(ts.getNodes().stream().map(es -> es.srv()).toList());
			Topology.update(withCore);

			ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			EsTopologyService api = new EsTopologyService(prov.getContext(), Topology::get, ts.getLifecycle());
			System.err.println("Reconfigure....");
			TaskRef ref = api.resetAndReconfigure();
			TaskStatus status = Tasks
					.followStream(prov, LoggerFactory.getLogger(SpawnElasticNodesTests.class), null, ref)
					.orTimeout(4, TimeUnit.MINUTES).join();
			System.err.println("RECONFIG is done");
			assertTrue(status.state.succeed);
		}

	}

	private void checkCluster(TopologySpawn ts) throws URISyntaxException, IOException, InterruptedException {
		HttpClient jdkHC = HttpClient.newHttpClient();
		for (EsSpawnedNode n : ts.getNodes()) {
			if (n.srv().value.tags.contains(TagDescriptor.bm_es.getTag())) {
				jsonReq(jdkHC, n, "");
				jsonReq(jdkHC, n, "/_cluster/health");
				jsonReq(jdkHC, n, "/_nodes/usage");
			}
		}
	}

	private JsonObject jsonReq(HttpClient jdkHC, EsSpawnedNode n, String uri)
			throws URISyntaxException, IOException, InterruptedException {
		String fullUri = "http://" + n.srv().value.address() + ":9200" + uri;
		System.err.println("Try " + fullUri);
		var req = HttpRequest.newBuilder().GET().header("Accept", "application/json")//
				.uri(new URI(fullUri)).build();
		HttpResponse<String> result = jdkHC.send(req, BodyHandlers.ofString());
		JsonObject jsResp = new JsonObject(result.body());
		System.err.println(jsResp.encodePrettily());
		return jsResp;
	}

	private void configureCluster(ContainerBasedTopology cbt) {
		TopologyChangePlanner planner = new TopologyChangePlanner(cbt::topo, cbt.lifecycle());
		TopologyChangePlan reconfPlan = planner.reconfigureCluster();
		reconfPlan.execute(new TestsMonitor()).orTimeout(2, TimeUnit.MINUTES).join();
		System.err.println("GOOOOOOAAAAAALLLLLLLLLLL");
	}

}
