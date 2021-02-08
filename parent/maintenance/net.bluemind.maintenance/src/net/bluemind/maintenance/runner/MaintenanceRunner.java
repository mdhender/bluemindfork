/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.maintenance.runner;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.maintenance.IMaintenanceScript;
import net.bluemind.maintenance.MaintenanceScripts;

public class MaintenanceRunner {
	public static final Logger logger = LoggerFactory.getLogger(MaintenanceRunner.class);

	private MaintenanceRunner() {
	}

	private static boolean isDisabled() {
		return new File(System.getProperty("user.home") + "/no.pgmaintenance").exists();
	}

	public static boolean run(IServerTaskMonitor monitor) {
		if (isDisabled()) {
			monitor.log("pgmaintenance is disabled");
			logger.warn("pgmaintenance is disabled");
			return true;
		}
		boolean warnings = false;

		List<IMaintenanceScript> scripts = MaintenanceScripts.getMaintenanceScripts();
		logger.info("maintenance scripts: {} scripts", scripts.size());
		for (IMaintenanceScript maintenanceScript : scripts) {
			logger.info("running maintenance script {}", maintenanceScript);
			try {
				maintenanceScript.run(monitor);
			} catch (Exception e) {
				monitor.log("maintenance script " + maintenanceScript + " failed: " + e);
				logger.error("Maintenance script {} failed", maintenanceScript, e);
				warnings |= true;
			}
		}
		return warnings;
	}
}
