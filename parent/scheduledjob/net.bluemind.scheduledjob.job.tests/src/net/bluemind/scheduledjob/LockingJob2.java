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
package net.bluemind.scheduledjob;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class LockingJob2 implements IScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(LockingJob1.class);

	@Override
	public void tick(IScheduler sched, boolean plannedExecution, String domainName, Date startDate) throws ServerFault {
		IScheduledJobRunId rid = sched.requestSlot(domainName, this, startDate);

		logger.info("LockingJob 2 starting up");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info("LockingJob 2 finished");
		sched.finish(rid, JobExitStatus.SUCCESS);
	}

	@Override
	public JobKind getType() {
		return JobKind.GLOBAL;
	}

	@Override
	public String getDescription(String locale) {
		return "Dummyjob1 description";
	}

	@Override
	public String getJobId() {
		return "LockingJob2";
	}

	@Override
	public Set<String> getLockedResources() {
		Set<String> res = new HashSet<>();
		res.add("resource2");
		return res;
	}

	@Override
	public boolean supportsScheduling() {
		return true;
	}

}
