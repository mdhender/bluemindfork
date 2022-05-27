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
package net.bluemind.user.service.internal;

import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.repair.ContainerRepairOp;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.RepairTaskMonitor;

public class DeferredActionUserRepair implements ContainerRepairOp {

	@Override
	public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		String userUid = entry.entryUid;
		String containerUid = IDeferredActionContainerUids.uidForUser(userUid);
		Runnable maintenance = () -> {
		};

		verifyContainer(domainUid, monitor, maintenance, containerUid);
	}

	@Override
	public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		String userUid = entry.entryUid;
		String containerUid = IDeferredActionContainerUids.uidForUser(userUid);
		Runnable maintenance = () -> {
			String logMessage = "Repairing deferred action container of user {}";
			monitor.log(logMessage.replace("{}", userUid));

			ContainerDescriptor descriptor = ContainerDescriptor.create(containerUid, containerUid, userUid,
					IDeferredActionContainerUids.TYPE, domainUid, true);
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IContainers.class)
					.create(containerUid, descriptor);
		};

		verifyContainer(domainUid, monitor, maintenance, containerUid);
	}

	@Override
	public Kind supportedKind() {
		return Kind.USER;
	}
}
