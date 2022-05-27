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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.service.internal.repair;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.mailbox.api.Mailbox;

public class RecordsInDirectoryRepair implements IDirEntryRepairSupport {

	public static class RepairFactory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new RecordsInDirectoryRepair(context);
		}
	}

	public static final MaintenanceOperation wrongDbOp = MaintenanceOperation
			.create(IMailReplicaUids.REPAIR_RECS_IN_DIR, "Record containers in wrong DB");

	private static class WrongLocationMaintenance extends MailboxFoldersRepairOp {

		public WrongLocationMaintenance(BmContext ctx) {
			super(ctx, wrongDbOp.identifier, null, IMailReplicaUids.REPAIR_SUBTREE_OP, 1);
		}

		@Override
		public void runOnFolders(boolean repair, RepairTaskMonitor monitor, String subTree, String domainUid,
				ItemValue<Mailbox> mbox, List<ItemValue<MailboxReplica>> fullList) {
			String subLoc = DataSourceRouter.location(context, subTree);
			IDbByContainerReplicatedMailboxes foldersApi = context.provider()
					.instance(IDbByContainerReplicatedMailboxes.class, subTree);
			List<ItemValue<MailboxReplica>> toPurge = new LinkedList<>();
			monitor.begin(1, "Inspecting subtree for mailbox " + mbox.value.name + "@" + domainUid);
			for (ItemValue<MailboxReplica> folder : fullList) {
				String recUid = IMailReplicaUids.mboxRecords(folder.uid);
				String recLoc = DataSourceRouter.location(context, recUid);
				if (recLoc == null || !recLoc.equals(subLoc)) {
					toPurge.add(folder);
				}
			}

			for (ItemValue<MailboxReplica> itemValue : toPurge) {
				monitor.log("Obsolete replica item {}", itemValue.uid);
				if (repair) {
					try {
						foldersApi.delete(itemValue.uid);
					} catch (Exception e) {
						monitor.log("Error deleting " + itemValue.uid + ": " + e.getMessage());
					}
				} else {
					monitor.log("Should purge " + itemValue.uid);
				}
			}
			monitor.end(true, toPurge.size() + " purged.", "{}");
		}

	}

	private final BmContext context;

	public RecordsInDirectoryRepair(BmContext context) {
		this.context = context;
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.USER || kind == Kind.MAILSHARE || kind == Kind.GROUP || kind == Kind.RESOURCE) {
			return ImmutableSet.of(wrongDbOp);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.USER || kind == Kind.MAILSHARE || kind == Kind.GROUP || kind == Kind.RESOURCE) {
			return ImmutableSet.of(new WrongLocationMaintenance(context));
		} else {
			return Collections.emptySet();
		}

	}
}
