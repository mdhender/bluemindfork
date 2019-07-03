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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.server.api.Server;

public interface IServiceTopology {

	/**
	 * @return true if the installation sits on single node
	 */
	boolean singleNode();

	ItemValue<Server> core();

	/**
	 * @return true if imap access can target user's datalocation
	 */
	boolean imapOnDatalocation();

	List<ItemValue<Server>> nodes();

	/**
	 * Search a server uid (aka datalocation) in current topology
	 * 
	 * @param serverUid
	 * @return the server with the given uid
	 * @throws TopologyException if not found
	 */
	default ItemValue<Server> datalocation(String serverUid) {
		return nodes().stream().filter(iv -> iv.uid.equals(serverUid)).findFirst()
				.orElseThrow(() -> new TopologyException("server uid " + serverUid + " unknown"));
	}

	/**
	 * @param tag
	 * @return a random server with the given tag
	 * @throws TopologyException if none match
	 */
	default ItemValue<Server> any(String tag) {
		List<ItemValue<Server>> servers = nodes().stream().filter(si -> si.value.tags.contains(tag))
				.collect(Collectors.toCollection(ArrayList::new));
		if (servers.isEmpty()) {
			throw TopologyException.missingTag(tag);
		} else {
			return servers.get(ThreadLocalRandom.current().nextInt(servers.size()));
		}
	}

	default Optional<ItemValue<Server>> anyIfPresent(String tag) {
		return nodes().stream().filter(si -> si.value.tags.contains(tag)).findAny();
	}

}
