/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.core.container.repair;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;

public class ContainerRepair implements IDirEntryRepairSupport {

	public static final String REPAIR_OP_ID = "containers";
	private final BmContext context;

	public static final MaintenanceOperation containerOp = MaintenanceOperation.create(ContainerRepair.REPAIR_OP_ID,
			"Check well-known containers (default cal, collected contacts, ...)");

	public static class Factory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new ContainerRepair(context);
		}
	}

	public ContainerRepair(BmContext context) {
		this.context = context;
	}

	private static class ContainerMaintenance extends InternalMaintenanceOperation {

		private final BmContext context;

		public ContainerMaintenance(BmContext context) {
			super(containerOp.identifier, null, null, 1);
			this.context = context;
		}

		@Override
		public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			verify(domainUid, entry, monitor, op -> op.check(context, domainUid, entry, monitor));
			monitor.end();
		}

		@Override
		public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			verify(domainUid, entry, monitor, op -> op.repair(context, domainUid, entry, monitor));
			monitor.end();
		}

		private void verify(String domainUid, DirEntry entry, RepairTaskMonitor monitor,
				Consumer<ContainerRepairOp> maintenance) {
			Activator.ops.stream().filter(op -> op.supportedKind() == entry.kind).forEach(op -> {
				maintenance.accept(op);
			});
		}

	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (supportedKind(kind)) {
			return ImmutableSet.of(containerOp);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (supportedKind(kind)) {
			return ImmutableSet.of(new ContainerMaintenance(context));
		} else {
			return Collections.emptySet();
		}
	}

	private boolean supportedKind(Kind kind) {
		return kind == Kind.USER || kind == Kind.RESOURCE || kind == Kind.MAILSHARE || kind == Kind.GROUP
				|| kind == Kind.DOMAIN;
	}

}