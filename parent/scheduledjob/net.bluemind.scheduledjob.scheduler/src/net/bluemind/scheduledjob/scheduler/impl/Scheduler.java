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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.scheduledjob.api.InProgressException;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.LogEntry;
import net.bluemind.scheduledjob.api.LogLevel;
import net.bluemind.scheduledjob.scheduler.IRecordingListener;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class Scheduler implements IScheduler, IRecordingListener {

	private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

	private static final Scheduler sched = new Scheduler();
	private Map<String, RunIdImpl> activeSlots;
	private static Map<String, Set<String>> lockedResources = new ConcurrentHashMap<>();
	private ExecutorService pool;
	private ThreadLocal<String> activeGroup;
	private ConcurrentHashMap<String, ExecutionRecorder> activeRecorders;
	private Executor exec;

	private Scheduler() {
		exec = Executors.newFixedThreadPool(4);

		activeGroup = new ThreadLocal<>();
		activeSlots = new ConcurrentHashMap<>();
		activeRecorders = new ConcurrentHashMap<>();
		pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	@Override
	public IScheduledJobRunId requestSlot(String domainName, IScheduledJob bj, Date startDate) throws ServerFault {
		String runId = domainName + "-" + bj.getJobId();
		activeSlotCheck(runId);
		RunIdImpl rid = new RunIdImpl(activeGroup.get(), domainName, bj.getJobId(), startDate);
		activeSlots.put(runId, rid);

		ExecutionRecorder recorder = new ExecutionRecorder(rid, this);
		exec.execute(recorder);
		activeRecorders.put(runId, recorder);
		if (!bj.getLockedResources().isEmpty()) {
			logger.info("Job {} blocks {} resources", bj.getJobId(), bj.getLockedResources().size());
			lockedResources.put(runId, bj.getLockedResources());
		}
		return rid;
	}

	private void activeSlotCheck(String runId) throws InProgressException {
		logger.debug("*** checking if " + runId + " is active");
		boolean ret = activeSlots.containsKey(runId);
		if (ret) {
			logger.warn(runId + " was already in progress.");
			throw new InProgressException();
		} else if (logger.isDebugEnabled()) {
			logger.debug("      * '" + runId + "' was not in progress.");
		}
	}

	@Override
	public void info(IScheduledJobRunId rid, String locale, String logEntry) {
		// we log as debug here as we don't wan't un-important (& visible in the
		// ui) stuff in our system logs
		logger.debug("[" + rid + "] [" + locale + "] => " + logEntry);

		log(rid, LogLevel.INFO, locale, logEntry);
	}

	@Override
	public void warn(IScheduledJobRunId rid, String locale, String logEntry) {
		logger.warn("[" + rid + "] [" + locale + "] => " + logEntry);
		log(rid, LogLevel.WARNING, locale, logEntry);
	}

	private void log(IScheduledJobRunId rid, LogLevel severity, String locale, String logEntry) {

		RunIdImpl rrid = (RunIdImpl) rid;
		LogEntry le = new LogEntry();
		le.timestamp = System.currentTimeMillis();
		if (locale != null) {
			le.locale = locale;
		}
		le.severity = severity;
		le.content = logEntry != null ? logEntry : "";
		rrid.addEntry(le);
		String key = rrid.domainName + "-" + rrid.jid;
		activeRecorders.get(key).logEntries.offer(le);
	}

	@Override
	public void error(IScheduledJobRunId rid, String locale, String logEntry) {
		logger.error("[" + rid + "] [" + locale + "] => " + logEntry);
		log(rid, LogLevel.ERROR, locale, logEntry);
	}

	@Override
	public void reportProgress(IScheduledJobRunId rid, int percent) {
		logger.debug("[" + rid + "] progress is now " + percent + "%.");
		log(rid, LogLevel.PROGRESS, null, "#progress " + percent);
	}

	@Override
	public synchronized void finish(IScheduledJobRunId irid, JobExitStatus status) {
		RunIdImpl rid = (RunIdImpl) irid;
		logger.debug("Finishing " + rid.toString());
		if (rid.endTime != rid.startTime) {
			if (logger.isDebugEnabled()) {
				logger.debug("Already finished job " + rid.jid, new Throwable());
			}
			return;
		}
		if (status == JobExitStatus.FAILURE) {
			logger.error("finished with FAILURE status called from here", new Throwable("sched.finish(FAILURE)"));
		}
		long endStamp = System.currentTimeMillis();
		reportProgress(rid, 100);
		rid.status = status;
		rid.endTime = endStamp;

		String key = rid.domainName + "-" + rid.jid;
		activeRecorders.get(key).finish();

		SendReport sr = new SendReport(rid);
		exec.execute(sr);
	}

	@Override
	public void recordingComplete(RunIdImpl rid) {
		String k = rid.domainName + "-" + rid.jid;
		long rt = rid.endTime - rid.startTime;
		logger.info("[" + rid + "] finished and recorded: " + rid.status + ", duration: " + rt + "ms.");
		lockedResources.remove(k);

		// help the live logs viewer see logs
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
		}
		activeSlots.remove(k);
		rid.destroy();
	}

	public static Scheduler get() {
		return sched;
	}

	public IScheduledJobRunId getActiveSlot(String domainName, String jid) {
		return activeSlots.get(domainName + "-" + jid);
	}

	public void tryRun(JobTicker runner) {
		pool.execute(runner);
	}

	public void setActiveGroup(String execGroup) {
		activeGroup.set(execGroup);
	}

	/**
	 * Returns a copy of the running jobs list
	 * 
	 * @return
	 */
	public Map<String, RunIdImpl> getActiveSlots() {
		HashMap<String, RunIdImpl> copy = new HashMap<String, RunIdImpl>();
		copy.putAll(activeSlots);
		return copy;
	}

	public Set<String> checkLockedResources(String domain, IScheduledJob bj) {
		logger.debug("Checking for locked resources needed by job {}", bj.getJobId());
		if (bj.getLockedResources().isEmpty()) {
			return Collections.emptySet();
		}

		final Set<String> resources = new HashSet<>();
		for (String resource : bj.getLockedResources()) {
			if (resourceIsLocked(resource)) {
				logger.debug("Job {} needs resource {} which is currently locked", bj.getJobId(), resource);
				resources.add(resource);
			}
		}

		return resources;
	}

	private boolean resourceIsLocked(String resource) {
		final AtomicBoolean locked = new AtomicBoolean();
		lockedResources.values().forEach(set -> {
			if (set.contains(resource)) {
				locked.set(true);
			}
		});
		return locked.get();
	}

}
