/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.elastic.topology.service.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider.IServerSideServiceFactory;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.elastic.topology.api.EsTopologyApi;
import net.bluemind.elastic.topology.service.ApiTopologyProvider;
import net.bluemind.elastic.topology.service.TopologyChangePlan;
import net.bluemind.elastic.topology.service.TopologyChangePlanner;
import net.bluemind.elastic.topology.service.lifecycle.ElasticLifecycleManager;
import net.bluemind.elastic.topology.service.lifecycle.LifecycleManagerFactory;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;

public class EsTopologyService implements EsTopologyApi {

	private final BmContext ctx;
	private final Supplier<IServiceTopology> topoProv;
	private final LifecycleManagerFactory life;

	public EsTopologyService(BmContext ctx) {
		this(ctx, new ApiTopologyProvider(ctx.su().provider()), ElasticLifecycleManager::defaultManager);
	}

	public EsTopologyService(BmContext ctx, Supplier<IServiceTopology> sup, LifecycleManagerFactory life) {
		this.ctx = ctx;
		this.topoProv = sup;
		this.life = life;
	}

	@Override
	public TaskRef resetAndReconfigure() {
		if (!ctx.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("only admin0 can reconfigure cluster", ErrorCode.NOT_GLOBAL_ADMIN);
		}

		ITasksManager mgr = ctx.provider().instance(ITasksManager.class);
		return mgr.run(new ReconfTask(topoProv, life));
	}

	private static class ReconfTask implements IServerTask {

		private final Supplier<IServiceTopology> topoProv;
		private LifecycleManagerFactory life;

		public ReconfTask(Supplier<IServiceTopology> topoProv, LifecycleManagerFactory life) {
			this.topoProv = topoProv;
			this.life = life;
		}

		@Override
		public CompletableFuture<Void> execute(IServerTaskMonitor monitor) {
			CompletableFuture<Void> end = new CompletableFuture<>();
			monitor.begin(2.0, "Starting");

			List<ItemValue<Server>> esNodes = Topology.get().nodes().stream()
					.filter(iv -> iv.value.tags.contains(TagDescriptor.bm_es.getTag())
							|| iv.value.tags.contains(TagDescriptor.bm_es_data.getTag()))
					.toList();

			for (var n : esNodes) {
				ElasticLifecycleManager mgr = life.forNode(n);
				mgr.stop();
				INodeClient nc = NodeActivator.get(n.value.address());
				NCUtils.exec(nc, "cp", "/usr/share/bm-elasticsearch/config/elasticsearch.yml.single-node-defaults",
						"/usr/share/bm-elasticsearch/config/elasticsearch.yml");
				NCUtils.exec(nc, "rm", "-fr", "/var/spool/bm-elasticsearch/data/nodes");
				mgr.start();
			}
			monitor.progress(1.0, "Default configuration is back in place");

			TopologyChangePlanner planner = new TopologyChangePlanner(topoProv);
			TopologyChangePlan plan = planner.reconfigureCluster();
			plan.execute(monitor.subWork(1));

			monitor.end(true, "Finished", "{}");
			end.complete(null);
			return end;
		}

	}

	public static class Factory implements IServerSideServiceFactory<EsTopologyApi> {

		@Override
		public Class<EsTopologyApi> factoryClass() {
			return EsTopologyApi.class;
		}

		@Override
		public EsTopologyApi instance(BmContext context, String... params) throws ServerFault {
			return new EsTopologyService(context);
		}

	}

}
