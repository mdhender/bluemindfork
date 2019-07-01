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
package net.bluemind.calendar.job;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.internal.IInternalContainerSync;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerSyncResult;
import net.bluemind.core.container.persistance.ContainersSyncStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class ExternalCalendarSyncJob implements IScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(ExternalCalendarSyncJob.class);
	private static final int MAX = 20;

	@Override
	public void tick(IScheduler sched, boolean forced, String domainName, Date startDate) throws ServerFault {
		IScheduledJobRunId rid = sched.requestSlot(domainName, this, startDate);

		if (rid != null) {
			try {
				boolean res = run(sched, rid);
				if (res) {
					sched.finish(rid, JobExitStatus.SUCCESS);
				} else {
					sched.finish(rid, JobExitStatus.COMPLETED_WITH_WARNINGS);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				sched.error(rid, "en", e.getMessage() != null ? e.getMessage() : "Failure");
				sched.error(rid, "fr", e.getMessage() != null ? e.getMessage() : "Echec");
				sched.finish(rid, JobExitStatus.FAILURE);
			}
		}

	}

	private boolean run(IScheduler sched, IScheduledJobRunId rid) {
		boolean ret = true;
		long begin = System.currentTimeMillis();
		Calendar from = Calendar.getInstance();
		from.setTimeZone(TimeZone.getTimeZone("UTC"));
		from.set(Calendar.SECOND, 0);
		from.set(Calendar.MILLISECOND, 0);

		BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

		List<String> uids = context.getAllMailboxDataSource().stream().flatMap(ds -> {
			ContainersSyncStore store = new ContainersSyncStore(ds);
			return JdbcAbstractStore
					.doOrFail(() -> store.list(ICalendarUids.TYPE, from.getTime().getTime(), MAX, "icsUrl")).stream();
		}).collect(Collectors.toList());

		if (uids.size() > 0) {
			double total = 100d / uids.size();
			int i = 0;
			for (String uid : uids) {
				long start = System.currentTimeMillis();
				IContainers contApi = context.provider().instance(IContainers.class);
				ContainerDescriptor cont = contApi.getIfPresent(uid);
				if (cont == null) {
					sched.warn(rid, "en", uid + " not found, skipped.");
					sched.warn(rid, "fr", uid + " non trouvé, ignoré.");
					continue;
				}
				IDirectory dirApi = context.provider().instance(IDirectory.class, cont.domainUid);
				DirEntry owner = dirApi.findByEntryUid(cont.owner);
				try {
					IInternalContainerSync service = context.provider().instance(IInternalContainerSync.class, uid);
					ContainerSyncResult res = service.sync();
					if (res != null) {
						long end = System.currentTimeMillis() - start;
						sched.info(rid, "en",
								"Sync calendar " + uid + " owned by " + owner.email + " in " + end + "ms. created: "
										+ res.added + ", updated: " + res.updated + ", removed: " + res.removed);
						sched.info(rid, "fr",
								"Synchronisation du calendrier " + uid + " de " + owner.email + " en " + end
										+ "ms. crées: " + res.added + ", modifiés: " + res.updated + ", supprimés: "
										+ res.removed);
					} else {
						sched.warn(rid, "en", "Fail to sync calendar " + uid + " owned by " + owner.email);
						sched.warn(rid, "fr",
								"Erreur lors de la synchronisation du calendrier " + uid + " de " + owner.email);
						ret = false;
					}
				} catch (ServerFault sf) {
					logger.error("Fail to sync calendar {} owned by {}", uid, owner, sf);
					sched.warn(rid, "en", "Fail to sync calendar " + uid + " owned by " + owner.email);
					sched.warn(rid, "fr",
							"Erreur lors de la synchronisation du calendrier " + uid + " de " + owner.email);
					ret = false;
				}
				i++;
				sched.reportProgress(rid, (int) (i * total));
			}
		}

		long end = System.currentTimeMillis() - begin;
		sched.info(rid, "en", "Calendars sync in " + end + "ms");
		sched.info(rid, "fr", "Calendriers synchronisés en " + end + "ms");

		return ret;
	}

	@Override
	public JobKind getType() {
		return JobKind.GLOBAL;
	}

	@Override
	public String getDescription(String locale) {
		if ("fr".equals(locale)) {
			return "Synchronisation des calendriers externes";
		}
		return "External calendars sync";
	}

	@Override
	public String getJobId() {
		return getClass().getName();
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
