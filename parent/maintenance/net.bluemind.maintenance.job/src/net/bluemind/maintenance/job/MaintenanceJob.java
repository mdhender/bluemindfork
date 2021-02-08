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
package net.bluemind.maintenance.job;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.maintenance.runner.MaintenanceRunner;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class MaintenanceJob implements IScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(MaintenanceJob.class);
	private TaskRef ref;

	public MaintenanceJob() {
		// sonar: ok
	}

	@Override
	public String getDescription(String locale) {
		return "Maintenance";
	}

	@Override
	public String getJobId() {
		return "Maintenance";
	}

	@Override
	public void tick(IScheduler sched, boolean forced, String domainName, Date startDate) throws ServerFault {
		JobExitStatus exitStatus = JobExitStatus.SUCCESS;
		if (!forced) {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(startDate);
			if (gc.get(Calendar.MINUTE) != 0 || gc.get(Calendar.HOUR_OF_DAY) != 3) {
				logger.info("automatic mode, not running at {}", gc.getTime());
				return;
			}
		}

		IScheduledJobRunId slot = sched.requestSlot("global.virt", this, startDate);
		if (slot != null) {
			logger.info("Starting maintenance...");
			sched.info(slot, "en", "Starting maintenance");
			sched.info(slot, "fr", "Démarrage de la maintenance");
			try {
				if (MaintenanceRunner.run(new MaintenanceTaskMonitor(sched, slot))) {
					exitStatus = JobExitStatus.COMPLETED_WITH_WARNINGS;
				}
			} catch (Exception e) {
				logger.error("maintenance runner launch failed", e);
				exitStatus = JobExitStatus.FAILURE;
				throw e;
			} finally {
				sched.finish(slot, exitStatus);
			}
		}
	}

	@Override
	public void cancel() {
		if (ref != null) {
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITasksManager.class).cancel(ref.id);
		}
	}

	@Override
	public JobKind getType() {
		return JobKind.GLOBAL;
	}

	@Override
	public Set<String> getLockedResources() {
		return Collections.emptySet();
	}

	@Override
	public boolean supportsScheduling() {
		return true;
	}

}
