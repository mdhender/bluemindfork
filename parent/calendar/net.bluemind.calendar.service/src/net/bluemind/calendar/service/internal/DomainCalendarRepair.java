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
package net.bluemind.calendar.service.internal;

import java.util.function.Consumer;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.repair.ContainerRepairOp;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.service.RepairTaskMonitor;

public class DomainCalendarRepair implements ContainerRepairOp {

	@Override
	public void check(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		verifyDomainCalendars(context, monitor, domainUid, (cal) -> {
		});

	}

	@Override
	public void repair(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		verifyDomainCalendars(context, monitor, domainUid, (cal) -> {
			IContainers containerService = context.getServiceProvider().instance(IContainers.class);
			ContainerDescriptor descriptor = new ContainerDescriptor();
			descriptor.type = "calendar";
			descriptor.uid = cal.uid;
			descriptor.owner = cal.uid;
			descriptor.name = cal.displayName;
			descriptor.defaultContainer = false;
			descriptor.offlineSync = false;
			descriptor.readOnly = false;
			descriptor.domainUid = domainUid;
			containerService.create(cal.uid, descriptor);
		});
	}

	private void verifyDomainCalendars(BmContext context, RepairTaskMonitor monitor, String domainUid,
			Consumer<CalendarInfo> repairOp) {

		IDirectory dir = context.getServiceProvider().instance(IDirectory.class, domainUid);
		IContainers containerService = context.getServiceProvider().instance(IContainers.class);

		ListResult<ItemValue<DirEntry>> cals = dir.search(DirEntryQuery.filterKind(Kind.CALENDAR));
		for (ItemValue<DirEntry> cal : cals.values) {
			String uid = cal.value.entryUid;
			if (containerService.getIfPresent(uid) == null) {
				monitor.notify("Domain calendar {} is missing associated container", uid);
				repairOp.accept(new CalendarInfo(uid, cal.displayName));
			}
		}

	}

	static class CalendarInfo {
		public final String uid;
		public final String displayName;

		public CalendarInfo(String uid, String displayName) {
			this.uid = uid;
			this.displayName = displayName;
		}
	}

	@Override
	public Kind supportedKind() {
		return Kind.DOMAIN;
	}

}
