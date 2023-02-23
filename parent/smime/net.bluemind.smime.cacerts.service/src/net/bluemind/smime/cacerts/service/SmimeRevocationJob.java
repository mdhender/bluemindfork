/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.smime.cacerts.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;
import net.bluemind.smime.cacerts.api.SmimeCacert;

public class SmimeRevocationJob implements IScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(SmimeRevocationJob.class);

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
				String formattedDate = DateTimeFormatter.ISO_LOCAL_DATE
						.format(startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
				IInCoreSmimeRevocation service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IInCoreSmimeRevocation.class, domainName);

				List<ItemValue<SmimeCacert>> cacerts = service.getByNextUpdateDate(startDate);
				if (cacerts == null || cacerts.isEmpty()) {
					String enLogMsg = String.format("no revocations list to update for domain %s on %s", domainName,
							formattedDate);
					logger.info(enLogMsg);
					sched.info(jobRunId, "en", enLogMsg);
					sched.info(jobRunId, "fr",
							String.format("aucune révocation à mettre à jour pour le domaine %s à cette date %s",
									domainName, formattedDate));
					sched.finish(jobRunId, JobExitStatus.SUCCESS);
				} else {
					String enLogMsg = String.format("%d S/MIME CA are concerned by the update of their revocations.",
							cacerts.size());
					logger.info(enLogMsg);
					sched.info(jobRunId, "en", enLogMsg);
					sched.info(jobRunId, "fr", String.format(
							"%d CA S/MIME sont concernés par la mise à jour de leurs révocations.", cacerts.size()));
				}

				boolean warn = false;
				for (ItemValue<SmimeCacert> ca : cacerts) {
					logger.info("update revocations list for S/MIME certificate {}", ca.uid);
					try {
						service.refreshRevocations(ca);
					} catch (Exception e) {
						warn = true;
						logger.warn(e.getMessage(), e);
						sched.warn(jobRunId, "en", String.format("fetch revoked certificates for S/MIME CA %s : %s",
								ca.displayName, e.getMessage()));
						sched.warn(jobRunId, "fr",
								String.format("récupération des certificats révoqués par le CA S/MIME %s : %s",
										ca.displayName, e.getMessage()));
					}
				}
				sched.finish(jobRunId, warn ? JobExitStatus.COMPLETED_WITH_WARNINGS : JobExitStatus.SUCCESS);

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				sched.error(jobRunId, "en", e.getMessage());
				sched.error(jobRunId, "fr", e.getMessage());
				sched.finish(jobRunId, JobExitStatus.FAILURE);
			}
		}
	}

	@Override
	public JobKind getType() {
		return JobKind.MULTIDOMAIN;
	}

	@Override
	public String getDescription(String locale) {
		if ("fr".equals(locale)) {
			return "Rafraîchir les certificats clients révoqués S/MIME pour un domaine";
		}
		return "Refresh S/MIME revoked client certificates for a domain";
	}

	@Override
	public String getJobId() {
		return getClass().getCanonicalName();
	}

	@Override
	public boolean supportsScheduling() {
		return true;
	}

}
