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

import java.util.List;
import java.util.function.Supplier;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class ApiTopologyProvider implements Supplier<IServiceTopology> {

	private IServiceProvider sp;

	public ApiTopologyProvider(IServiceProvider sp) {
		this.sp = sp;
	}

	@Override
	public IServiceTopology get() {
		return loadElasticTopology();
	}

	private IServiceTopology loadElasticTopology() {
		IServer srvApi = sp.instance(IServer.class, "default");
		List<ItemValue<Server>> fullTopo = srvApi.allComplete();
		List<ItemValue<Server>> esTopo = srvApi.allComplete().stream()
				.filter(s -> s.value.tags.contains(EsTopology.ES_TAG) || s.value.tags.contains(EsTopology.ES_DATA_TAG))
				.toList();
		return new IServiceTopology() {

			@Override
			public boolean singleNode() {
				return fullTopo.size() == 1;
			}

			@Override
			public List<ItemValue<Server>> nodes() {
				return esTopo;
			}

			@Override
			public boolean imapOnDatalocation() {
				throw new UnsupportedOperationException();
			}

			@Override
			public ItemValue<Server> core() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
