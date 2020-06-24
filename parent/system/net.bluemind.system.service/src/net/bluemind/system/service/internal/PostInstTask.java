/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.system.service.internal;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.system.schemaupgrader.ISchemaUpgradersProvider;
import net.bluemind.system.schemaupgrader.PostInst;
import net.bluemind.system.schemaupgrader.UpdateResult;

public class PostInstTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(PostInstTask.class);

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(2, "Running post-installation upgraders...");
		ISchemaUpgradersProvider upgradersProvider = ISchemaUpgradersProvider.getSchemaUpgradersProvider();

		if (!upgradersProvider.isActive()) {
			StringBuilder msg = new StringBuilder("*********************************************************");
			msg.append("* Post-installation upgraders are not active. Make sure your subscription is valid.");
			msg.append("*********************************************************");
			logger.warn("{}", msg);
			monitor.end(false, "Post-installation upgraders are not active. Make sure your subscription is valid", "");
			throw new ServerFault("Post-installation upgraders are not available");
		}

		monitor.progress(1, "Post-installation tasks found");

		List<PostInst> postinst = upgradersProvider.postInstJavaUpdaters();
		IServerTaskMonitor upgraderMonitor = monitor.subWork(postinst.size());

		try {
			postinst.stream().forEach(upgrader -> runUpgrader(upgraderMonitor, upgrader));
		} catch (ServerFault sf) {
			monitor.end(false, "Post-installation upgrade failed!", "");
		}
	}

	private void runUpgrader(IServerTaskMonitor monitor, PostInst upgrader) {
		UpdateResult updateResult = upgrader.executeUpdate(monitor);
		if (updateResult.equals(UpdateResult.failed())) {
			monitor.end(false, String.format("Post-installations upgrade %s fail!", upgrader.getClass().getName()), "");
			throw new ServerFault();
		}

		monitor.progress(1, String.format("Post-installation upgrade %s success!", upgrader.getClass().getName()));
	}
}
