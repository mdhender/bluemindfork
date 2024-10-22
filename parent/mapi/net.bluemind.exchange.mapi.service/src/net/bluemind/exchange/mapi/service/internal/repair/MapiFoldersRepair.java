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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.exchange.mapi.api.IMapiFolder;
import net.bluemind.exchange.mapi.api.IMapiFoldersMgmt;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.exchange.mapi.api.MapiFolder;
import net.bluemind.exchange.mapi.api.MapiFolderContainer;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class MapiFoldersRepair implements IDirEntryRepairSupport {

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
		if (kind == Kind.USER || kind == Kind.MAILSHARE) {
			return ImmutableSet.of(mapiFoldersOp);
		}
		return Collections.emptySet();
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.USER || kind == Kind.MAILSHARE) {
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

		private interface FolderRepairAction {
			void extraFolder(IMapiFoldersMgmt foldersApi, BaseContainerDescriptor c);

			void extraContent(IMapiFolder contentApi, BaseContainerDescriptor c);
		}

		@Override
		public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			verifyExtraFolders(domainUid, entry, monitor, new FolderRepairAction() {

				@Override
				public void extraFolder(IMapiFoldersMgmt foldersApi, BaseContainerDescriptor extra) {
				}

				@Override
				public void extraContent(IMapiFolder contentApi, BaseContainerDescriptor extra) {
				}
			});
			monitor.end();
		}

		@Override
		public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			verifyExtraFolders(domainUid, entry, monitor, new FolderRepairAction() {

				@Override
				public void extraFolder(IMapiFoldersMgmt foldersApi, BaseContainerDescriptor extra) {
					foldersApi.delete(extra.uid);
				}

				@Override
				public void extraContent(IMapiFolder contentApi, BaseContainerDescriptor c) {
					List<Long> allIds = contentApi.changesetById(0L).created;
					if (!allIds.isEmpty()) {
						Lists.partition(allIds, 50).forEach(contentApi::multipleDeleteById);
					}
				}
			});
			Producer prod = MQ.getProducer(Topic.MAPI_REPAIRS);
			ItemValue<Mailbox> mb = context.provider().instance(IMailboxes.class, domainUid)
					.getComplete(entry.entryUid);
			String latd = mb.value.name + "@" + domainUid;
			prod.send(new JsonObject().put("owner", latd));
			prod.close();
			monitor.end();
		}

		private void verifyExtraFolders(String domainUid, DirEntry entry, RepairTaskMonitor monitor,
				FolderRepairAction processExtra) {
			monitor.log("Checking mapi folders of " + entry + "@" + domainUid);

			IMapiMailbox mboxApi = context.provider().instance(IMapiMailbox.class, domainUid, entry.entryUid);
			MapiReplica replica = mboxApi.get();
			if (replica == null) {
				monitor.log(String.format("No replica found. Skip %s", mapiFoldersOp.identifier));
				return;
			}
			monitor.log("Replica is " + replica.localReplicaGuid);

			IContainers contApi = context.provider().instance(IContainers.class);
			monitor.log("Deleting all mapi folders of {} : {}", replica.mailboxUid, MapiFolderContainer.TYPE);
			List<BaseContainerDescriptor> all = contApi
					.allLight(ContainerQuery.ownerAndType(replica.mailboxUid, MapiFolderContainer.TYPE));
			IMapiFoldersMgmt foldersApi = context.provider().instance(IMapiFoldersMgmt.class, domainUid,
					entry.entryUid);
			monitor.begin(all.size(), "Working on " + all.size() + " container(s)");
			for (BaseContainerDescriptor c : all) {
				monitor.log("Checking container " + c.uid);
				String k = MapiFolderContainer.mapiKind(c.uid);
				if (k.equals("DEFERRED_ACTION")) {
					MapiFolder folder = foldersApi.get(c.uid);
					IMapiFolder contentApi = context.provider().instance(IMapiFolder.class, c.uid);
					monitor.notify("Extra content found: {} => {}", k, folder);
					processExtra.extraContent(contentApi, c);
				}
				if (!NonMapiFolder.legitKind(k, entry.kind)) {
					MapiFolder folder = foldersApi.get(c.uid);
					monitor.notify("We should not have a folder for kind {} => {}", k, folder);
					processExtra.extraFolder(foldersApi, c);
				}

				monitor.progress(1, c.uid + " handled.");
			}

		}

	}

}
