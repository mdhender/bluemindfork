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
package net.bluemind.smime.cacerts.service.internal;

import java.util.function.Consumer;

import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.repair.ContainerRepairOp;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.smime.cacerts.api.ISmimeCacertUids;

public class DomainSmimeCacertsRepair implements ContainerRepairOp {

	@Override
	public void check(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		verifyDomainSmimeCacerts(context, monitor, domainUid, (uid) -> {
		});

	}

	@Override
	public void repair(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		verifyDomainSmimeCacerts(context, monitor, domainUid, (uid) -> {
			IContainers containerService = context.getServiceProvider().instance(IContainers.class);
			ContainerDescriptor descriptor = new ContainerDescriptor();
			descriptor.type = ISmimeCacertUids.TYPE;
			descriptor.uid = uid;
			descriptor.owner = domainUid;
			descriptor.name = uid;
			// domain smimecacerts check
			descriptor.defaultContainer = true;
			descriptor.offlineSync = false;
			descriptor.readOnly = false;
			descriptor.domainUid = domainUid;
			containerService.create(uid, descriptor);
		});
	}

	private void verifyDomainSmimeCacerts(BmContext context, RepairTaskMonitor monitor, String domainUid,
			Consumer<String> repairOp) {

		IContainers containerService = context.getServiceProvider().instance(IContainers.class);

		String uid = ISmimeCacertUids.domainCreatedCerts(domainUid);
		if (containerService.getIfPresent(uid) == null) {
			monitor.notify("Domain smimecacerts {} is missing associated container", uid);
			repairOp.accept(uid);
		}

	}

	@Override
	public Kind supportedKind() {
		return Kind.DOMAIN;
	}

}
