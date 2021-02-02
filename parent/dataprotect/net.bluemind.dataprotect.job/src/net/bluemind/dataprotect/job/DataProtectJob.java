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
package net.bluemind.dataprotect.job;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.GenerationStatus;
import net.bluemind.dataprotect.api.IDataProtect;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class DataProtectJob implements IScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(DataProtectJob.class);
	private TaskRef ref;

	public DataProtectJob() {
	}

	@Override
	public String getDescription(String locale) {
		return "en".equals(locale) ? "Backup" : "Sauvegarde";
	}

	@Override
	public String getJobId() {
		return "DataProtect";
	}

	@Override
	public void tick(IScheduler sched, boolean forced, String domainName, Date startDate) throws ServerFault {
		if (!forced) {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(startDate);
			if (gc.get(Calendar.MINUTE) != 0 || gc.get(Calendar.HOUR_OF_DAY) != 1) {
				logger.info("automatic mode, not running at {}", gc.getTime());
				return;
			}
		}

		IScheduledJobRunId slot = sched.requestSlot("global.virt", this, startDate);
		if (slot != null) {
			logger.info("Starting backup...");
			sched.info(slot, "en", "Starting backup");
			sched.info(slot, "fr", "Démarrage de la sauvegarde");

			IServiceProvider sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			IDataProtect dpApi = sp.instance(IDataProtect.class);
			AtomicReference<JobExitStatus> status = new AtomicReference<>(JobExitStatus.SUCCESS);
			try {
				ref = dpApi.saveAll();
				ITask taskApi = sp.instance(ITask.class, String.format("%s", ref.id));
				Stream logsStream = taskApi.log();

				VertxStream.<Buffer>read(logsStream).handler(log -> displayLogs(sched, slot, log));

				TaskStatus taskResult;
				try {
					taskResult = TaskUtils.waitForInterruptible(sp, ref);
				} catch (InterruptedException e) {
					status.set(JobExitStatus.INTERRUPTED);
					return;
				}
				if (!taskResult.state.succeed) {
					status.set(JobExitStatus.FAILURE);
				} else {
					dpApi.getAvailableGenerations().stream().reduce(new DataProtectGeneration(), (a, b) -> {
						if (a.id > b.id) {
							return a;
						}
						return b;
					}).parts.forEach(part -> {
						if (part.valid == GenerationStatus.INVALID) {
							status.set(JobExitStatus.FAILURE);
						} else {
							if (part.valid == GenerationStatus.UNKNOWN && status.get() == JobExitStatus.SUCCESS) {
								status.set(JobExitStatus.COMPLETED_WITH_WARNINGS);
							}
						}
					});
				}

			} finally {
				sched.finish(slot, status.get());
			}

		}

	}

	private void displayLogs(IScheduler sched, IScheduledJobRunId slot, Buffer logBuffer) {
		JsonObject log = new JsonObject(logBuffer.toString());
		String message = log.getString("message");

		if (message == null || message.equals("")) {
			return;
		}

		logger.info("{} {} {} {}", log.getInteger("done"), log.getInteger("total"), message, log.getBoolean("end"));

		String formatedMessage = String.format("%d/%d: %s", log.getInteger("done").intValue() + 1,
				log.getInteger("total").intValue() + 1, message);
		sched.info(slot, "en", formatedMessage);
		sched.info(slot, "fr", formatedMessage);
	}

	@Override
	public void cancel() {
		if (ref != null) {
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITasksManager.class).cancel(ref.id);
		}
	}

	@Override
	public JobKind getType() {
		return JobKind.GLOBAL;
	}

	@Override
	public Set<String> getLockedResources() {
		Set<String> resources = new HashSet<>();
		resources.add("mails");
		return resources;
	}

	@Override
	public boolean supportsScheduling() {
		return true;
	}

}
