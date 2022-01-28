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

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.scheduledjob.api.IJob;
import net.bluemind.scheduledjob.api.Job;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.api.JobPlanification;
import net.bluemind.scheduledjob.api.PlanKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;

public class JobRunner {

	private static final Logger logger = LoggerFactory.getLogger(JobRunner.class);

	private IScheduledJob bjp;
	private boolean forced;

	private String restrictToDomain;

	public JobRunner(IScheduledJob bjp, boolean forced, String domainName) {
		this.bjp = bjp;
		this.forced = forced;
		this.restrictToDomain = domainName;
	}

	public void run() {
		Collection<String> domains = getRelevantDomains();
		if (domains == null) {
			logger.error("Domains list is empty !");
			return;
		}

		String gid = UUID.randomUUID().toString();

		Job job = null;
		try {
			job = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IJob.class)
					.getJobFromId(bjp.getJobId());
		} catch (ServerFault e) {
			// showing this one to the user would be in a 'server logs' story
			logger.error("aborted: " + e.getMessage(), e);
		}

		for (String domain : domains) {
			logger.debug("tick for {} on domain {}", bjp.getJobId(), domain);
			JobPlanification plan = getJobPlan(job, domain);

			if (forced) {
				tick(domain, gid, true);
			} else if (plan == null || plan.kind == PlanKind.OPPORTUNISTIC) {
				tick(domain, gid, false);
			} else if (plan.kind == PlanKind.SCHEDULED) {
				Date nextRun = plan.nextRun;

				if (nextRun == null) {
					logger.error("Next run date is null, your scheduling might be invalid for job {}", bjp.getJobId());
				} else if (nextRun.compareTo(new Date()) <= 0) {
					logger.info("{}@{}: scheduling execution.", job.id, domain);
					tick(domain, gid, true);
				}
			} else if (plan.kind == PlanKind.DISABLED) {
				logger.info("{} is disabled for {}", job.id, domain);
			}

		}

		logger.debug("ticked {} for {} domains", bjp.getJobId(), domains.size());
	}

	/**
	 * @param domainName
	 * @param groupId
	 * @param forced
	 */
	private void tick(String domainName, String groupId, boolean forced) {
		JobTicker jt = new JobTicker(bjp, forced, domainName, new Date(), groupId);
		Scheduler.get().tryRun(jt);
	}

	/**
	 * @param job
	 * @param domain
	 * @return
	 */
	private JobPlanification getJobPlan(Job job, String domain) {
		if (job == null) {
			return null;
		}

		JobPlanification plan = null;
		for (JobPlanification jp : job.domainPlanification) {
			if (domain.equals(jp.domain)) {
				plan = jp;
			}
		}
		return plan;
	}

	/**
	 * @return
	 */
	private Collection<String> getRelevantDomains() {
		Set<String> ret = new HashSet<String>();

		if (bjp.getType() == JobKind.GLOBAL) {
			ret.add("global.virt");

		} else if (bjp.getType() == JobKind.MULTIDOMAIN) {
			try {
				IDomains service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IDomains.class);

				List<ItemValue<Domain>> domains = service.all();

				for (ItemValue<Domain> domain : domains) {
					if (restrictToDomain != null && !restrictToDomain.equals(domain.value.name)) {
						logger.warn(" **** Skipping execution on d: {}, restrict: {}", domain.value.name,
								restrictToDomain);
						continue;
					}

					ret.add(domain.value.name);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				ret = null;
			}

		}
		return ret;
	}
}
