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
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.platform.Verticle;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.InstallationVersion;

public class JobSchedulerVerticle extends Verticle {
	private static final Logger logger = LoggerFactory.getLogger(JobSchedulerVerticle.class);

	private Map<String, IScheduledJob> jobs;

	@Override
	public void start() {
		SanitizeJobExecutions.sanitizeJobs();

		jobs = new HashMap<>();
		for (IScheduledJob bjp : JobRegistry.getBluejobs()) {
			jobs.put(bjp.getJobId(), bjp);
		}

		scheduleNext();

	}

	private void scheduleNext() {
		// 1 minutes
		getVertx().setPeriodic(1000 * 60, new Handler<Long>() {

			@Override
			public void handle(Long event) {
				executeJobs();
			}
		});
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
		if (new File(System.getProperty("user.home") + "/no.core.jobs").exists()) {
			return true;
		}
		if (versionMismatch()) {
			return true;
		}
		return false;
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

	@Override
	public void stop() {
	}

}
