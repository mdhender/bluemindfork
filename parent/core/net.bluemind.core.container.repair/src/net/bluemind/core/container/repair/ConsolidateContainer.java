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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.container.repair;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.event.Level;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;

public class ConsolidateContainer implements IDirEntryRepairSupport {

	public static final String REPAIR_OP_ID = "consolidateContainer";

	public static final MaintenanceOperation containerOp = MaintenanceOperation
			.create(ConsolidateContainer.REPAIR_OP_ID, "Check containers integrity (missing sequence, settings ...)");

	public static class ConsolidateContainerFactory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new ConsolidateContainer();
		}
	}

	private static class ContainerConsolidateMaintenance extends InternalMaintenanceOperation {

		public ContainerConsolidateMaintenance() {
			super(containerOp.identifier, null, null, 1);
		}

		@Override
		public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			checkMissingData(ServerSideServiceProvider.defaultDataSource, monitor);
			ServerSideServiceProvider.mailboxDataSource.entrySet().forEach(pool -> {
				checkMissingData(pool.getValue(), monitor);
			});
			monitor.end();
		}

		@Override
		public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			repairMissingData(ServerSideServiceProvider.defaultDataSource, monitor);
			ServerSideServiceProvider.mailboxDataSource.entrySet().forEach(pool -> {
				repairMissingData(pool.getValue(), monitor);
			});
			monitor.end();
		}

		private void checkMissingData(DataSource pool, RepairTaskMonitor monitor) {
			ContainerStore cs = new ContainerStore(null, pool, SecurityContext.SYSTEM);
			try {
				Set<String> missingSeq = cs.getMissingContainerSequence();
				monitor.log("Missing container sequence for containers {}", Level.WARN, missingSeq);
				monitor.notify("Missing container sequence for containers {}", Level.WARN, missingSeq);
			} catch (SQLException e) {
				monitor.log(e.getMessage(), e);
			}

			try {
				Set<String> missingSettings = cs.getMissingContainerSettings();
				monitor.log("Missing container settings for containers {}", Level.WARN, missingSettings);
				monitor.notify("Missing container settings for containers {}", Level.WARN, missingSettings);
			} catch (SQLException e) {
				monitor.log(e.getMessage(), e);
			}
		}

		private void repairMissingData(DataSource pool, RepairTaskMonitor monitor) {
			ContainerStore cs = new ContainerStore(null, pool, SecurityContext.SYSTEM);
			try {
				monitor.log("Create missing container sequence");
				cs.createMissingContainerSequence();
			} catch (SQLException e) {
				monitor.log(e.getMessage(), e);
			}
			try {
				monitor.log("Create missing container settings");
				cs.createMissingContainerSettings();
			} catch (SQLException e) {
				monitor.log(e.getMessage(), e);
			}
		}

	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.DOMAIN) {
			return ImmutableSet.of(containerOp);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.DOMAIN) {
			return ImmutableSet.of(new ContainerConsolidateMaintenance());
		} else {
			return Collections.emptySet();
		}
	}

}
