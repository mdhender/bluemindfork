/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.auditlogs.client.es.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.auditlogs.client.loader.AuditLogLoader;
import net.bluemind.core.auditlogs.exception.AuditLogCreationException;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.domain.api.IDomains;
import net.bluemind.maintenance.IMaintenanceScript;

public class DataStreamConsistency implements IMaintenanceScript {
	private static final String AUDIT_LOG_PREFIX = "audit_log";
	private static final String SEPARATOR = "_";

	private static final Logger logger = LoggerFactory.getLogger(DataStreamConsistency.class);

	@Override
	public void run(IServerTaskMonitor monitor) {
		long start = System.currentTimeMillis();
		logger.info("Run consistency check for auditlog datastreams");
		IDomains domainsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class);

		AuditLogLoader auditLogProvider = new AuditLogLoader();
		domainsService.all().forEach(d -> {
			String dataStreamFullName = AUDIT_LOG_PREFIX + SEPARATOR + d.uid;
			boolean isDataStream = auditLogProvider.getManager().hasAuditBackingStoreForDomain(d.uid);
			if (!isDataStream) {
				logger.info("Datastream '{}' does not exist : must be created", dataStreamFullName);
				try {
					auditLogProvider.getManager().setupAuditBackingStoreForDomain(d.uid);
				} catch (AuditLogCreationException e) {
					monitor.log("error exexuting " + name() + ": " + e);
					monitor.end(false, null, e.getMessage());
				}
			}
		});
		monitor.end(true, name() + " took " + (System.currentTimeMillis() - start) + " ms to execute", null);
	}

	@Override
	public String name() {
		return "DataStreamConsistency";
	}
}
