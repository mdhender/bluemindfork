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
package net.bluemind.mailbox.job;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.QuotaInfo;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class QuotaGatheringJob implements IScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(QuotaGatheringJob.class);
	private static final int WARNING_PERCENT = 85;

	@Override
	public void tick(IScheduler sched, boolean plannedExecution, String domainName, Date startDate) throws ServerFault {
		if ("global.virt".equals(domainName)) {
			return;
		}

		if (!plannedExecution) {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(startDate);
			if (gc.get(Calendar.MINUTE) != 0 || gc.get(Calendar.HOUR_OF_DAY) != 1) {
				logger.debug("automatic mode, not running at {}", gc.getTime().toString());
				return;
			}
		}

		IScheduledJobRunId jobRunId = sched.requestSlot(domainName, this, startDate);
		if (jobRunId != null) {
			try {
				boolean warning = false;
				Map<String, ItemValue<Server>> servers = new HashMap<String, ItemValue<Server>>();

				IMailboxes service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IMailboxes.class, domainName);

				List<ItemValue<Mailbox>> mboxes = service.list();

				for (ItemValue<Mailbox> mailbox : mboxes) {
					if (mailbox.value.quota != null) {

						if (null == mailbox.value.dataLocation) {
							logger.info("Mailbox {} has no datalocation, Skipping...", mailbox.value.name);
							continue;
						}

						ItemValue<Server> srv = servers.get(mailbox.value.dataLocation);
						if (srv == null) {
							srv = getServer(mailbox.value.dataLocation);
							servers.put(mailbox.value.dataLocation, srv);
						}

						warning = warning | process(sched, jobRunId, domainName, srv, mailbox);
					}
				}

				sched.finish(jobRunId, warning ? JobExitStatus.COMPLETED_WITH_WARNINGS : JobExitStatus.SUCCESS);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				sched.error(jobRunId, "en", e.getMessage());
				sched.error(jobRunId, "fr", e.getMessage());
				sched.finish(jobRunId, JobExitStatus.FAILURE);
			}
		}

	}

	private ItemValue<Server> getServer(String serverUid) throws ServerFault {
		ItemValue<Server> srvItem = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).getComplete(serverUid);
		if (srvItem == null) {
			throw new ServerFault("imap data location server: " + serverUid + " not found");
		}
		return srvItem;
	}

	private boolean process(IScheduler sched, IScheduledJobRunId rid, String domainName, ItemValue<Server> srv,
			ItemValue<Mailbox> mailbox) {
		boolean warn = false;
		try {
			CyrusService cyrus = new CyrusService(srv.value.address());

			// Cyrus quota
			String mboxName = mailbox.value.name + "@" + domainName;
			if (mailbox.value.type == Type.user) {
				mboxName = "user/" + mboxName;
			}
			QuotaInfo qi = cyrus.getQuota(mboxName);
			int usage = qi.getUsage();
			int limit = qi.getLimit();

			// Archive size
			double archiveSize = 0;
			if (mailbox.value.type == Type.user) {
				// size used on archive in Mo
				archiveSize = RecordIndexActivator.getIndexer().get().getArchivedMailSum(mailbox.uid) / (1000 * 1000);
			}

			logger.info("Quota for {} : usage {}, limit {}, archive {}", mailbox.value.name, usage, limit, archiveSize);

			String frArchiveSize = "";
			String enArchiveSize = "";
			if (archiveSize > Double.MIN_NORMAL) {
				DecimalFormat df = new DecimalFormat("#.##");
				frArchiveSize = "( espace archive utilisé : " + df.format(archiveSize) + " Mo )";
				enArchiveSize = "( Archive space used : " + df.format(archiveSize) + " Mo )";
			}

			if (usage > 0) {
				int pct = (100 * usage) / limit;
				if (pct >= WARNING_PERCENT) {
					warn = true;
					sched.warn(rid, "en",
							"usage for " + mailbox.value.name + " is above warning threshold (" + WARNING_PERCENT
									+ "%): " + pct + "% in use (used: " + usage + " / " + limit + ") " + enArchiveSize);
					sched.warn(rid, "fr",
							"L'utilisation pour " + mailbox.value.name + " dépasse le niveau d'alerte ("
									+ WARNING_PERCENT + "%) : " + pct + "% utilisés (consommé : " + usage + " / "
									+ limit + ") " + frArchiveSize);
				} else {
					sched.info(rid, "en", "usage for " + mailbox.value.name + " is: " + pct + "% in use (used: " + usage
							+ " / " + limit + ") " + enArchiveSize);
					sched.info(rid, "fr", "L'utilisation pour " + mailbox.value.name + " est : " + pct
							+ "% utilisés (consommé : " + usage + " / " + limit + ") " + frArchiveSize);
				}
			} else {
				sched.info(rid, "en",
						"usage for " + mailbox.value.name + " is: 0% in use (usable: " + limit + ") " + enArchiveSize);
				sched.info(rid, "fr", "L'utilisation pour " + mailbox.value.name + " est : 0% utilisé (consommable : "
						+ limit + ") " + frArchiveSize);
			}
		} catch (Exception e) {
			warn = true;
			logger.warn("problem fetching quota for {}", mailbox.value.name, e);
			sched.warn(rid, "en",
					String.format("Problem fetching quota for %s: %s", mailbox.value.name, e.getMessage()));
			sched.warn(rid, "fr", String.format("Probleme lors de l'analyse du quota pour %s: %s", mailbox.value.name,
					e.getMessage()));
		}

		return warn;
	}

	@Override
	public JobKind getType() {
		return JobKind.MULTIDOMAIN;
	}

	@Override
	public String getDescription(String locale) {
		if ("fr".equals(locale)) {
			return "Analyse de la consommation des quotas de messagerie";
		}
		return "Gather user quotas usage";
	}

	@Override
	public String getJobId() {
		return getClass().getCanonicalName();
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
