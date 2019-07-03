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
package net.bluemind.scheduledjob.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.scheduledjob.api.IJob;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class ExecutionsCleanerJob implements IScheduledJob {

	private static int JOB_EXECUTION_LIMIT = 100;

	private static final Logger logger = LoggerFactory.getLogger(ExecutionsCleanerJob.class);

	public static void setLimit(int limit) {
		JOB_EXECUTION_LIMIT = limit;
	}

	@Override
	public void tick(IScheduler sched, boolean plannedExecution, String domainName, Date startDate) throws ServerFault {
		IScheduledJobRunId rid = sched.requestSlot(domainName, this, startDate);

		try {
			IJob jobService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IJob.class);

			Collection<JobExecutionInfo> executions = searchJobExecutions(jobService);
			int count = purgeExpiredJobExecutions(jobService, executions);
			sched.info(rid, "en", "" + count + " JobExecutions processed.");
			sched.info(rid, "fr", "" + count + " JobExecutions traîtés.");

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			sched.finish(rid, JobExitStatus.FAILURE);
		}

		sched.finish(rid, JobExitStatus.SUCCESS);
	}

	private int purgeExpiredJobExecutions(IJob jobService, Collection<JobExecutionInfo> executions) throws ServerFault {
		int count = 0;
		logger.debug("Checking {} job executions", executions.size());
		for (JobExecutionInfo jobExecutionInfo : executions) {
			List<JobExecution> expiredJobExecutions = jobExecutionInfo.getExpiredJobExecutions();
			List<Integer> toDelete = expiredJobExecutions.stream().map(je -> je.id).collect(Collectors.toList());
			jobService.deleteExecutions(toDelete);
			count += toDelete.size();
		}
		return count;
	}

	private Collection<JobExecutionInfo> searchJobExecutions(IJob jobService) throws ServerFault {
		JobExecutionQuery query = new JobExecutionQuery();
		query.active = false;
		ListResult<JobExecution> allExecutions = jobService.searchExecution(query);
		Map<String, JobExecutionInfo> executionInfos = new HashMap<>();
		for (JobExecution execution : allExecutions.values) {
			String mapKey = execution.domainName + ":" + execution.jobId;
			JobExecutionInfo info;
			if (executionInfos.containsKey(mapKey)) {
				info = executionInfos.get(mapKey);
			} else {
				info = new JobExecutionInfo();
			}
			info.addExecution(execution);
			executionInfos.put(mapKey, info);
		}
		return executionInfos.values();
	}

	@Override
	public JobKind getType() {
		return JobKind.GLOBAL;
	}

	@Override
	public String getDescription(String locale) {
		if ("fr".equals(locale)) {
			return "Vérifie que l'on conserve au maximum 100 exécutions pour chaque job / domaine";
		} else {
			return "Ensure you don't keep more than 100 executions record for each job / domain";
		}
	}

	@Override
	public String getJobId() {
		return getClass().getName();
	}

	private static class JobExecutionInfo {
		private List<JobExecution> executions;

		public JobExecutionInfo() {
			this.executions = new ArrayList<>();
		}

		private void sort() {
			Collections.sort(executions, new ReverseExecutionComparator());
		}

		private List<JobExecution> getExpiredJobExecutions() {
			if (!exceedsLimit()) {
				return Collections.emptyList();
			}

			sort();
			return executions.subList(JOB_EXECUTION_LIMIT, executions.size());
		}

		private void addExecution(JobExecution execution) {
			this.executions.add(execution);
		}

		private boolean exceedsLimit() {
			return executions.size() > JOB_EXECUTION_LIMIT;
		}
	}

	private static class ReverseExecutionComparator implements Comparator<JobExecution> {

		@Override
		public int compare(JobExecution o1, JobExecution o2) {
			return o2.id - o1.id;
		}
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
