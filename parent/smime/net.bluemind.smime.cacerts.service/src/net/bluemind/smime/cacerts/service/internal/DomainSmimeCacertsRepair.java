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

import java.util.Arrays;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.repair.ContainerRepairOp;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.smime.cacerts.api.ISmimeCacertUids;

public class DomainSmimeCacertsRepair implements ContainerRepairOp {

	private static final Logger logger = LoggerFactory.getLogger(DomainSmimeCacertsRepair.class);

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
			descriptor.defaultContainer = true;
			descriptor.offlineSync = false;
			descriptor.readOnly = false;
			descriptor.domainUid = domainUid;
			containerService.create(uid, descriptor);

			addDomainReadAccess(context, uid, domainUid);
		});
	}

	private void addDomainReadAccess(BmContext context, String containerUid, String domainUid) {
		context.provider().instance(IContainerManagement.class, containerUid)
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(domainUid, Verb.Read)));
	}

	private void verifyDomainSmimeCacerts(BmContext context, RepairTaskMonitor monitor, String domainUid,
			Consumer<String> repairOp) {
		IContainers containerService = context.getServiceProvider().instance(IContainers.class);

		String uid = ISmimeCacertUids.domainCreatedCerts(domainUid);
		if (containerService.getLightIfPresent(uid) == null) {
			logger.info("Domain smimecacerts {} is missing associated container", uid);
			monitor.notify("Domain smimecacerts {} is missing associated container", uid);
			repairOp.accept(uid);
		}

	}

	@Override
	public Kind supportedKind() {
		return Kind.DOMAIN;
	}

}
