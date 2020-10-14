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

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.service.IMailboxesStorage.CheckAndRepairStatus;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;

public class MailboxSharedSeenMaintenanceOperation extends MailboxMaintenanceOperation {
	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxSharedSeen.name();

	public MailboxSharedSeenMaintenanceOperation(BmContext context) {
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
		monitor.begin(1, String.format("%s mailbox %s sharedseen status", repair ? "Repair" : "Check",
				mailboxToString(domainUid)));

		if (mailbox.value.type == Type.user) {
			CheckAndRepairStatus status = MailboxesStorageFactory.getMailStorage().checkAndRepairSharedSeen(context,
					domainUid, mailbox, repair);

			monitor.end(status.broken == status.fixed, "sharedseen " + mailbox.value.name + "@" + domainUid
					+ " checked: " + status.checked + ", broken: " + status.broken + ", fixed: " + status.fixed, null);
		} else {
			monitor.end(true, "nothing to do for shared mailbox " + mailbox.value.name, null);
		}
	}
}
