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
package net.bluemind.filehosting.filesystem.job;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.filehosting.filesystem.service.internal.FileSystemFileHostingService;
import net.bluemind.filehosting.service.export.IFileHostingService;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;
import net.bluemind.system.api.GlobalSettingsKeys;

public class FileHostingCleanUpJob implements IScheduledJob {

	private Logger logger = LoggerFactory.getLogger(FileHostingCleanUpJob.class);

	@Override
	public void tick(IScheduler sched, boolean plannedExecution, String domainName, Date startDate) throws ServerFault {
		if (!plannedExecution) {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(startDate);
			if (gc.get(Calendar.MINUTE) != 0 || gc.get(Calendar.HOUR_OF_DAY) != 2) {
				logger.debug("automatic mode, not running at {}", gc.getTime().toString());
				return;
			}
		}

		IScheduledJobRunId rid = null;

		try {
			rid = sched.requestSlot(domainName, this, startDate);
			final IScheduledJobRunId ridRef = rid;

			Optional<IFileHostingService> service = getFileHostingService();
			if (!service.isPresent()) {
				logger.info("No FileHosting implementation found...Exiting");
				sched.finish(rid, JobExitStatus.COMPLETED_WITH_WARNINGS);
				return;
			}

			final AtomicReference<Integer> count = new AtomicReference<>(0);
			IDomains domainService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IDomains.class);
			domainService.all().stream() //
					.forEach(domain -> {
						if (!"global.virt".equals(domain.uid)) {
							try {
								Map<String, String> values = getDomainSettings(domain.uid);
								int retentionTime = intValue(values, GlobalSettingsKeys.filehosting_retention.name(),
										365);
								int domainCount = ((FileSystemFileHostingService) service.get()).cleanup(retentionTime,
										domain.uid);
								count.set(count.get() + domainCount);
							} catch (Exception e) {
								logger.error(e.getMessage(), e);
								sched.warn(ridRef, "en", "Cannot cleanup filehosting file of domain " + domain.uid
										+ " : " + e.getMessage());
								sched.warn(ridRef, "fr",
										"Erreurs pendant le cleanup du domaine " + domain.uid + " : " + e.getMessage());
							}
						}
					});

			sched.info(rid, "en", "" + count.get() + " files have been deleted.");
			sched.info(rid, "fr", "" + count.get() + " fichiers supprimés.");

			sched.finish(rid, JobExitStatus.SUCCESS);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			if (rid != null) {
				sched.finish(rid, JobExitStatus.FAILURE);
			}
		}

	}

	public Integer intValue(Map<String, String> values, String prop, int defaultValue) {
		String value = values.get(prop);
		if (value == null) {
			return defaultValue;
		} else {
			return Integer.valueOf(value);
		}
	}

	private Map<String, String> getDomainSettings(String domainUid) throws ServerFault {
		IDomainSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		return settings.get();
	}

	private Optional<IFileHostingService> getFileHostingService() {
		RunnableExtensionLoader<IFileHostingService> epLoader = new RunnableExtensionLoader<>();
		List<IFileHostingService> services = epLoader.loadExtensions("net.bluemind.filehosting", "service", "service",
				"api");
		return services.stream().filter(FileSystemFileHostingService.class::isInstance).findAny();
	}

	@Override
	public JobKind getType() {
		return JobKind.GLOBAL;
	}

	@Override
	public String getDescription(String locale) {
		return "Cleans up old shared files";
	}

	@Override
	public String getJobId() {
		return "net.bluemind.filehosting.filesystem.job.FileHostingCleanUpJob";
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
