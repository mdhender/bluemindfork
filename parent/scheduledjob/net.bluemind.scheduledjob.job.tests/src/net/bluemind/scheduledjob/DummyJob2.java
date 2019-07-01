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

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class DummyJob2 implements IScheduledJob {

	@Override
	public void tick(IScheduler sched, boolean plannedExecution, String domainName, Date startDate) throws ServerFault {
		IScheduledJobRunId rid = sched.requestSlot(domainName, this, startDate);

		sched.finish(rid, JobExitStatus.SUCCESS);
	}

	@Override
	public JobKind getType() {
		return JobKind.GLOBAL;
	}

	@Override
	public String getDescription(String locale) {
		return "Dummyjob2 description";
	}

	@Override
	public String getJobId() {
		return "DummyJob2";
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
