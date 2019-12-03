/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.domain.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.repair.ContainerRepairOp;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;

public class DeferredActionDomainRepair implements ContainerRepairOp {

	private static final Logger logger = LoggerFactory.getLogger(DeferredActionDomainRepair.class);

	@Override
	public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
		String containerUid = IDeferredActionContainerUids.uidForDomain(domainUid);

		Runnable maintenance = () -> {
			String logMessage = "Deferred action container of domain {} is missing";
			monitor.log(logMessage.replace("{}", domainUid));
			logger.info(logMessage, domainUid);
		};

		verifyContainer(domainUid, report, monitor, maintenance, containerUid);
	}

	@Override
	public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
		String containerUid = IDeferredActionContainerUids.uidForDomain(domainUid);

		Runnable maintenance = () -> {
			String logMessage = "Repairing deferred action container of domain {}";
			monitor.log(logMessage.replace("{}", domainUid));
			logger.info(logMessage, domainUid);
			ContainerDescriptor descriptor = ContainerDescriptor.create(containerUid, containerUid, domainUid,
					IDeferredActionContainerUids.TYPE, domainUid, true);
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IContainers.class)
					.create(containerUid, descriptor);
		};

		verifyContainer(domainUid, report, monitor, maintenance, containerUid);
	}

	@Override
	public Kind supportedKind() {
		return Kind.DOMAIN;
	}
}
