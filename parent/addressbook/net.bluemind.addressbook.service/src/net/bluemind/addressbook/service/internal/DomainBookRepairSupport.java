/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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

import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;

public class DomainBookRepairSupport implements IDirEntryRepairSupport {

	public static class Factory implements IDirEntryRepairSupport.Factory {

		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new DomainBookRepairSupport(context);
		}

	}

	private final BmContext context;

	public static final String REPAIR_AB_CONTAINER = " domainBook.abContainer";

	public DomainBookRepairSupport(BmContext context) {
		this.context = context;
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind != Kind.ADDRESSBOOK) {
			return ImmutableSet.of();
		} else {
			MaintenanceOperation op = new MaintenanceOperation();
			op.identifier = REPAIR_AB_CONTAINER;
			op.description = ResourceBundle
					.getBundle("OSGI-INF/l10n/DomainBookRepairSupport",
							new Locale(context.getSecurityContext().getLang()))
					.getString("defaultAddressBook.description");
			return ImmutableSet.of(op);
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind != Kind.ADDRESSBOOK) {
			return ImmutableSet.of();
		} else {
			return ImmutableSet.of(new RepairAB(context));
		}

	}

	public static class RepairAB extends InternalMaintenanceOperation {

		private BmContext context;

		public RepairAB(BmContext context) {
			super(REPAIR_AB_CONTAINER, null, null, 1);
			this.context = context;
		}

		@Override
		public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			checkAndRepair(false, domainUid, entry, report, monitor);
		}

		@Override
		public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			checkAndRepair(true, domainUid, entry, report, monitor);
		}

		private void checkAndRepair(boolean repair, String domainUid, DirEntry entry, DiagnosticReport report,
				IServerTaskMonitor monitor) {
			monitor.begin(2, String.format("Check container validity %s", entry.entryUid));
			ContainerStore cs = new ContainerStore(context, context.getMailboxDataSource(entry.dataLocation),
					context.getSecurityContext());
			Container container = null;
			try {
				container = cs.get(entry.entryUid);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}

			monitor.progress(1, "lookup container " + entry.entryUid);

			if (container != null) {
				report.ok(REPAIR_AB_CONTAINER, "container is ok");
				return;
			}

			if (!repair) {
				report.ko(REPAIR_AB_CONTAINER, "container " + entry.entryUid + " not found");
				return;
			} else {
				report.warn(REPAIR_AB_CONTAINER, "container " + entry.entryUid + " not found, going to recreate it");
				container = Container.create(entry.entryUid, IAddressBookUids.TYPE, entry.displayName, entry.entryUid,
						domainUid, true);
				try {
					container = cs.create(container);
				} catch (SQLException e) {
					throw ServerFault.sqlFault(e);
				}
				monitor.progress(1, "container " + entry.entryUid + " repair finished!");
				report.ok(REPAIR_AB_CONTAINER, "container repair is a success !!");
			}
		}
	}

}
