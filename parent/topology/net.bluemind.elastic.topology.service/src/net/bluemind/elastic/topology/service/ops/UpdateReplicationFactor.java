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
import net.bluemind.elastic.topology.service.esclient.DumbElasticClient;
import net.bluemind.server.api.Server;

public class UpdateReplicationFactor implements IServerTask {

	private ItemValue<Server> node;
	private int extraCopies;

	public UpdateReplicationFactor(ItemValue<Server> node, int extraCopies) {
		this.node = node;
		this.extraCopies = extraCopies;
	}

	@Override
	public CompletableFuture<Void> execute(IServerTaskMonitor monitor) {
		try (DumbElasticClient es = new DumbElasticClient(node.value.address())) {
			monitor.log("Update required extra copies to {}", extraCopies);
			es.setNumberOfCopies(extraCopies);
		}
		return CompletableFuture.completedFuture(null);
	}

}
