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

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;

public class TopologyChangePlan implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(TopologyChangePlan.class);
	private IServerTask[] tasks;

	public TopologyChangePlan(IServerTask... tasks) {
		this.tasks = tasks;
	}

	public CompletableFuture<Void> execute(IServerTaskMonitor mon) {
		if (tasks.length == 0) {
			logger.warn("Plan has {} tasks", tasks.length);
			return CompletableFuture.completedFuture(null);
		}
		var root = tasks[0].execute(mon);
		for (int i = 1; i < tasks.length; i++) {
			final int idx = i;
			root = root.thenCompose(v -> tasks[idx].execute(mon));
		}
		return root;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(TopologyChangePlan.class).add("tasks", tasks.length).toString();
	}

}
