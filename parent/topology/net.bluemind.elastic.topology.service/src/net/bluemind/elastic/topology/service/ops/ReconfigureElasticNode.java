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
package net.bluemind.elastic.topology.service.ops;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.elastic.topology.service.EsTopology;
import net.bluemind.elastic.topology.service.config.ConfigurationBuilder;
import net.bluemind.elastic.topology.service.config.ConfigurationBuilderFactory;
import net.bluemind.elastic.topology.service.config.EsConfiguration;
import net.bluemind.elastic.topology.service.lifecycle.ElasticLifecycleManager;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.utils.NetworkHelper;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;

public class ReconfigureElasticNode implements IServerTask {

	private final ItemValue<Server> node;
	private final EsTopology esTopo;
	private final boolean resetData;
	private final ElasticLifecycleManager lifeMgr;

	public ReconfigureElasticNode(IServiceTopology esNodes, ElasticLifecycleManager lifeMgr, ItemValue<Server> seedNode,
			boolean resetData) {
		this.esTopo = new EsTopology(esNodes);
		this.lifeMgr = lifeMgr;
		this.node = seedNode;
		this.resetData = resetData;
	}

	@Override
	public CompletableFuture<Void> execute(IServerTaskMonitor monitor) {
		monitor.begin(1, "configure " + node);
		ConfigurationBuilder configBuild = ConfigurationBuilderFactory.newBuilder(node)//
				.withRoles(esTopo.nodeRoles(node.value))//
				.withSeedNodes(esTopo.seedAddresses());
		INodeClient nc = NodeActivator.get(node.value.address());
		monitor.log("Write config to " + node.value.address());

		lifeMgr.stop();

		if (resetData) {
			monitor.log("Resetting elastic data on {}", node);
			NCUtils.exec(nc, "rm -fr /var/spool/bm-elasticsearch/data/nodes", 2, TimeUnit.MINUTES);

			configBuild.bootstrapRequired();
		}

		EsConfiguration built = configBuild.build();
		nc.writeFile("/usr/share/bm-elasticsearch/config/elasticsearch.yml", built.configFile());

		monitor.log("{} should be stopped and configured", node.value.address());
		NetworkHelper nh = new NetworkHelper(node.value.address());
		nh.waitForClosedPort(9300, 30, TimeUnit.SECONDS);
		monitor.log("{} is no longer listening on 9300", node.value.address());

		lifeMgr.start();

		monitor.log("Waiting for port 9300 on {}...", node.value.address());
		nh.waitForListeningPort(9300, 60, TimeUnit.SECONDS);

		monitor.end(true, "Port 9300 available on " + node.value.address(), "ok");
		return CompletableFuture.completedFuture(null);
	}

}
