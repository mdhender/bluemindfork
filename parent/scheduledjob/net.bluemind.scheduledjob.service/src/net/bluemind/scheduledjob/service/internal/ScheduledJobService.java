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
package net.bluemind.scheduledjob.service.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.scheduledjob.api.IInCoreJob;
import net.bluemind.scheduledjob.api.Job;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.api.JobPlanification;
import net.bluemind.scheduledjob.api.JobQuery;
import net.bluemind.scheduledjob.api.LogEntry;
import net.bluemind.scheduledjob.api.PlanKind;
import net.bluemind.scheduledjob.persistence.ScheduledJobStore;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.impl.JobRegistry;
import net.bluemind.scheduledjob.scheduler.impl.RunIdImpl;
import net.bluemind.scheduledjob.scheduler.impl.Scheduler;

public class ScheduledJobService implements IInCoreJob {

	private static final Logger logger = LoggerFactory.getLogger(ScheduledJobService.class);

	private ScheduledJobStore store;
	private SecurityContext context;

	public ScheduledJobService(BmContext context) {
		store = new ScheduledJobStore(context.getDataSource());
		this.context = context.getSecurityContext();
	}

	@Override
	public ListResult<Job> searchJob(JobQuery query) throws ServerFault {
		if (!context.isAdmin()) {
			throw new ServerFault("ScheduledJobService.searchJob is only available to admin0 or domain admin",
					ErrorCode.PERMISSION_DENIED);
		}

		ListResult<Job> ret = new ListResult<Job>();
		Collection<IScheduledJob> jobs = JobRegistry.getBluejobs();
		ArrayList<Job> values = new ArrayList<Job>(jobs.size());
		for (IScheduledJob bj : jobs) {
			if (!context.isDomainGlobal() && bj.getType() == JobKind.GLOBAL) {
				// do not allow admin to see global jobs
				continue;
			}

			if (query.domain != null && bj.getType() == JobKind.GLOBAL) {
				continue;
			}

			values.add(convertJob(bj));
		}
		store.loadStatusesAndPlans(context, query, values);

		// filter status
		if (query.statuses != null && query.statuses.size() < 3) {
			Iterator<Job> it = values.iterator();
			while (it.hasNext()) {
				Job job = it.next();
				if (job.domainPlanification.isEmpty()) {
					it.remove();
				}
			}
		}

		// add planification for never executed global jobs
		Iterator<Job> it = values.iterator();
		while (it.hasNext()) {
			Job job = it.next();
			if (job.domainPlanification.isEmpty() && context.isDomainGlobal() && job.kind == JobKind.GLOBAL) {
				JobPlanification jp = new JobPlanification();
				jp.kind = PlanKind.OPPORTUNISTIC;
				jp.domain = context.getContainerUid();
				job.domainPlanification.add(jp);
			}
		}
		ret.values = values;
		ret.total = values.size();
		return ret;
	}

	@Override
	public ListResult<JobExecution> searchExecution(JobExecutionQuery query) throws ServerFault {

		if (!context.isAdmin()) {
			throw new ServerFault("ScheduledJobService.searchExecution is only available to admin0 or domain admin",
					ErrorCode.PERMISSION_DENIED);
		}

		ListResult<JobExecution> ret = new ListResult<JobExecution>();
		if (query.active) {

			Map<String, RunIdImpl> slots = Scheduler.get().getActiveSlots();
			ArrayList<JobExecution> items = new ArrayList<JobExecution>(slots.size());

			for (String key : slots.keySet()) {
				RunIdImpl rid = slots.get(key);
				if (query.domain != null && !query.domain.equals(rid.domainName)) {
					logger.debug("active slot not from my domain {} vs. {}", query.domain, rid.domainName);
					continue;
				}
				JobExecution je = new JobExecution();
				je.domainName = rid.domainName;
				je.jobId = rid.jid;
				je.startDate = new Date(rid.startTime);
				je.status = JobExitStatus.IN_PROGRESS;
				items.add(je);
			}

			ret.values = items;
		} else {
			ret = store.findExecutions(query);
		}

		return ret;

	}

	@Override
	public Job getJobFromId(String jobId) throws ServerFault {
		if (!context.isAdmin()) {
			throw new ServerFault("ScheduledJobService.getJobFromId is only available to admin0 or domain admin",
					ErrorCode.PERMISSION_DENIED);
		}

		logger.debug("Get job from id {}", jobId);

		IScheduledJob job = JobRegistry.getScheduledJob(jobId);
		if (job == null) {
			throw new ServerFault("Job with id " + jobId + " not found.");
		}
		if (!context.isDomainGlobal() && job.getType() == JobKind.GLOBAL) {
			throw new ServerFault("Job " + jobId + " visibility is limited to global admins", ErrorCode.FORBIDDEN);
		}

		Job ret = convertJob(job);
		if (ret == null) {
			logger.error("No job found for jobId {}", jobId);
			return null;
		}

		store.loadStatusesAndPlans(context, new JobQuery(), Arrays.asList(ret));

		return ret;
	}

	@Override
	public void update(Job job) throws ServerFault {
		if (!context.isAdmin()) {
			throw new ServerFault("ScheduledJobService.update is only available to admin0 or domain admin",
					ErrorCode.PERMISSION_DENIED);
		}

		logger.debug("Update job {}", job.id);
		sanitize(job);
		store.updateJob(job);
	}

