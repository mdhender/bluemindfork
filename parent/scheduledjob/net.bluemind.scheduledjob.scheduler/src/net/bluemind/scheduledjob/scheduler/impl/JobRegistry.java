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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;

public class JobRegistry {

	private static Map<String, IScheduledJob> jobs;
	private static final Logger logger = LoggerFactory.getLogger(JobRegistry.class);

	public JobRegistry() {

		jobs = new ConcurrentHashMap<String, IScheduledJob>();

		RunnableExtensionLoader<IScheduledJob> rel = new RunnableExtensionLoader<IScheduledJob>();
		List<IScheduledJob> provided = rel.loadExtensions("net.bluemind.scheduledjob.scheduler", "job",
				"scheduledjob_provider", "implementation");

		for (IScheduledJob bj : provided) {
			logger.info("load job: {}", bj.getJobId());
			jobs.put(bj.getJobId(), bj);
		}
	}

	public static Collection<IScheduledJob> getBluejobs() {
		Collection<IScheduledJob> col = jobs.values();
		Collection<IScheduledJob> ret = new ArrayList<IScheduledJob>(col.size());
		ret.addAll(col);
		return ret;
	}

	public static IScheduledJob getScheduledJob(String jid) {
		return jobs.get(jid);
	}

	public static void runNow(SecurityContext context, String jobId, String domainName) throws ServerFault {
		IScheduledJob bj = jobs.get(jobId);
		if (bj != null) {
			if (bj.getType() == JobKind.GLOBAL && !context.isDomainGlobal()) {
				throw new ServerFault("Only runnable by global admin", ErrorCode.FORBIDDEN);
			}
			logger.info("[{}] triggered run of job {}", context.getSubject(), jobId);
			JobRunner runner = new JobRunner(bj, true, domainName);
			runner.run();
		} else {
			logger.warn("User triggered run of non-existent jobId " + jobId);
			throw new ServerFault("Missing job");
		}
	}
}
