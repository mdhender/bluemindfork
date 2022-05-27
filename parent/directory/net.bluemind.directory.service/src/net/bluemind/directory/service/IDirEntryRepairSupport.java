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
package net.bluemind.directory.service;

import java.util.Set;

import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;

public interface IDirEntryRepairSupport {

	public interface Factory {
		IDirEntryRepairSupport create(BmContext context);
	}

	public Set<MaintenanceOperation> availableOperations(DirEntry.Kind kind);

	public Set<InternalMaintenanceOperation> ops(DirEntry.Kind kind);

	public static abstract class InternalMaintenanceOperation {
		/**
		 * @see MaintenanceOperation#identifier
		 */
		public final String identifier;
		public final String beforeOp;
		public final String afterOp;
		/**
		 * make progress tracking more precise
		 */
		public final int cost;

		public InternalMaintenanceOperation(String identifier, String beforeOp, String afterOp, int cost) {
			this.identifier = identifier;
			this.beforeOp = beforeOp;
			this.afterOp = afterOp;
			this.cost = cost;
		}

		@Override
		public final int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
			return result;
		}

		@Override
		public final boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			InternalMaintenanceOperation other = (InternalMaintenanceOperation) obj;
			if (identifier == null) {
				if (other.identifier != null)
					return false;
			} else if (!identifier.equals(other.identifier))
				return false;
			return true;
		}

		public abstract void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor);

		public abstract void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor);
	}
}
