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
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesRootMgmt;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class MailboxSubtreesRepair implements IDirEntryRepairSupport {

	public static class RepairFactory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new MailboxSubtreesRepair(context);
		}
	}

	public static final MaintenanceOperation repairSubtree = MaintenanceOperation
			.create(IMailReplicaUids.REPAIR_SUBTREE_OP, "Check replication subtree container presence");

	private static class MailboxSubtreesMaintenance extends InternalMaintenanceOperation {

		private final BmContext context;

		public MailboxSubtreesMaintenance(BmContext ctx) {
			super(repairSubtree.identifier, null, "mailboxDefaultFolders", 1);
			this.context = ctx;
		}

		@Override
		public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			monitor.log("Check subtree {} {}", domainUid, entry);
			monitor.end();
		}

		@Override
		public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			monitor.log("Repair subtree {} {}", domainUid, entry);

			IMailboxes mboxApi = context.provider().instance(IMailboxes.class, domainUid);
			ItemValue<Mailbox> mbox = mboxApi.getComplete(entry.entryUid);
			if (mbox == null) {
				monitor.log("{} does not have a mailbox, nothing to repair", entry);
				return;
			}
			if (mbox.value.dataLocation == null) {
				monitor.notify("{} lacks a dataLocation, can't repair", mbox);
				return;
			}
			monitor.begin(1, "Repairing subtree for mailbox " + mbox.value.name + "@" + domainUid);
			CyrusPartition cyrusPartition = CyrusPartition.forServerAndDomain(mbox.value.dataLocation, domainUid);
			IReplicatedMailboxesRootMgmt subtreeMgmt = context.provider().instance(IReplicatedMailboxesRootMgmt.class,
					cyrusPartition.name);
			MailboxReplicaRootDescriptor descriptor = MailboxReplicaRootDescriptor.create(mbox.value);
			subtreeMgmt.create(descriptor);
			monitor.progress(1, "Subtree " + cyrusPartition + " / " + descriptor + " repaired.");
			monitor.end();
		}

	}

	private final BmContext context;

	public MailboxSubtreesRepair(BmContext context) {
		this.context = context;
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.USER || kind == Kind.MAILSHARE || kind == Kind.GROUP || kind == Kind.RESOURCE) {
			return ImmutableSet.of(repairSubtree);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.USER || kind == Kind.MAILSHARE || kind == Kind.GROUP || kind == Kind.RESOURCE) {
			return ImmutableSet.of(new MailboxSubtreesMaintenance(context));
		} else {
			return Collections.emptySet();
		}

	}
}
