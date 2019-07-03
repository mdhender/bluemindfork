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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.index.mail.BoxIndexing;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;
import net.bluemind.scheduledjob.scheduler.SchedulerTaskMonitor;

public class ConsolidateMailspoolIndexJob extends MailspoolJob implements IScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(ConsolidateMailspoolIndexJob.class);

	@Override
	public void tick(IScheduler sched, boolean plannedExecution, String domainName, Date startDate) throws ServerFault {

		if (!plannedExecution) {
			logger.debug("Not planned");
			return;
		}

		IDomains service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		IScheduledJobRunId jobRunId = sched.requestSlot("global.virt", this, startDate);
		if (jobRunId != null) {
			createIndexIfNecessary();

			List<ItemValue<Domain>> domains = service.all();
			for (ItemValue<Domain> domain : domains) {
				if (!domain.uid.equals("global.virt")) {
					prepareDomainMailboxes(sched, jobRunId, domain.value.name, startDate);
				}
			}

			for (ItemValue<Domain> domain : domains) {
				if (!domain.uid.equals("global.virt")) {
					processDomainMailboxes(sched, jobRunId, domain.value.name);
				}
			}

			sched.finish(jobRunId, jobStatus);

		}
	}

	/**
	 * @param sched
	 * @param jobRunId
	 * @param domainName
	 * @param startDate
	 * @throws ServerFault
	 */
	private void processDomainMailboxes(IScheduler sched, IScheduledJobRunId jobRunId, String domainName)
			throws ServerFault {

		long start = System.currentTimeMillis();

		logger.info("Start consolidate mailspool index for domain {}", domainName);

		sched.info(jobRunId, "en", "Start consolidate mailspool index for domain " + domainName);
		sched.info(jobRunId, "fr",
				"Démarrage de la consolidation de l'index de messagerie pour le domaine " + domainName);

		BoxIndexing mailboxIndexer = new BoxIndexing(domainName);
		IMailboxes service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class,
				domainName);

		try {
			List<ItemValue<Mailbox>> mailboxes = service.list();
			int mboxConsolidated = 0;
			for (ItemValue<Mailbox> mbox : mailboxes) {
				try {

					mboxConsolidated++;

					if (mbox.value.routing == Routing.internal) {
						mailboxIndexer.resync(mbox, new SchedulerTaskMonitor(sched, jobRunId));
						sched.info(jobRunId, "en", mbox.value.defaultEmail() + ": index resync (" + mboxConsolidated
								+ "/" + mailboxes.size() + ")");
						sched.info(jobRunId, "fr", mbox.value.defaultEmail() + ": index resync (" + mboxConsolidated
								+ "/" + mailboxes.size() + ")");

					}
				} catch (Exception e) {
					jobStatus = JobExitStatus.COMPLETED_WITH_WARNINGS;
					logger.warn("Fail to consolidate index for mailbox {}", mbox.uid, e);

					sched.warn(jobRunId, "en", String.format("Fail to consolidate index for mailbox %s (uid: %s)",
							mbox.displayName, mbox.uid));
					sched.warn(jobRunId, "fr",
							String.format("Erreur lors de la consolidation de l'index de la boîte %s (uid: %s)",
									mbox.displayName, mbox.uid));

				}
				sched.reportProgress(jobRunId, mboxConsolidated / mailboxes.size() * 100);

			}
			long end = (System.currentTimeMillis() - start);

			logger.info("Consolidate mailspool index for domain {} done in {}ms", domainName, end);

			sched.info(jobRunId, "en",
					"Mailspool index consolidation for domain " + domainName + " is done (" + end + "ms)");
			sched.info(jobRunId, "fr", "Consolidation de l'index de messagerie pour le domaine " + domainName
					+ " terminée (" + end + "ms)");

		} catch (Exception t) {
			logger.error(t.getMessage(), t);

			sched.error(jobRunId, "en", t.getMessage());
			sched.error(jobRunId, "fr", t.getMessage());
			sched.finish(jobRunId, JobExitStatus.FAILURE);
		}
	}

	@Override
	public JobKind getType() {
		return JobKind.GLOBAL;
	}

	@Override
	public String getDescription(String locale) {
		if ("fr".equals(locale)) {
			return "Consolide l'index plein-texte des emails";
		} else {
			return "Consolidate mail spool full-text indexing";
		}
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
		return false;
	}

}
