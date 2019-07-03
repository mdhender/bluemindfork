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
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.mailbox.service.common.DefaultFolder.Status;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;

public class MailboxDefaultFoldersMaintenanceOperation extends MailboxMaintenanceOperation {
	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxDefaultFolders.name();

	public MailboxDefaultFoldersMaintenanceOperation(BmContext context) {
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
		monitor.begin(1, String.format("Check mailbox %s default folders", mailboxToString(domainUid)));

		Status status = MailboxesStorageFactory.getMailStorage().checkAndRepairDefaultFolders(context, domainUid,
				mailbox, repair);

		boolean success = true;

		monitor.progress(1, String.format("Mailbox %s default folders checked", mailboxToString(domainUid)));
		if (status.isOk()) {
			status.fixed.forEach(df -> monitor
					.log(String.format("Mailbox %s folder %s fixed", mailboxToString(domainUid), df.name)));
			report.ok(MAINTENANCE_OPERATION_ID,
					String.format("Mailbox %s default folders ok", mailboxToString(domainUid)));
		} else {
			status.missing.forEach(df -> monitor
					.log(String.format("Missing mailbox %s folder %s", mailboxToString(domainUid), df.name)));
			status.invalidSpecialuse.forEach(df -> monitor.log(
					String.format("Invalid xlist flag for mailbox %s folder %s", mailboxToString(domainUid), df.name)));

			report.ko(MAINTENANCE_OPERATION_ID,
					String.format("Mailbox %s default folders must be fixed", mailboxToString(domainUid)));
			success = false;
		}

		monitor.end(success, null, null);
	}
}
