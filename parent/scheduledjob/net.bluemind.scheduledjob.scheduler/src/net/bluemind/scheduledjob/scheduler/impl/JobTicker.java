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

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.scheduledjob.api.IJob;
import net.bluemind.scheduledjob.api.InProgressException;
import net.bluemind.scheduledjob.api.Job;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.PlanKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;

public class JobTicker implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(JobTicker.class);
	private IScheduledJob bj;
	private Scheduler sch;
	private boolean forced;
	private String domainName;
	private Date date;
	private String execGroup;

	public JobTicker(IScheduledJob bjp, boolean forced, String domainName, Date cur, String groupId) {
		this.bj = bjp;
		this.sch = Scheduler.get();
		this.forced = forced;
		this.domainName = domainName;
		this.date = cur;
		this.execGroup = groupId;
	}

	@Override
	public void run() {
		try {
			Set<String> lockedResources = sch.checkLockedResources(domainName, bj);
			if (!forced && !lockedResources.isEmpty()) {
				logger.info("Job {} waits for {} blocked resources", bj.getJobId(), lockedResources.size());
				return;
			}
			sch.setActiveGroup(execGroup);
			IScheduledJobRunId slot = sch.getActiveSlot(domainName, bj.getJobId());
			if (slot == null) {
				bj.tick(sch, forced, domainName, date);
				resetJobSchedule(bj);
				finishIfNeeded(null);
			}
		} catch (InProgressException ipe) {
			logger.warn("job {}@{} already in progress.", bj.getJobId(), domainName);
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
			finishIfNeeded(e);
		} catch (Exception t) {
			logger.error(t.getMessage(), t);
			finishIfNeeded(t);
		}

	}

	private void resetJobSchedule(IScheduledJob job) {
		if (!job.supportsScheduling()) {
			try {
				IJob jobService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IJob.class);
				Job storedJob = jobService.getJobFromId(job.getJobId());
				if (!storedJob.domainPlanification.isEmpty()) {
					boolean scheduled = storedJob.domainPlanification.stream().filter(plan -> {
						return plan.kind == PlanKind.SCHEDULED;
					}).findFirst().isPresent();
					if (scheduled) {
						// let the store set the default plan
						storedJob.domainPlanification = Collections.emptyList();
						jobService.update(storedJob);
					}
				}
			} catch (ServerFault e) {
				logger.warn("Cannot reset job scheduling", e);
			}
		}
	}

	private void finishIfNeeded(Throwable t) {
		RunIdImpl slot = (RunIdImpl) sch.getActiveSlot(domainName, bj.getJobId());
		if (slot != null) {
			if (t != null) {
				sch.error(slot, "en", "Job halted by exception: " + t.getMessage());
				sch.error(slot, "fr", "Tâche stoppée par une exception: " + t.getMessage());
				sch.finish(slot, JobExitStatus.FAILURE);
			} else if (slot.status == JobExitStatus.IN_PROGRESS) {
				sch.warn(slot, "en", "Job completed without proper status reporting");
				sch.warn(slot, "fr", "Tâche terminée sans avoir indiquée son statut");
				sch.finish(slot, JobExitStatus.COMPLETED_WITH_WARNINGS);
			}
		}
	}

}
