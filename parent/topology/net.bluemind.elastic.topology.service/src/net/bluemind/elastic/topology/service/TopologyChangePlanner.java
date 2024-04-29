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
package net.bluemind.elastic.topology.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.elastic.topology.service.esclient.DumbElasticClient;
import net.bluemind.elastic.topology.service.lifecycle.ElasticLifecycleManager;
import net.bluemind.elastic.topology.service.lifecycle.LifecycleManagerFactory;
import net.bluemind.elastic.topology.service.ops.ReconfigureElasticNode;
import net.bluemind.elastic.topology.service.ops.StopElasticNode;
import net.bluemind.elastic.topology.service.ops.UpdateReplicationFactor;
import net.bluemind.elastic.topology.service.ops.WaitForTransportPort;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;

public class TopologyChangePlanner {

	private final Supplier<IServiceTopology> topoProvider;
	private final LifecycleManagerFactory lifeManager;
	private static final Logger logger = LoggerFactory.getLogger(TopologyChangePlanner.class);

	public TopologyChangePlanner(Supplier<IServiceTopology> esTopologyProvider, LifecycleManagerFactory lifeManager) {
		this.topoProvider = esTopologyProvider;
		this.lifeManager = lifeManager;
	}

	public TopologyChangePlanner(Supplier<IServiceTopology> esTopologyProvider) {
		this(esTopologyProvider, ElasticLifecycleManager::defaultManager);
	}

	public TopologyChangePlan reconfigureCluster() {
		IServiceTopology esNodes = topoProvider.get();
		List<IServerTask> toRun = new ArrayList<>();

		if (esNodes.nodes().size() == 1) {
			return new TopologyChangePlan(toRun.toArray(IServerTask[]::new));
		}

		List<ItemValue<Server>> allSeeds = esNodes.all(EsTopology.ES_TAG);
		List<ItemValue<Server>> dataOnly = esNodes.nodes().stream()
				.filter(s -> s.value.tags.contains(EsTopology.ES_DATA_TAG) && !s.value.tags.contains(EsTopology.ES_TAG))
				.toList();
		List<ItemValue<Server>> dataHolders = esNodes.nodes().stream()
				.filter(s -> s.value.tags.contains(EsTopology.ES_DATA_TAG)).toList();
		logger.info("Configuring ES topology with {} seed-nodes & {} data-only nodes.", allSeeds.size(),
				dataOnly.size());

		// we do our best to avoid clearing a node with data
		// we try to leave our first bm/es alone & all the nodes with a similar
		// clusterUUID
		Set<String> toBootstrap = nodesToBootstrap(esNodes);
		List<StopElasticNode> stopOps = esNodes.nodes().stream().filter(iv -> toBootstrap.contains(iv.uid))
				.map(iv -> new StopElasticNode(lifeManager.forNode(iv))).toList();
		toRun.addAll(stopOps);

		logger.info("The following nodes will bootstrap (data reset) => {}", toBootstrap);
		List<IServerTask> postBootstrap = new ArrayList<>();

		int replicationFactors = dataHolders.size() / 2 + 1;
		int extraCopies = replicationFactors - 1;

		// configure seed nodes
		for (ItemValue<Server> seedNode : allSeeds) {
			boolean reset = toBootstrap.contains(seedNode.uid);
			toRun.add(new ReconfigureElasticNode(esNodes, lifeManager.forNode(seedNode), seedNode, reset));
			if (reset) {
				postBootstrap.add(new ReconfigureElasticNode(esNodes, lifeManager.forNode(seedNode), seedNode, false));
			}
		}

		// configure data only nodes
		for (ItemValue<Server> dataNode : dataOnly) {
			boolean reset = toBootstrap.contains(dataNode.uid);
			toRun.add(new ReconfigureElasticNode(esNodes, lifeManager.forNode(dataNode), dataNode, reset));
			if (reset) {
				postBootstrap.add(new ReconfigureElasticNode(esNodes, lifeManager.forNode(dataNode), dataNode, false));
			}
		}
		toRun.addAll(postBootstrap);

		// wait again for all ports
		for (ItemValue<Server> node : esNodes.nodes()) {
			toRun.add(new WaitForTransportPort(node));
		}

		toRun.add(new UpdateReplicationFactor(allSeeds.getFirst(), extraCopies));

		return new TopologyChangePlan(toRun.toArray(IServerTask[]::new));
	}

	private Set<String> nodesToBootstrap(IServiceTopology esNodes) {
		ItemValue<Server> firstMaster = esNodes.nodes().stream().filter(iv -> iv.value.tags.contains(EsTopology.ES_TAG))
				.min((iv1, iv2) -> Long.compare(iv1.internalId, iv2.internalId)).orElseThrow();
		// if we don't want data the first master, clear it too
		boolean clearDataOnFirstMaster = !firstMaster.value.tags.contains(EsTopology.ES_DATA_TAG);

		if (clearDataOnFirstMaster) {
			INodeClient nc = NodeActivator.get(firstMaster.value.address());
			boolean hasData = nc.exists("/var/spool/bm-elasticsearch/data/nodes/0/indices/");
			if (!hasData) {
				logger.info("Master-only {} does not hold data, not resetting", firstMaster.value);
				clearDataOnFirstMaster = false;
			}
		}
		final boolean doClearFirst = clearDataOnFirstMaster;

		String preservedClusterUid = doClearFirst ? "preserve.nothing.%d".formatted(System.nanoTime())
				: clusterUUID(firstMaster.value);
		// do NOT bootstrap nodes that share the same cluster id as our first master
		return esNodes.nodes().stream()//
				.filter(iv -> iv.value.tags.contains(EsTopology.ES_TAG)
						|| iv.value.tags.contains(EsTopology.ES_DATA_TAG))
				.filter(iv -> doClearFirst || iv.internalId != firstMaster.internalId)
				.filter(iv -> !preservedClusterUid.equals(clusterUUID(iv.value)))//
				.map(iv -> iv.uid).collect(Collectors.toSet());
	}

	private String clusterUUID(Server value) {
		try (DumbElasticClient es = new DumbElasticClient(value.address())) {
			return es.clusterUUID().uuid();
		} catch (Exception e) {
			return "preserve.nothing." + System.nanoTime();
		}
	}

}
