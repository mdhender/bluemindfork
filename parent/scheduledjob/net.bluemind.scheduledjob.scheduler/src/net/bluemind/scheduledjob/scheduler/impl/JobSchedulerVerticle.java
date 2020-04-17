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
package net.bluemind.scheduledjob.scheduler.impl;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.InstallationVersion;

public class JobSchedulerVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(JobSchedulerVerticle.class);

	@Override
	public void start() {
		SanitizeJobExecutions.sanitizeJobs();

		for (IScheduledJob bjp : JobRegistry.getBluejobs()) {
			logger.debug("{} registered.", bjp);
		}

		scheduleNext();

	}

	private void scheduleNext() {
		// 1 minutes
		getVertx().setPeriodic(1000 * 60L, (Long event) -> executeJobs());
	}

	protected void executeJobs() {
		if (isDisabled()) {
			logger.warn("core jobs are disabled");
			return;
		}
		for (IScheduledJob bjp : JobRegistry.getBluejobs()) {
			JobRunner runner = new JobRunner(bjp, false, null);
			runner.run();
		}
	}

	protected boolean isDisabled() {
		return !(new File(System.getProperty("user.home") + "/no.core.jobs").exists() || versionMismatch());
	}

	private boolean versionMismatch() {
		boolean ret = true;

		String dbVersion = null;
		String coreVersion = null;
		try {
			InstallationVersion version = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IInstallation.class).getVersion();
			coreVersion = version.softwareVersion;
			dbVersion = version.databaseVersion;

		} catch (Exception e) {
			logger.error("error during version retrieving : {}", e.getMessage());
			return true;
		}

		if (dbVersion == null) {
			logger.info("No db version");
			ret = true;
		} else if (dbVersion.equals(coreVersion)) {
			ret = false;
		} else {
			logger.info("Versions mismatch. db has '{}' while core is '{}'", dbVersion, coreVersion);
		}
		return ret;
	}

}
