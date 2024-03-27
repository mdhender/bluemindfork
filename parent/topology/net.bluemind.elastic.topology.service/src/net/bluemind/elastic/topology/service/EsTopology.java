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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;

public class EsTopology {

	public static final String ES_TAG = TagDescriptor.bm_es.getTag();
	public static final String ES_DATA_TAG = TagDescriptor.bm_es_data.getTag();

	public enum NodeRole {
		DATA, MASTER, INGEST
	}

	private IServiceTopology esNodes;

	public EsTopology(IServiceTopology esNodes) {
		this.esNodes = esNodes;
	}

	public List<String> seedAddresses() {
		return esNodes.all(TagDescriptor.bm_es.getTag()).stream().map(s -> s.value.address()).toList();
	}

	public Set<NodeRole> nodeRoles(Server s) {
		EnumSet<NodeRole> ret = EnumSet.noneOf(NodeRole.class);
		if (s.tags.contains(TagDescriptor.bm_es.getTag())) {
			ret.add(NodeRole.MASTER);
		}
		if (s.tags.contains(TagDescriptor.bm_es_data.getTag())) {
			ret.add(NodeRole.DATA);
		}
		return ret;
	}

}