	@Override
	public void deleteExecution(int jobExecutionId) throws ServerFault {
		if (!context.isAdmin()) {
			throw new ServerFault("ScheduledJobService.deleteExecution is only available to admin0 or domain admin",
					ErrorCode.PERMISSION_DENIED);
		}

		logger.debug("delete job {}", jobExecutionId);
		store.delete(Arrays.asList(jobExecutionId));
	}

	@Override
	public void deleteExecutions(List<Integer> jobExecutionId) throws ServerFault {
		if (!context.isAdmin()) {
			throw new ServerFault("ScheduledJobService.deleteExecutions is only available to admin0 or domain admin",
					ErrorCode.PERMISSION_DENIED);
		}

		store.delete(jobExecutionId);
	}

	@Override
	public void start(String jobId, String domainName) throws ServerFault {
		if (jobId == null || jobId.trim().isEmpty()) {
			logger.error("No job ID specified !");
			throw new ServerFault("No job ID specified !", ErrorCode.UNKNOWN);
		}

		if (domainName == null || domainName.trim().isEmpty()) {
			domainName = context.getContainerUid();
		}

		logger.info("Start job {}, domain {}", jobId, domainName);

		if (!context.isDomainAdmin(domainName)) {
			throw new ServerFault("ScheduledJobService.start is only available to admin0 or domain admin",
					ErrorCode.PERMISSION_DENIED);
		}

		JobRegistry.runNow(context, jobId, domainName);

	}

	@Override
	public Set<LogEntry> getLogs(JobExecution jobExecution, int offset) throws ServerFault {
		if (!context.isDomainAdmin(jobExecution.domainName)) {
			throw new ServerFault("ScheduledJobService.getLogs is only available to admin0 or domain admin",
					ErrorCode.PERMISSION_DENIED);
		}

		logger.debug("Get logs for job {}, offset {}", jobExecution.id, offset);

		Set<LogEntry> ret = new LinkedHashSet<LogEntry>();

		Set<LogEntry> entries = null;
		if (jobExecution.id > 0) {
			// Stored logs
			entries = store.fetchLogEntries(context, jobExecution.id);
		} else {
			RunIdImpl slot = (RunIdImpl) Scheduler.get().getActiveSlot(jobExecution.domainName, jobExecution.jobId);
			// Live logs
			entries = ImmutableSet.copyOf(slot.entries);
		}
		Iterator<LogEntry> it = entries.iterator();
		int i = 0;
		while (it.hasNext()) {
			if (i > offset) {
				LogEntry entry = it.next();
				entry.offset = i;
				ret.add(entry);
			}
			i++;
		}
		return ret;
	}

	@Override
	public JobExecution createExecution(JobExecution je) throws ServerFault {
		if (!context.isDomainAdmin(je.domainName)) {
			throw new ServerFault("ScheduledJobService.createExecution is only available to admin0 or domain admin",
					ErrorCode.PERMISSION_DENIED);
		}

		store.ensureDefaultPlan(je.domainName, je.jobId);
		return store.createExecution(je);
	}

	@Override
	public void updateExecution(JobExecution je) throws ServerFault {
		if (!context.isDomainAdmin(je.domainName)) {
			throw new ServerFault("ScheduledJobService.updateExecution is only available to admin0 or domain admin",
					ErrorCode.PERMISSION_DENIED);
		}
		store.updateExecution(je);
	}

	@Override
	public void storeLogEntries(int jobExecutionid, Set<LogEntry> entries) throws ServerFault {
		if (!context.isAdmin()) {
			throw new ServerFault("ScheduledJobService.storeLogEntries is only available to admin0 or domain admin",
					ErrorCode.PERMISSION_DENIED);
		}

		store.storeLogEntries(jobExecutionid, entries);
	}

	private void sanitize(Job job) throws ServerFault {
		List<JobPlanification> plans = job.domainPlanification;
		if (!context.isDomainGlobal()) {
			JobPlanification domainPlan = null;
			for (JobPlanification jp : plans) {
				if (jp.domain.equals(context.getContainerUid())) {
					domainPlan = jp;
					continue;
				}
			}

			if (domainPlan == null) {
				throw new ServerFault("Update triggered from domain without a plan set.");
			}
			plans.clear();
			plans.add(domainPlan);
		}

		for (JobPlanification jp : plans) {
			if (jp.kind == PlanKind.SCHEDULED) {
				if (jp.rec == null) {
					throw new ServerFault("rec must not be null on a scheduled plan");
				}
			}
		}

		String recipients = job.recipients;
		if (recipients != null && !recipients.isEmpty()) {
			String list[] = recipients.split(" ");
			for (String email : list) {
				if (email == null || !Regex.EMAIL.validate(email)) {
					logger.error("Email " + email + " is invalid");
					throw new ServerFault("Invalid email: " + email, ErrorCode.INVALID_EMAIL);
				}
			}
		}
	}

	private Job convertJob(IScheduledJob bj) {
		Job j = new Job();
		j.id = bj.getJobId();
		j.description = bj.getDescription("en");
		j.kind = bj.getType();
		return j;
	}

}
