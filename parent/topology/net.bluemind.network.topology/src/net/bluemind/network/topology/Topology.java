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
package net.bluemind.network.topology;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.network.topology.impl.DefaultServiceTopology;
import net.bluemind.server.api.Server;

public class Topology {

	private static final Logger logger = LoggerFactory.getLogger(Topology.class);

	private static final CompletableFuture<Void> init = new CompletableFuture<>();
	private static IServiceTopology topology = null;

	private Topology() {
	}

	public static Optional<IServiceTopology> getIfAvailable() {
		return Optional.ofNullable(topology);
	}

	/**
	 * This method will block until an update is received
	 * 
	 * @return
	 */
	public static IServiceTopology get() {
		try {
			return init.thenApply(v -> topology).get(20, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw TopologyException.availabilityTimeout(e);
		}
	}

	public static void update(List<ItemValue<Server>> servers) {
		topology = new DefaultServiceTopology(servers);
		if (!init.isDone()) {
			logger.info("Initial topology {} received.", topology);
			init.complete(null);
		} else {
			logger.info("Updated topology {} received.", topology);
		}
	}
}
