/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.addressbook.ldap.job;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.internal.IInternalContainerSync;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerSyncResult;
import net.bluemind.core.container.model.ContainerSyncStatus.Status;
import net.bluemind.core.container.persistence.ContainersSyncStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class LdapAddressBookSyncJob implements IScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(LdapAddressBookSyncJob.class);
	private static final int MAX = 50;

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

		IContainers containerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainers.class);

		BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

		List<String> uids = context.getAllMailboxDataSource().stream().flatMap(dataSource -> {
			ContainersSyncStore store = new ContainersSyncStore(dataSource);
			return ContainersSyncStore
					.doOrFail(() -> store.list(IAddressBookUids.TYPE, from.getTime().getTime(), MAX, "baseDn"))
					.stream();
		}).collect(Collectors.toList());

		if (uids.size() > 0) {
			double total = 100d / uids.size();
			int i = 0;
			for (String uid : uids) {
				ContainerDescriptor containerDescriptor = containerService.get(uid);
				String name = containerDescriptor.domainUid + ":" + containerDescriptor.name;
				long start = System.currentTimeMillis();
				try {
					IInternalContainerSync service = context.provider().instance(IInternalContainerSync.class, uid);
					ContainerSyncResult res = service.sync();
					if (res != null) {
						if (res.status.syncStatus == Status.SUCCESS) {
							long end = System.currentTimeMillis() - start;
							sched.info(rid, "en",
									"Sync LDAP addressbook " + uid + ":" + name + " in " + end + "ms. created: "
											+ res.added + ", updated: " + res.updated + ", removed: " + res.removed);
							sched.info(rid, "fr",
									"Synchronisation du carnet d'adresses LDAP " + uid + ":" + name + " en " + end
											+ "ms. créés: " + res.added + ", modifiés: " + res.updated + ", supprimés: "
											+ res.removed);
						} else {
							long end = System.currentTimeMillis() - start;
							sched.warn(rid, "en",
									"Sync LDAP addressbook " + uid + ":" + name + " in " + end
											+ "ms, with warnings. created: " + res.added + ", updated: " + res.updated
											+ ", removed: " + res.removed);
							sched.warn(rid, "fr",
									"Synchronisation du carnet d'adresses LDAP " + uid + ":" + name + " en " + end
											+ "ms, avec des erreurs. créés: " + res.added + ", modifiés: " + res.updated
											+ ", supprimés: " + res.removed);
							ret = false;
						}
					} else {
						sched.warn(rid, "en", "Fail to sync LDAP addressbook " + uid + ":" + name);
						sched.warn(rid, "fr",
								"Erreur lors de la synchronisation du carnet d'adresses LDAP " + uid + ":" + name);
						ret = false;
					}
				} catch (ServerFault sf) {
					logger.error("Fail to sync LDAP addressbook {}", uid + ":" + name, sf);
					sched.warn(rid, "en", "Fail to sync LDAP addressbook " + uid + ":" + name);
					sched.warn(rid, "fr",
							"Erreur lors de la synchronisation du carnet d'adresses LDAP " + uid + ":" + name);
					ret = false;
				}
				i++;
				sched.reportProgress(rid, (int) (i * total));
			}
		}

		long end = System.currentTimeMillis() - begin;
		sched.info(rid, "en", "LDAP Addressbooks sync in " + end + "ms");
		sched.info(rid, "fr", "Carnets d'adresses LDAP synchronisés en " + end + "ms");

		return ret;
	}

	@Override
	public JobKind getType() {
		return JobKind.GLOBAL;
	}

	@Override
	public String getDescription(String locale) {
		if ("fr".equals(locale)) {
			return "Synchronisation des carnets d'adresses LDAP";
		}
		return "LDAP addressbooks synchronization";
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
