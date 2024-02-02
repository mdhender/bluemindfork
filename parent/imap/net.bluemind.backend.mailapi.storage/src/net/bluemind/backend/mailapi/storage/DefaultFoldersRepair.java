/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.backend.mailapi.storage;

import java.util.Set;

import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.service.IMailboxesStorage;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;

public class DefaultFoldersRepair implements IDirEntryRepairSupport {
	public static final MaintenanceOperation foldersRepair = MaintenanceOperation.create(
			MailboxMaintenanceOperation.DiagnosticReportCheckId.mailboxDefaultFolders.name(),
			"Ensure default folders (eg. Trash, Templates) exists");
	private final BmContext context;

	public DefaultFoldersRepair(BmContext context) {
		this.context = context;
	}

	public class DefaultFoldersRepairImpl extends InternalMaintenanceOperation {

		public DefaultFoldersRepairImpl() {
			super(foldersRepair.identifier, null,
					MailboxMaintenanceOperation.DiagnosticReportCheckId.mailboxExists.name(), 1);
		}

		@Override
		public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			// NOOP
		}

		@Override
		public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			monitor.begin(1, "Check folders of %s exists in mail store".formatted(entry.email));

			IMailboxesStorage storage = MailboxesStorageFactory.getMailStorage();
			if (storage instanceof MailApiBoxStorage apiStorage) {
				IMailboxes mboxApi = context.su().provider().instance(IMailboxes.class, domainUid);
				ItemValue<Mailbox> mbox = mboxApi.getComplete(entry.entryUid);
				String subtree = IMailReplicaUids.subtreeUid(domainUid, entry);
				IDbByContainerReplicatedMailboxes folders = context.su().provider()
						.instance(IDbByContainerReplicatedMailboxes.class, subtree);
				apiStorage.ensureDefaultFolders(domainUid, mbox, folders);
				monitor.progress(1, "Folders of %s repaired".formatted(entry.entryUid));
			}
			monitor.end();
		}

	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		return kind.hasMailbox() ? Set.of(foldersRepair) : Set.of();
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		return kind.hasMailbox() ? Set.of(new DefaultFoldersRepairImpl()) : Set.of();
	}

	public static class Factory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new DefaultFoldersRepair(context);
		}
	}

}
