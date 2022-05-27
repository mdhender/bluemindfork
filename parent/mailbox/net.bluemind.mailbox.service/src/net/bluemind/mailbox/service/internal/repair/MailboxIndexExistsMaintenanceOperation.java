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
package net.bluemind.mailbox.service.internal.repair;

import java.util.Optional;

import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;

public class MailboxIndexExistsMaintenanceOperation extends MailboxMaintenanceOperation {
	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxIndexExists.name();

	public MailboxIndexExistsMaintenanceOperation(BmContext context) {
		super(context, MAINTENANCE_OPERATION_ID);
	}

	@Override
	protected void checkMailbox(String domainUid, RepairTaskMonitor monitor) {
		checkAndRepair(false, domainUid, monitor);
	}

	@Override
	protected void repairMailbox(String domainUid, RepairTaskMonitor monitor) {
		checkAndRepair(true, domainUid, monitor);
	}

	private void checkAndRepair(boolean repair, String domainUid, RepairTaskMonitor monitor) {
		monitor.begin(1, String.format("Check mailbox %s index exists in ES", mailboxToString(domainUid)));
		Optional<IMailIndexService> optIndexer = RecordIndexActivator.getIndexer();
		if (!optIndexer.isPresent()) {
			monitor.progress(1, "record indexer missing");
			monitor.end(false, "record indexer missing", null);
			return;
		}
		IMailIndexService recIdx = optIndexer.get();

		if (!recIdx.checkMailbox(mailbox.uid)) {
			monitor.notify("Mailbox {} index not found", mailboxToString(domainUid));
			if (repair) {
				recIdx.repairMailbox(mailbox.uid, monitor.subWork(1));
			}
		}

		monitor.end();
	}
}
