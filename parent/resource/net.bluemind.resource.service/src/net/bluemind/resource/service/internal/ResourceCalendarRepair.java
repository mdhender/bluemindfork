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
package net.bluemind.resource.service.internal;

import java.util.Arrays;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.IFreebusyMgmt;
import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.repair.ContainerRepairOp;
import net.bluemind.core.container.repair.ContainerRepairUtil;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;

public class ResourceCalendarRepair implements ContainerRepairOp {

	@Override
	public void check(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		verifyDefaultContainer(domainUid, entry.entryUid, monitor, () -> {
		});

		String containerUid = ICalendarUids.getResourceCalendar(entry.entryUid);
		ContainerRepairUtil.verifyContainerIsMarkedAsDefault(containerUid, monitor, () -> {
		});

		verifyFreebusyContainer(domainUid, entry.entryUid, monitor, () -> {
		});

	}

	@Override
	public void repair(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		ResourceDescriptor descriptor = context.provider().instance(IResources.class, domainUid).get(entry.entryUid);

		verifyDefaultContainer(domainUid, entry.entryUid, monitor, () -> {
			ContainerDescriptor calContainerDescriptor = ContainerDescriptor.create(
					ICalendarUids.getResourceCalendar(entry.entryUid), descriptor.label, entry.entryUid,
					ICalendarUids.TYPE, domainUid, true);
			IContainers containers = context.su().provider().instance(IContainers.class);
			containers.create(calContainerDescriptor.uid, calContainerDescriptor);

		});

		String containerUid = ICalendarUids.getResourceCalendar(entry.entryUid);
		ContainerRepairUtil.verifyContainerIsMarkedAsDefault(containerUid, monitor, () -> {
			ContainerRepairUtil.setAsDefault(containerUid, context, monitor);
		});

		verifyFreebusyContainer(domainUid, entry.entryUid, monitor, () -> {
			IContainers containers = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainers.class);
			String fbContainerUid = IFreebusyUids.getFreebusyContainerUid(entry.entryUid);
			ContainerDescriptor containerDescriptor = ContainerDescriptor.create(fbContainerUid, "freebusy container",
					entry.entryUid, IFreebusyUids.TYPE, domainUid, true);
			containers.create(fbContainerUid, containerDescriptor);
			context.provider().instance(IContainerManagement.class, fbContainerUid)
					.setAccessControlList(Arrays.asList(AccessControlEntry.create(domainUid, Verb.Read)));

			context.provider().instance(IFreebusyMgmt.class, fbContainerUid)
					.add(ICalendarUids.getResourceCalendar(entry.entryUid));
		});

	}

	private void verifyDefaultContainer(String domainUid, String uid, RepairTaskMonitor monitor, Runnable maintenance) {
		String containerUid = ICalendarUids.getResourceCalendar(uid);
		verifyContainer(domainUid, monitor, maintenance, containerUid);
	}

	private void verifyFreebusyContainer(String domainUid, String uid, RepairTaskMonitor monitor,
			Runnable maintenance) {

		String containerUid = IFreebusyUids.getFreebusyContainerUid(uid);
		verifyContainer(domainUid, monitor, maintenance, containerUid);
	}

	@Override
	public Kind supportedKind() {
		return Kind.RESOURCE;
	}

}
