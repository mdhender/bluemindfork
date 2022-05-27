/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.directory.hollow.datamodel.producer.impl;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.hollow.datamodel.producer.DirectorySerializer;
import net.bluemind.directory.hollow.datamodel.producer.Serializers;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;

public class HollowDirectoryRepair implements IDirEntryRepairSupport {
	public static final MaintenanceOperation hollowRepair = MaintenanceOperation.create("hollow.directory",
			"Ensure Hollow copy of the directory is in sync");

	public HollowDirectoryRepair(BmContext context) {
	}

	public static class RepairFactory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new HollowDirectoryRepair(context);
		}
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.DOMAIN) {
			return ImmutableSet.of(hollowRepair);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.DOMAIN) {
			return ImmutableSet.of(new HollowDirRepairImpl());
		} else {
			return Collections.emptySet();
		}
	}

	private static class HollowDirRepairImpl extends InternalMaintenanceOperation {

		public HollowDirRepairImpl() {
			super(hollowRepair.identifier, null, null, 1);
		}

		@Override
		public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			monitor.end();
		}

		@Override
		public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			DirectorySerializer serializer = Serializers.forDomain(domainUid);
			monitor.begin(1, "Repairing " + domainUid + " hollow directory");
			// this will force production from a changeset(0L)
			DomainVersions.get().invalidate(domainUid);
			if (serializer != null) {
				serializer.remove();
			} else {
				serializer = new DirectorySerializer(domainUid);
				serializer.remove();
				Serializers.put(domainUid, serializer);
			}
			serializer.init();
			serializer.produce();

			monitor.end(true, "Hollow dir for " + domainUid + " refreshed.", null);
		}

	}

}
