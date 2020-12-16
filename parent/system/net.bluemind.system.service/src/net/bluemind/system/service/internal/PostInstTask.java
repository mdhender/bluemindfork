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
import net.bluemind.system.schemaupgrader.PostInst;
import net.bluemind.system.schemaupgrader.UpdateResult;

public class PostInstTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(PostInstTask.class);

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(1, "Running post-installation upgraders...");

		List<PostInst> postinst = PostInstTasks.postInstJavaUpdaters();
		try {
			postinst.stream().forEach(
					upgrader -> runUpgrader(monitor.subWork(upgrader.getClass().getName(), postinst.size()), upgrader));
		} catch (ServerFault sf) {
			logger.error(sf.getMessage(), sf);
			monitor.end(false, "upgrader failed", sf.getMessage());
			throw sf;
		}
		monitor.end(true, "upgraders finished successfully", null);
	}

	private void runUpgrader(IServerTaskMonitor submonitor, PostInst upgrader) {
		UpdateResult updateResult = upgrader.executeUpdate(submonitor);
		if (updateResult.equals(UpdateResult.failed())) {
			throw new ServerFault("upgrader " + upgrader.getClass().getName() + " failed");
		}
		submonitor.end(true, String.format("upgrader %s finished successfully", upgrader.getClass().getName()), null);
	}
}
