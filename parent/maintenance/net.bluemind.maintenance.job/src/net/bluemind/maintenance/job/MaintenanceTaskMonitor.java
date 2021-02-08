/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.maintenance.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class MaintenanceTaskMonitor implements IServerTaskMonitor {
	private static final Logger logger = LoggerFactory.getLogger(MaintenanceTaskMonitor.class);

	private final String logPrefix;
	private final TaskMonitorToSched logAdapter;

	public static class TaskMonitorToSched {
		private final IScheduler sched;
		private final IScheduledJobRunId slot;

		public TaskMonitorToSched(IScheduler sched, IScheduledJobRunId slot) {
			this.sched = sched;
			this.slot = slot;
		}

		public void log(String message) {
			logger.info(message);
			sched.info(slot, "en", message);
			sched.info(slot, "fr", message);
		}
	}

	public MaintenanceTaskMonitor(IScheduler sched, IScheduledJobRunId slot) {
		this(sched, slot, "");
	}

	public MaintenanceTaskMonitor(IScheduler sched, IScheduledJobRunId slot, String logPrefix) {
		this.logAdapter = new TaskMonitorToSched(sched, slot);
		this.logPrefix = logPrefix;
	}

	public MaintenanceTaskMonitor(TaskMonitorToSched adapter, String logPrefix) {
		this.logAdapter = adapter;
		this.logPrefix = logPrefix;
	}

	@Override
	public IServerTaskMonitor subWork(double work) {
		return this;
	}

	@Override
	public IServerTaskMonitor subWork(String logPrefix, double work) {
		return new MaintenanceTaskMonitor(this.logAdapter,
				this.logPrefix.isEmpty() ? logPrefix : this.logPrefix + " - " + logPrefix);
	}

	@Override
	public void begin(double totalWork, String log) {
		this.log(log);
	}

	@Override
	public void progress(double doneWork, String log) {
		this.log(log);
	}

	public void progress(int total, int current) {
		// empty
	}

	@Override
	public void end(boolean success, String log, String result) {
		if (log != null && !log.isEmpty()) {
			if (result != null && !result.isEmpty()) {
				this.log(log + " : " + result);
			} else {
				this.log(log);
			}
		} else {
			if (result != null && !result.isEmpty()) {
				this.log(result);
			}
		}
	}

	@Override
	public void log(String log) {
		if (log != null && !log.isEmpty()) {
			this.logAdapter.log(this.logPrefix != null ? this.logPrefix + ": " + log : log);
		}
	}
}
