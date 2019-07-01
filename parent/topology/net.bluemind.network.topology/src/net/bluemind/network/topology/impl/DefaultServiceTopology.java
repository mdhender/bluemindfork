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
package net.bluemind.network.topology.impl;

import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.server.api.Server;

public class DefaultServiceTopology implements IServiceTopology {

	private boolean singleNode;
	private List<ItemValue<Server>> nodes;
	private ItemValue<Server> core;

	public DefaultServiceTopology(List<ItemValue<Server>> servers) {
		this.singleNode = servers.size() == 1;
		this.nodes = ImmutableList.copyOf(servers);
		if (singleNode) {
			core = servers.get(0);
		} else {
			core = any("bm/core");
		}
	}

	@Override
	public boolean singleNode() {
		return singleNode;
	}

	@Override
	public ItemValue<Server> core() {
		return core;
	}

	@Override
	public boolean imapOnDatalocation() {
		// on BM4, there is no IPS bullshit so we use the user's datalocation for imap
		// connections
		return true;
	}

	@Override
	public List<ItemValue<Server>> nodes() {
		return nodes;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(DefaultServiceTopology.class)//
				.add("node(s)", nodes().size())//
				.toString();
	}

}
