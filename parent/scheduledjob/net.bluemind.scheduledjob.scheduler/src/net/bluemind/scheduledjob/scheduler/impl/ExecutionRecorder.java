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

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.metrics.annotations.TimeRangeAnnotation;
import net.bluemind.scheduledjob.api.IInCoreJob;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.LogEntry;
import net.bluemind.scheduledjob.scheduler.IRecordingListener;

public class ExecutionRecorder implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ExecutionRecorder.class);

	private RunIdImpl rid;
	private IRecordingListener rl;
	private IInCoreJob service;
	private AtomicBoolean finished;
	public BlockingQueue<LogEntry> logEntries;

	public ExecutionRecorder(RunIdImpl rid, IRecordingListener rl) {
		this.rid = rid;
		this.rl = rl;
		try {
			service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInCoreJob.class);
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
		this.finished = new AtomicBoolean(false);
		logEntries = new LinkedBlockingQueue<>();
	}

	@Override
	public void run() {
		logger.info("recording execution of {}", rid);

		JobExecution je = createExecution();
		JobExecution exec = storeExecution(je);

		if (null == exec) {
			logger.warn("Could not create execution of job {}", rid);
			return;
		}

		while (!finished.get() || !logEntries.isEmpty()) {
			try {
				LogEntry entry = logEntries.poll(1, TimeUnit.SECONDS);
				if (null != entry) {
					storeLogEntry(exec.id, entry);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

		je.status = rid.status;
		je.endDate = new Date(rid.endTime);
		updateExecution(je);
		rl.recordingComplete(rid);
		long durationMs = je.endDate.getTime() - je.startDate.getTime();
		if (TimeUnit.MILLISECONDS.toSeconds(durationMs) > 30) {
			TimeRangeAnnotation.annotate(je.jobId, je.startDate, Optional.of(je.endDate),
					ImmutableMap.of("kind", "job", "product", "bm-core", "jobId", je.jobId));
		}

	}

	private JobExecution storeExecution(JobExecution je) {
		JobExecution exec = null;
		try {
			exec = service.createExecution(je);
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
		return exec;
	}

	private void updateExecution(JobExecution je) {
		try {
			service.updateExecution(je);
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
	}

	private JobExecution createExecution() {
		JobExecution je = new JobExecution();
		je.domainUid = rid.domainUid;
		je.jobId = rid.jid;
		je.startDate = new Date(rid.startTime);
		je.endDate = new Date(rid.endTime);
		je.execGroup = rid.groupId;
		je.status = rid.status;
		return je;
	}

	private void storeLogEntry(int id, LogEntry entry) throws ServerFault {
		Set<LogEntry> set = new HashSet<>();
		set.add(entry);
		service.storeLogEntries(id, set);
	}

	public void finish() {
		finished.set(true);
	}
}
