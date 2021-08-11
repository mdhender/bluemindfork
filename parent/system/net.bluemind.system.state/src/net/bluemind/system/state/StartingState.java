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
package net.bluemind.system.state;

import java.io.File;

import net.bluemind.config.Token;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.InstallationVersion;
import net.bluemind.system.api.SystemState;

public class StartingState extends State {

	@Override
	public State stateChange(String operation) {
		switch (operation) {
		case "core.started":
			if (!Token.exists()) {
				return new MaintenanceNotInstalledState();
			} else if (kafkaConfigured(operation) && cloningStarted()) {
				return new CloningState();
			} else if (needsUpgrade()) {
				return new MaintenanceUpgradeState();
			}
			return new RunningState();
		case "core.cloning.start":
			if (kafkaConfigured(operation)) {
				return new CloningState();
			}
			return new RunningState();
		default:
			return super.stateChange(operation);
		}

	}

	private boolean kafkaConfigured(String op) {
		boolean confLooksOk = System.getProperty("bm.kafka.bootstrap.servers") != null
				|| new File("/etc/bm/kafka.properties").exists();
		if (!confLooksOk) {
			logger.warn(
					"\"bm.kafka.bootstrap.servers\" system prop is not set & /etc/bm/kafka.properties does not exist for transition '{}'",
					op);
		}
		return confLooksOk;
	}

	private boolean cloningStarted() {
		return new File("/etc/bm/continuous.clone").exists();
	}

	private boolean needsUpgrade() {
		try {
			InstallationVersion version = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IInstallation.class).getVersion();
			return version.needsUpgrade;
		} catch (Exception e) {
			logger.warn("Cannot determine if upgrade is needed: {}", e.getMessage());
			return false;
		}

	}

	@Override
	public SystemState getSystemState() {
		return SystemState.CORE_STATE_STARTING;
	}

}
