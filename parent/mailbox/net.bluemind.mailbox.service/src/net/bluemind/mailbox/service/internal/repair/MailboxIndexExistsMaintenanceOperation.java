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
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;

public class MailboxIndexExistsMaintenanceOperation extends MailboxMaintenanceOperation {
	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxIndexExists.name();

	public MailboxIndexExistsMaintenanceOperation(BmContext context) {
		super(context, MAINTENANCE_OPERATION_ID);
	}

	@Override
	protected void checkMailbox(String domainUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		checkAndRepair(false, domainUid, report, monitor);
	}

	@Override
	protected void repairMailbox(String domainUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		checkAndRepair(true, domainUid, report, monitor);
	}

	private void checkAndRepair(boolean repair, String domainUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		monitor.begin(1, String.format("Check mailbox %s index exists in ES", mailboxToString(domainUid)));
		Optional<IMailIndexService> optIndexer = RecordIndexActivator.getIndexer();
		if (!optIndexer.isPresent()) {
			monitor.progress(1, "record indexer missing");
			monitor.end(false, null, null);
			report.ko(MAINTENANCE_OPERATION_ID, "record indexer missing");
			return;
		}
		IMailIndexService recIdx = optIndexer.get();

		if (!recIdx.checkMailbox(mailbox.uid)) {
			if (repair) {
				monitor.log(String.format("Mailbox %s index not found, creating it", mailboxToString(domainUid)));

				recIdx.repairMailbox(mailbox.uid, monitor.subWork(1));

				report.ok(MAINTENANCE_OPERATION_ID,
						String.format("Mailbox %s index repair finished", mailboxToString(domainUid)));
			} else {
				monitor.progress(1, String.format("Mailbox %s index does not exists", mailboxToString(domainUid)));
				monitor.end(false, null, null);

				report.ko(MAINTENANCE_OPERATION_ID,
						String.format("Mailbox %s index does not exists", mailboxToString(domainUid)));
				return;
			}
		} else {
			monitor.progress(1, String.format("Mailbox index %s exists", mailboxToString(domainUid)));
			report.ok(MAINTENANCE_OPERATION_ID, String.format("Mailbox index %s exists", mailboxToString(domainUid)));
		}

		monitor.end(true, null, null);
	}
}
