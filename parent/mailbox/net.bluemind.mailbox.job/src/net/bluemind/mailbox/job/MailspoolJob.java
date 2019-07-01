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

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.service.IInCoreMailboxes;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class MailspoolJob {

	Logger logger = LoggerFactory.getLogger(MailspoolJob.class);

	protected void createIndexIfNecessary() {
		ESearchActivator.initIndex("mailspool");
	}

	protected JobExitStatus jobStatus = JobExitStatus.SUCCESS;

	protected void prepareDomainMailboxes(IScheduler sched, IScheduledJobRunId jobRunId, String domainName,
			Date startDate) throws ServerFault {

		long start = System.currentTimeMillis();

		logger.info("Start reconstruct mailspool index struture for domain {}", domainName);

		sched.info(jobRunId, "en", "Start reconstruct mailspool index structure for domain " + domainName);
		sched.info(jobRunId, "fr",
				"Démarrage de la reconstruction de la structure de l'index de messagerie pour le domaine "
						+ domainName);

		IInCoreMailboxes service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInCoreMailboxes.class, domainName);
		List<ItemValue<Mailbox>> mailboxes = service.list();
		int mboxReconstructed = 0;
		for (ItemValue<Mailbox> mbox : mailboxes) {
			try {
				service.checkAndRepairTask(mbox.uid, new NullTaskMonitor());
			} catch (Exception e) {
				jobStatus = JobExitStatus.COMPLETED_WITH_WARNINGS;
				logger.warn("Fail to reconstruct mailspool index structure for mailbox {}", mbox.uid, e);

				sched.error(jobRunId, "en",
						String.format("Fail to reconstruct mailspool index structure for mailbox %s (uid: %s)",
								mbox.displayName, mbox.uid));
				sched.error(jobRunId, "fr",
						String.format(
								"Erreur lors de la reconstruction de la structure de l'index de la boîte %s (uid: %s)",
								mbox.displayName, mbox.uid));

			}
		}
		long end = (System.currentTimeMillis() - start);

		logger.info("Reconstruct mailspool index structure, {} mailbox(es) for domain {} done in {}ms",
				mboxReconstructed, domainName, end);

		sched.info(jobRunId, "en",
				"Mailspool index structure reconstruction for domain " + domainName + " is done (" + end + "ms)");
		sched.info(jobRunId, "fr", "Reconstruction de la structure de l'index de messagerie pour le domaine "
				+ domainName + " terminée (" + end + "ms)");

	}

}
