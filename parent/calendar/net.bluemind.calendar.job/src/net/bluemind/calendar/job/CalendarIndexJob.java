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
package net.bluemind.calendar.job;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.service.IInCoreCalendarsMgmt;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;
import net.bluemind.scheduledjob.scheduler.SchedulerProgressMonitor;

public class CalendarIndexJob implements IScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(CalendarIndexJob.class);

	@Override
	public void tick(IScheduler sched, boolean plannedExecution, String domainName, Date startDate) throws ServerFault {
		if (!plannedExecution) {
			logger.debug("Not planned");
			return;
		}

		IScheduledJobRunId rid = sched.requestSlot(domainName, this, startDate);

		if (rid != null) {
			try {
				run(sched, rid);
				sched.finish(rid, JobExitStatus.SUCCESS);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				sched.error(rid, "en", e.getMessage());
				sched.error(rid, "fr", e.getMessage());
				sched.finish(rid, JobExitStatus.FAILURE);
			}
		}

	}

	private void run(IScheduler sched, IScheduledJobRunId rid) throws Exception {
		BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

		IInCoreCalendarsMgmt calMgmt = context.provider().instance(IInCoreCalendarsMgmt.class);

		calMgmt.reindexAll(new SchedulerProgressMonitor(sched, rid));
	}

	@Override
	public JobKind getType() {
		return JobKind.GLOBAL;
	}

	@Override
	public String getDescription(String locale) {
		if ("fr".equals(locale)) {
			return "Indexation des calendriers";
		}
		return "Calendars indexing";
	}

	@Override
	public String getJobId() {
		return getClass().getName();
	}

	@Override
	public Set<String> getLockedResources() {
		return Collections.emptySet();
	}

	@Override
	public boolean supportsScheduling() {
		return false;
	}

}
