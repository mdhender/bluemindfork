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
package net.bluemind.scheduledjob.scheduler;

import net.bluemind.core.task.service.AbstractTaskMonitor;

public class SchedulerProgressMonitor extends AbstractTaskMonitor {
	private IScheduler sch;
	private double totalWork = 0;
	private double doneWork = 0;
	private IScheduledJobRunId runId;

	public SchedulerProgressMonitor(IScheduler sch, IScheduledJobRunId runId) {
		super(0);
		this.sch = sch;
		this.runId = runId;
	}

	@Override
	public void begin(double totalWork, String log) {
		this.totalWork = totalWork;
	}

	@Override
	public void progress(double pr, String log) {
		this.doneWork += pr;
		sch.reportProgress(runId, (int) Math.round((doneWork / totalWork) * 100.0));
	}

	@Override
	public void end(boolean success, String log, String result) {
		// do nothing. Parent task monitor handle end
	}

	@Override
	public void log(String log) {
		sch.info(runId, "en", log);
	}
}
