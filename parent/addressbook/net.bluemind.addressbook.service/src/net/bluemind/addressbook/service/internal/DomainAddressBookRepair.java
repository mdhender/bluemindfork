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
package net.bluemind.addressbook.service.internal;

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

public class DomainAddressBookRepair implements ContainerRepairOp {

	@Override
	public void check(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		verifyDomainAddressBooks(context, monitor, domainUid, (ab) -> {
		});

	}

	@Override
	public void repair(BmContext context, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {

		verifyDomainAddressBooks(context, monitor, domainUid, (ab) -> {
			IContainers containerService = context.getServiceProvider().instance(IContainers.class);
			ContainerDescriptor descriptor = new ContainerDescriptor();
			descriptor.type = "addressbook";
			descriptor.uid = ab.uid;
			descriptor.owner = ab.uid;
			descriptor.name = ab.displayName;
			descriptor.defaultContainer = ab.uid.equals("addressbook_" + domainUid); // domain addressbook check
			descriptor.offlineSync = false;
			descriptor.readOnly = false;
			descriptor.domainUid = domainUid;
			containerService.create(ab.uid, descriptor);
		});
	}

	private void verifyDomainAddressBooks(BmContext context, RepairTaskMonitor monitor, String domainUid,
			Consumer<AddressBookInfo> repairOp) {

		IDirectory dir = context.getServiceProvider().instance(IDirectory.class, domainUid);
		IContainers containerService = context.getServiceProvider().instance(IContainers.class);

		DirEntryQuery filter = DirEntryQuery.filterKind(Kind.ADDRESSBOOK);
		filter.systemFilter = false;
		ListResult<ItemValue<DirEntry>> abs = dir.search(filter);

		for (ItemValue<DirEntry> ab : abs.values) {
			String uid = ab.value.entryUid;
			if (containerService.getIfPresent(uid) == null) {
				monitor.notify("Domain addressbook {} is missing associated container", uid);
				repairOp.accept(new AddressBookInfo(uid, ab.displayName));
			}
		}

	}

	static class AddressBookInfo {
		public final String uid;
		public final String displayName;

		public AddressBookInfo(String uid, String displayName) {
			this.uid = uid;
			this.displayName = displayName;
		}
	}

	@Override
	public Kind supportedKind() {
		return Kind.DOMAIN;
	}

}
