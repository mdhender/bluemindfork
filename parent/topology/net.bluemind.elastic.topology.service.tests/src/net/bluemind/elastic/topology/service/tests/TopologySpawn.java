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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.elastic.topology.service.lifecycle.ElasticLifecycleManager;
import net.bluemind.elastic.topology.service.lifecycle.LifecycleManagerFactory;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.server.api.Server;

public class TopologySpawn implements AutoCloseable {

	static {
		System.setProperty("ahcnode.fail.https.ok", "true");
		System.setProperty("elastic.config.memlock", "false");
	}

	public static record EsSpawnedNode(ItemValue<Server> srv, ElasticNode container) {

	}

	public static record ContainerBasedTopology(IServiceTopology topo, LifecycleManagerFactory lifecycle) {

	}

	private final List<EsSpawnedNode> nodes;
	private final AtomicInteger nodeIdx;

	public TopologySpawn() {
		this.nodes = new ArrayList<>();
		this.nodeIdx = new AtomicInteger(0);
	}

	public LifecycleManagerFactory getLifecycle() {
		return new LifecycleManagerFactory() {

			@Override
			public ElasticLifecycleManager forNode(ItemValue<Server> node) {
				EsSpawnedNode spawned = nodes.stream().filter(spn -> spn.srv == node).findAny().orElseThrow();
				return new ElasticLifecycleManager(spawned.srv()) {

					@Override
					public void stop() {
						spawned.container().stopElastic();
					}

					@Override
					public void start() {
						spawned.container.restartElastic();
					}
				};
			}
		};
	}

	public TopologySpawn addNode(String... tags) {
		ElasticNode en = new ElasticNode();
		en.start();
		Server srv = Server.tagged(en.inspectAddress(), tags);
		long id = nodeIdx.incrementAndGet();
		srv.name = "es-" + id;
		ItemValue<Server> iv = ItemValue.create(srv.name, srv);
		iv.internalId = id;
		nodes.add(new EsSpawnedNode(iv, en));

		return this;
	}

	public ContainerBasedTopology build() {
		IServiceTopology topo = new IServiceTopology() {

			@Override
			public boolean singleNode() {
				return nodes.size() == 1;
			}

			@Override
			public List<ItemValue<Server>> nodes() {
				return nodes.stream().map(EsSpawnedNode::srv).toList();
			}

			@Override
			public boolean imapOnDatalocation() {
				return false;
			}

			@Override
			public ItemValue<Server> core() {
				return null;
			}
		};
		LifecycleManagerFactory lifecycle = getLifecycle();
		return new ContainerBasedTopology(topo, lifecycle);
	}

	@Override
	public void close() {
		nodes.stream().map(EsSpawnedNode::container).forEach(ElasticNode::stop);
	}

	public List<EsSpawnedNode> getNodes() {
		return nodes;
	}

}
