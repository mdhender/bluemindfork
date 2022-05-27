/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.container.sharding.repair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.sql.DataSource;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;

public class OrphanContainerLocationRepair implements IDirEntryRepairSupport {
	public static final String REPAIR_OP_ID = "containers.sharding.orphan.location";

	private final BmContext context;

	public static final MaintenanceOperation containerOp = MaintenanceOperation
			.create(OrphanContainerLocationRepair.REPAIR_OP_ID, "check and remove orphan container locations");

	public static class Factory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new OrphanContainerLocationRepair(context);
		}
	}

	public OrphanContainerLocationRepair(BmContext context) {
		this.context = context;
	}

	private static class OrphanContainerLocationMaintenance extends InternalMaintenanceOperation {
		private final BmContext context;
		private final ContainerStore directoryContainerStore;

		public OrphanContainerLocationMaintenance(BmContext context) {
			super(containerOp.identifier, ContainerShardingRepair.REPAIR_OP_ID, null, 1);
			this.context = context;
			directoryContainerStore = new ContainerStore(context, context.getDataSource(),
					context.getSecurityContext());
		}

		public List<Consumer<Boolean>> getRepairActions(DirEntry dirEntry, RepairTaskMonitor monitor) {
			List<Consumer<Boolean>> ops = new ArrayList<>();
			HashSet<String> containerUids = new HashSet<>();
			for (DataSource ds : Iterables.concat(context.getAllMailboxDataSource(),
					Collections.singleton(context.getDataSource()))) {
				try (Connection conn = ds.getConnection()) {
					try (PreparedStatement stmt = conn.prepareStatement("SELECT uid FROM t_container")) {
						try (ResultSet res = stmt.executeQuery()) {
							if (res == null) {
								break;
							}
							while (res.next()) {
								containerUids.add(res.getString(1));
							}
						}
					}
				} catch (SQLException e) {
					monitor.notify("Unable to execute request on {}: {}", ds, e.getMessage());
				}
			}
			try (Connection conn = context.getDataSource().getConnection()) {
				try (PreparedStatement stmt = conn.prepareStatement("SELECT container_uid FROM t_container_location")) {
					try (ResultSet res = stmt.executeQuery()) {
						while (res.next()) {
							String containerUid = res.getString(1);
							if (!containerUids.contains(containerUid)) {
								ops.add(dry -> {
									monitor.notify(
											"Container uid {} does not exists on any database. Removing from t_container_location",
											containerUid);
									if (Boolean.FALSE.equals(dry)) {
										try {
											directoryContainerStore.deleteContainerLocation(containerUid);
										} catch (SQLException e) {
											monitor.notify("Unable to remove {} from t_container_location: {}",
													containerUid, e.getMessage());
										}
									}
								});
							}
						}
					}
				}
			} catch (SQLException e) {
				monitor.notify("Unable to execute request on {}: {}", context.getDataSource(), e.getMessage());
			}
			return ops;
		}

		@Override
		public void check(String domainUid, DirEntry dirEntry, RepairTaskMonitor monitor) {
			getRepairActions(dirEntry, monitor).forEach(op -> op.accept(true));
			monitor.end();
		}

		@Override
		public void repair(String domainUid, DirEntry dirEntry, RepairTaskMonitor monitor) {
			getRepairActions(dirEntry, monitor).forEach(op -> op.accept(false));
			monitor.end();
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
			return ImmutableSet.of(new OrphanContainerLocationMaintenance(context));
		} else {
			return Collections.emptySet();
		}
	}

	private boolean supportedKind(Kind kind) {
		return kind == Kind.DOMAIN;
	}
}
