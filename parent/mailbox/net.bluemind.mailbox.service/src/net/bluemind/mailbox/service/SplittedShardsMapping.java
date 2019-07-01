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
package net.bluemind.mailbox.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;

/**
 * This is useful for unit tests where pg & cyrus have 2 ips
 *
 */
public class SplittedShardsMapping {

	private static final Logger logger = LoggerFactory.getLogger(SplittedShardsMapping.class);

	private static final Map<String, ItemValue<Server>> altBackends = new ConcurrentHashMap<>();

	private SplittedShardsMapping() {
	}

	public static ItemValue<Server> remap(ItemValue<Server> srv) {
		ItemValue<Server> ret = srv;
		if (altBackends.containsKey(srv.value.address())) {
			ret = altBackends.get(srv.value.address());
		}
		if (ret != srv) {
			logger.info("Remapped {} to {}", srv, ret);
		}
		return ret;
	}

	public static void map(String shardAddressUid, String altBackendUid) {
		IServiceTopology topo = Topology.get();
		ItemValue<Server> source = topo.datalocation(shardAddressUid);
		ItemValue<Server> dest = topo.datalocation(altBackendUid);
		altBackends.put(source.value.address(), dest);
	}

}
