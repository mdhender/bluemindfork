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

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.common.task.Tasks;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;

public class EsTopologyHook extends DefaultServerHook {

	private static final Logger logger = LoggerFactory.getLogger(EsTopologyHook.class);
	private static final AtomicBoolean SUSPENDED = new AtomicBoolean();
	private static final Set<String> ES_TAGS = Set.of(EsTopology.ES_TAG, EsTopology.ES_DATA_TAG);

	public static void pause() {
		SUSPENDED.set(true);
	}

	public static void resume() {
		SUSPENDED.set(false);
	}

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		reconfAfterSingleChange(context, tag);
	}

	private void reconfAfterSingleChange(BmContext context, String tag) {
		if (SUSPENDED.get()) {
			return;
		}
		if (!ES_TAGS.contains(tag)) {
			return;
		}
		TopologyChangePlanner planner = new TopologyChangePlanner(new ApiTopologyProvider(context.su().provider()));
		TopologyChangePlan plan = planner.reconfigureCluster();
		logger.info("Apply {} after elastic topology change", plan);
		ITasksManager mgr = context.provider().instance(ITasksManager.class);
		TaskRef taskref = mgr.run(plan);
		CompletableFuture<TaskStatus> completion = Tasks.followStream(context.provider(), logger, "ES-topology",
				taskref);
		completion.whenComplete((status, ex) -> {
			if (ex != null) {
				logger.error("Error changing elastic topology", ex);
			} else {
				logger.info("ES topology update finished {}", status);
			}
		});
	}

}
