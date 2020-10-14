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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.service.IInCoreMailboxes;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class OutOfOfficeJob implements IScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(OutOfOfficeJob.class);

	@Override
	public void tick(IScheduler sched, boolean plannedExecution, String domainName, Date startDate) throws ServerFault {
		IScheduledJobRunId rid = sched.requestSlot(domainName, this, startDate);
		if (rid != null) {
			try {
				sched.finish(rid, run(sched, rid));
			} catch (Exception t) {
				logger.error(t.getMessage(), t);
				sched.error(rid, "en", t.getMessage());
				sched.error(rid, "fr", t.getMessage());
				sched.finish(rid, JobExitStatus.FAILURE);
			}
		}

	}

	private JobExitStatus run(IScheduler sched, IScheduledJobRunId rid) throws ServerFault {
		JobExitStatus result = JobExitStatus.SUCCESS;

		BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

		IDomains domains = context.provider().instance(IDomains.class);

		for (ItemValue<Domain> domain : domains.all()) {
			if ("global.virt".equals(domain.uid)) {
				continue;
			}

			logger.debug("executing outofoffice for domain {}", domain.uid);
			JobExitStatus domainResult = context.provider().instance(IInCoreMailboxes.class, domain.uid)
					.refreshOutOfOffice(sched, rid);
			if (result == JobExitStatus.SUCCESS || domainResult == JobExitStatus.FAILURE) {
				result = domainResult;
			}
		}

		return result;
	}

	@Override
	public JobKind getType() {
		return JobKind.GLOBAL;
	}

	@Override
	public String getDescription(String locale) {
		return "Gère l'activation programmée du répondeur automatique de messagerie";
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
