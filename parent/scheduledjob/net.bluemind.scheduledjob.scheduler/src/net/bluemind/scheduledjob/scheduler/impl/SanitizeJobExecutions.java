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

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.scheduledjob.api.IJob;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.persistence.ScheduledJobStore;

public class SanitizeJobExecutions {

	private static final Logger logger = LoggerFactory.getLogger(SanitizeJobExecutions.class);

	public static void sanitizeJobs() {
		try {
			IJob jobService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IJob.class);
			JobExecutionQuery query = new JobExecutionQuery();
			// query all jobs which are not active but IN_PROGRESS
			query.active = false;
			Set<JobExitStatus> stat = new HashSet<>();
			stat.add(JobExitStatus.IN_PROGRESS);
			query.statuses = stat;
			ScheduledJobStore store = new ScheduledJobStore(JdbcActivator.getInstance().getDataSource());
			jobService.searchExecution(query).values.forEach(je -> {
				logger.info("Marking Job execution {} as interrupted", je.id);
				je.status = JobExitStatus.INTERRUPTED;
				store.updateExecution(je);
			});
		} catch (Exception e) {
			logger.warn("Cannot sanitize job executions: {}", e.getMessage());
		}
	}

}
