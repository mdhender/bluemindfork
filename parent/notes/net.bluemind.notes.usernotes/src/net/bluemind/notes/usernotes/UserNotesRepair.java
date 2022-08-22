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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.notes.usernotes;

import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.repair.ContainerRepairOp;
import net.bluemind.core.container.repair.ContainerRepairUtil;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.notes.api.INoteUids;

public class UserNotesRepair implements ContainerRepairOp {

	@Override
	public void check(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		String userUid = entry.entryUid;
		String containerUid = INoteUids.defaultUserNotes(userUid);
		Runnable maintenance = () -> {
		};

		verifyContainer(domainUid, monitor, maintenance, containerUid);

		ContainerRepairUtil.verifyContainerIsMarkedAsDefault(containerUid, monitor, maintenance);

		ContainerRepairUtil.verifyContainerSubscription(entry.entryUid, domainUid, monitor, (container) -> {
		}, containerUid);
	}

	@Override
	public void repair(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		String userUid = entry.entryUid;
		String containerUid = INoteUids.defaultUserNotes(userUid);
		Runnable maintenance = () -> {

			ContainerDescriptor descriptor = ContainerDescriptor.create(containerUid, containerUid, userUid,
					INoteUids.TYPE, domainUid, true);
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IContainers.class)
					.create(containerUid, descriptor);
		};

		verifyContainer(domainUid, monitor, maintenance, containerUid);

		ContainerRepairUtil.verifyContainerIsMarkedAsDefault(containerUid, monitor, () -> {
			ContainerRepairUtil.setAsDefault(containerUid, context, monitor);
		});

		ContainerRepairUtil.verifyContainerSubscription(entry.entryUid, domainUid, monitor, (container) -> {
			ContainerRepairUtil.subscribe(entry.entryUid, domainUid, container);
		}, containerUid);

	}

	@Override
	public Kind supportedKind() {
		return Kind.USER;
	}
}
