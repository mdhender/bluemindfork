/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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

import java.util.LinkedList;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.VersionInfo;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.system.api.UpgradeReport;
import net.bluemind.system.schemaupgrader.SchemaUpgrade;
import net.bluemind.system.schemaupgrader.UpdateResult;
import net.bluemind.system.state.StateContext;

public class InstallationUpgradeTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(InstallationUpgradeTask.class);
	private VersionInfo to;
	private VersionInfo from;
	private DataSource pool;

	public InstallationUpgradeTask(BmContext context, VersionInfo from, VersionInfo to) {
		this.from = from;
		this.to = to;
		this.pool = context.getDataSource();
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(ServerSideServiceProvider.mailboxDataSource.size() + 1, "Begin upgrade");
		notifyUpgradeStatus("core.upgrade.start");

		doUpgradeForDataSource(pool, false, monitor.subWork(1));
		for (DataSource mbDS : ServerSideServiceProvider.mailboxDataSource.values()) {
			doUpgradeForDataSource(mbDS, true, monitor.subWork(1));
		}
		upgraders(monitor);
		notifyUpgradeStatus("core.upgrade.end");
	}

	private void doUpgradeForDataSource(DataSource pool, boolean onlySchema, IServerTaskMonitor monitor) {

		UpgradeReport report = new UpgradeReport();
		report.upgraders = new LinkedList<>();
		SchemaUpgrade schemaUpgrader = new SchemaUpgrade(pool, onlySchema);
		UpdateResult schemaUpgrade = schemaUpgrader.schemaUpgrade(monitor, report, from, to);
		if (schemaUpgrade.equals(UpdateResult.failed())) {
			throw new ServerFault("Upgrade failed !");
		}
	}

	private void notifyUpgradeStatus(String operation) {
		StateContext.setState(operation);
	}

	private void upgraders(IServerTaskMonitor monitor) throws Exception {

		logger.info("Updating using hooks");
		// FIXME hook upgrade ?
		// for (ISystemHook sh : hooks) {
		// logger.info("Upgrading " + sh);
		// sh.onUpgrade(at, previous, target);
		// }
		monitor.end(true, "Core upgrade complete.", "");
		logger.info("Core upgrade ending");
	}

}
