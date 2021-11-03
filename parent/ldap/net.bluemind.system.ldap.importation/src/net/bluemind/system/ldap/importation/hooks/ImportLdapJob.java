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
package net.bluemind.system.ldap.importation.hooks;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.commons.scanner.RepportStatus;
import net.bluemind.system.importation.commons.scanner.Scanner;
import net.bluemind.system.ldap.importation.LdapScannerFactory;
import net.bluemind.system.ldap.importation.api.LdapConstants;
import net.bluemind.system.ldap.importation.api.LdapProperties;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;

public class ImportLdapJob implements IScheduledJob {
	private static final Logger logger = LoggerFactory.getLogger(ImportLdapJob.class);

	private static final long EXECUTION_INTERVAL = TimeUnit.MILLISECONDS.convert(4, TimeUnit.HOURS);

	private Date lastRun = null;

	public ImportLdapJob() {
		lastRun = new Date();
	}

	@Override
	public void tick(IScheduler sched, boolean plannedExecution, String domainName, Date startDate) {
		if ("global.virt".equals(domainName)) {
			return;
		}

		if (!importMustBeRun(plannedExecution, startDate)) {
			if (logger.isDebugEnabled()) {
				logger.debug(" * Not a forced run condition: " + domainName);
			}
			return;
		} else {
			logger.info("Forced run condition: " + domainName + " (scheduled or manual start)");
		}

		lastRun = startDate;

		logger.info("Run import LDAP job at: " + startDate.toString());

		IScheduledJobRunId rid = null;

		ItemValue<Domain> domain = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class).get(domainName);
		Map<String, String> domainSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domain.uid).get();
		LdapParameters ldapParameters = LdapParameters.build(domain.value, domainSettings);

		if (!ldapParameters.enabled) {
			return;
		}

		rid = sched.requestSlot(domainName, this, startDate);

		if (ldapParameters.lastUpdate.isPresent()) {
			sched.info(rid, "en",
					"LDAP incremental update for domain: " + domainName + " since: " + ldapParameters.lastUpdate);
			sched.info(rid, "fr", "Import LDAP incrémental pour le domaine : " + domainName + " depuis: "
					+ ldapParameters.lastUpdate.get());
		} else {
			sched.info(rid, "en", "LDAP global import for domain: " + domainName);
			sched.info(rid, "fr", "Import LDAP global pour le domaine : " + domainName);
		}

		ImportLogger importLogger = new ImportLogger(Optional.ofNullable(sched), Optional.ofNullable(rid),
				Optional.of(new RepportStatus()));

		Scanner ldapScanner = LdapScannerFactory.getLdapScanner(importLogger, ldapParameters, domain);
		ldapScanner.scan();

		updateLastUpdateDomainDate(domain, importLogger.repportStatus.get().getJobStatus(), startDate);
		sched.finish(rid, importLogger.repportStatus.get().getJobStatus());
	}

	protected void updateLastUpdateDomainDate(ItemValue<Domain> domain, JobExitStatus importStatus, Date d)
			throws ServerFault {
		switch (importStatus) {
		case SUCCESS:
			logger.info("LDAP import job terminated");
			domain.value.properties.put(LdapProperties.import_ldap_lastupdate.name(),
					getDateInGeneralizedTimeFormat(d));
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class).update(domain.uid,
					domain.value);
			break;
		case COMPLETED_WITH_WARNINGS:
			logger.warn("LDAP import job terminated with warning");
			break;
		default:
			logger.error("LDAP import job terminated with error");
			break;
		}
	}

	protected static String getDateInGeneralizedTimeFormat(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(LdapConstants.GENERALIZED_TIME_FORMAT);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		return sdf.format(date);
	}

	/*
	 * Run LDAP incremental job on BM-core planned execution or every
	 * EXECUTION_INTERVAL
	 */
	private boolean importMustBeRun(boolean plannedExecution, Date d) {
		if (plannedExecution || lastRun == null) {
			return true;
		}

		if (EXECUTION_INTERVAL < d.getTime() - lastRun.getTime()) {
			return true;
		}

		return false;
	}

	@Override
	public JobKind getType() {
		return JobKind.MULTIDOMAIN;
	}

	@Override
	public String getDescription(String locale) {
		if ("fr".equals(locale)) {
			return "Importe un annuaire LDAP externe";
		} else {
			return "Imports an external LDAP directory";
		}
	}

	@Override
	public String getJobId() {
		return LdapConstants.JID;
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
