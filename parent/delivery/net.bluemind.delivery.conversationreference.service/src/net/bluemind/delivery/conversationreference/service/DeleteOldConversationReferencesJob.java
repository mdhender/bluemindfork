/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.delivery.conversationreference.service;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class DeleteOldConversationReferencesJob implements IScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(DeleteOldConversationReferencesJob.class);
	private IScheduledJobRunId rid;

	@Override
	public void tick(IScheduler sched, boolean forced, String domainName, Date startDate) throws ServerFault {

		if (!forced) {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(startDate);
			if (gc.get(Calendar.MINUTE) != 0 || gc.get(Calendar.HOUR_OF_DAY) != 1) {
				return;
			}
		}
		logger.info("Run deleting old entries from t_conversationreference table job at: {}", startDate.toString());
		rid = sched.requestSlot(domainName, this, startDate);
		if (rid == null) {
			return;
		}

		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		IInCoreConversationReferenceMgmt service = provider.instance(IInCoreConversationReferenceMgmt.class);
		try {
			long deleteEntries = service.deleteEntriesOlderThanOneYear();
			logger.info("Number of deleted entries older than one year for t_conversationreference: {}", deleteEntries);
			sched.info(rid, "en", String.format(
					"Number of deleted entries older than one year for t_conversationreference: %s", deleteEntries));
			sched.finish(rid, JobExitStatus.SUCCESS);
		} catch (ServerFault e) {
			logger.error(e.getMessage());
			sched.warn(rid, "en", String.format("Problem deleting old entries: %s", e.getMessage()));
			sched.finish(rid, JobExitStatus.FAILURE);
		}

	}

	@Override
	public JobKind getType() {
		return JobKind.GLOBAL;
	}

	@Override
	public String getDescription(String locale) {
		return "Remove references older than one year from t_conversationreference table";
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
