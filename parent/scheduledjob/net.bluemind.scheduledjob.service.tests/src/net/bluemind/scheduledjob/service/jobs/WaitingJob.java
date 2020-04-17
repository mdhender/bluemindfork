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
package net.bluemind.scheduledjob.service.jobs;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class WaitingJob implements IScheduledJob {

	public static volatile int cancelled = 0;

	@Override
	public void tick(IScheduler sched, boolean plannedExecution, String domainName, Date startDate) throws ServerFault {
		IScheduledJobRunId rid = sched.requestSlot(domainName, this, startDate);

		sched.info(rid, "en", "WaitingJob");
		try {
			sleep();
		} catch (InterruptedException e) {
			throw new ServerFault("i dont like interruptions");
		}

		sched.finish(rid, JobExitStatus.SUCCESS);
	}

	private void sleep() throws InterruptedException {
		Thread.sleep(10000);
	}

	@Override
	public JobKind getType() {
		return JobKind.MULTIDOMAIN;
	}

	@Override
	public String getDescription(String locale) {
		return "DomainJob description";
	}

	@Override
	public String getJobId() {
		return "WaitingJob";
	}

	@Override
	public Set<String> getLockedResources() {
		return Collections.emptySet();
	}

	@Override
	public boolean supportsScheduling() {
		return true;
	}

	@Override
	public void cancel() {
		cancelled += 1;
	}

}
