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
package net.bluemind.exchange.mapi.service.internal.repair;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.exchange.mapi.api.IMapiFoldersMgmt;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.exchange.mapi.api.MapiFolder;
import net.bluemind.exchange.mapi.api.MapiFolderContainer;
import net.bluemind.exchange.mapi.api.MapiReplica;

public class MapiFoldersRepair implements IDirEntryRepairSupport {

	private static final Logger logger = LoggerFactory.getLogger(MapiFoldersRepair.class);
	public static final MaintenanceOperation mapiFoldersOp = MaintenanceOperation.create("mapi.folders",
			"Mapi Folders");

	public static class Factory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new MapiFoldersRepair(context);
		}
	}

	private final BmContext ctx;

	public MapiFoldersRepair(BmContext context) {
		this.ctx = context;
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.USER) {
			return ImmutableSet.of(mapiFoldersOp);
		}
		return Collections.emptySet();
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.USER) {
			return ImmutableSet.of(new MapiFoldersMaintenance(ctx));
		}
		return Collections.emptySet();
	}

	private static class MapiFoldersMaintenance extends InternalMaintenanceOperation {
		private final BmContext context;

		public MapiFoldersMaintenance(BmContext ctx) {
			super(mapiFoldersOp.identifier, null, null, 1);
			this.context = ctx;
		}

		@Override
		public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			verifyExtraFolders(domainUid, entry, report, monitor, (foldersApi, extra) -> {
				report.warn(mapiFoldersOp.identifier, "Folder " + extra.uid + " should be removed.");
			});
		}

		@Override
		public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			verifyExtraFolders(domainUid, entry, report, monitor, (foldersApi, extra) -> {
				monitor.log("Deleting " + extra.uid);
				foldersApi.delete(extra.uid);
			});
		}

		private void verifyExtraFolders(String domainUid, DirEntry entry, DiagnosticReport report,
				IServerTaskMonitor monitor, BiConsumer<IMapiFoldersMgmt, BaseContainerDescriptor> processExtra) {
			logger.info("Checking mapi folders of {}@{}", entry, domainUid);
			monitor.log("Checking folders of " + entry + "@" + domainUid);

			IMapiMailbox mboxApi = context.provider().instance(IMapiMailbox.class, domainUid, entry.entryUid);
			MapiReplica replica = mboxApi.get();
			if (replica == null) {
				report.warn(mapiFoldersOp.identifier, "Missing replica for " + entry);
				return;
			}
			monitor.log("Replica is " + replica.localReplicaGuid);

			IContainers contApi = context.provider().instance(IContainers.class);
			logger.info("Deleting all mapi folders of {} : {}", replica.mailboxUid, MapiFolderContainer.TYPE);
			List<BaseContainerDescriptor> all = contApi
					.allLight(ContainerQuery.ownerAndType(replica.mailboxUid, MapiFolderContainer.TYPE));
			IMapiFoldersMgmt foldersApi = context.provider().instance(IMapiFoldersMgmt.class, domainUid,
					entry.entryUid);
			monitor.begin(all.size(), "Working on " + all.size() + " container(s)");
			for (BaseContainerDescriptor c : all) {
				monitor.log("Checking container " + c.uid);
				String k = MapiFolderContainer.mapiKind(c.uid);
				if (!NonMapiFolder.legitKind(k)) {
					MapiFolder folder = foldersApi.get(c.uid);
					logger.warn("We should not have a folder for kind {} => {}", k, folder);
					processExtra.accept(foldersApi, c);
				}
				monitor.progress(1, c.uid + " handled.");
			}

			report.ok(mapiFoldersOp.identifier, "mapi folders checked.");
		}

	}

}
