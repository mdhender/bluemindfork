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

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.elastic.topology.service.lifecycle.ElasticLifecycleManager;
import net.bluemind.server.api.Server;

public class StopElasticNode implements IServerTask {

	private ItemValue<Server> node;
	private ElasticLifecycleManager lifeMgr;

	public StopElasticNode(ElasticLifecycleManager lifeMgr) {
		this.lifeMgr = lifeMgr;
	}

	@Override
	public CompletableFuture<Void> execute(IServerTaskMonitor monitor) {
		monitor.begin(1, "configure " + node);

		lifeMgr.stop();

		return CompletableFuture.completedFuture(null);
	}

}
