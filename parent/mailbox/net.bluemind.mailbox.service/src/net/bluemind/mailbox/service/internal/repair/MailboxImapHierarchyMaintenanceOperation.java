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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.mailbox.service.IMailboxesStorage.MailFolder;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;

public class MailboxImapHierarchyMaintenanceOperation extends MailboxMaintenanceOperation {

	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxImapHierarchy.name();

	public MailboxImapHierarchyMaintenanceOperation(BmContext context) {
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
		monitor.begin(1, String.format("Check imap hierarchy for mailbox %s", mailboxToString(domainUid)));

		try {
			List<MailFolder> gaps = MailboxesStorageFactory.getMailStorage().checkAndRepairHierarchy(context, domainUid,
					mailbox, repair);

			monitor.progress(1, String.format("Mailbox %s imap hierarchy checked", mailboxToString(domainUid)));
			if (gaps.isEmpty()) {
				monitor.log(String.format("Mailbox %s imap hierarchy ok", mailboxToString(domainUid)));
			} else {
				for (MailFolder gap : gaps) {
					if (repair) {
						monitor.log(String.format("Imap folder %s was fixed", gap.name));
					} else {
						monitor.log(String.format("Imap folder %s was missing", gap.name));
					}
				}
			}
		} catch (ServerFault sf) {
			monitor.notify("Error on checking imap hierarchy for mailbox {}: {}", mailboxToString(domainUid),
					sf.getMessage());
		}

		monitor.end();
	}
}
