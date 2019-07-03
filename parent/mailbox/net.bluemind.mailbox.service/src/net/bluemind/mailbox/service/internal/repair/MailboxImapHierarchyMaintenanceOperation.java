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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.mailbox.service.IMailboxesStorage.MailFolder;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;

public class MailboxImapHierarchyMaintenanceOperation extends MailboxMaintenanceOperation {
	private static final Logger logger = LoggerFactory.getLogger(MailboxImapHierarchyMaintenanceOperation.class);

	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxImapHierarchy.name();

	public MailboxImapHierarchyMaintenanceOperation(BmContext context) {
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
		monitor.begin(1, String.format("Check imap hierarchy for mailbox %s", mailboxToString(domainUid)));

		boolean success = true;
		try {
			List<MailFolder> gaps = MailboxesStorageFactory.getMailStorage().checkAndRepairHierarchy(context, domainUid,
					mailbox, repair);

			monitor.progress(1, String.format("Mailbox %s imap hierarchy checked", mailboxToString(domainUid)));
			if (gaps.size() == 0) {
				report.ok(MAINTENANCE_OPERATION_ID,
						String.format("Mailbox %s imap hierarchy ok", mailboxToString(domainUid)));
			} else {
				for (MailFolder gap : gaps) {
					if (repair) {
						report.ok(MAINTENANCE_OPERATION_ID, String.format("Imap folder %s was fixed", gap.name));
					} else {
						report.ko(MAINTENANCE_OPERATION_ID, String.format("Imap folder %s was missing", gap.name));
						success = false;
					}
				}
			}
		} catch (ServerFault sf) {
			logger.error(String.format("Error on checking imap hierarchy for mailbox %s: %s",
					mailboxToString(domainUid), sf.getMessage()), sf);

			report.ko(MAINTENANCE_OPERATION_ID, String.format("Error on checking imap hierarchy for mailbox %s: %s",
					mailboxToString(domainUid), sf.getMessage()));
			monitor.end(false, null, null);
			throw sf;
		}

		monitor.end(success, null, null);
	}
}
