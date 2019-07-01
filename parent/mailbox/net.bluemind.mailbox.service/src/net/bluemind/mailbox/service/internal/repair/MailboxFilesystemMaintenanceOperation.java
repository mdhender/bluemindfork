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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;

public class MailboxFilesystemMaintenanceOperation extends MailboxMaintenanceOperation {
	private static final Logger logger = LoggerFactory.getLogger(MailboxFilesystemMaintenanceOperation.class);

	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxFilesystem.name();

	public MailboxFilesystemMaintenanceOperation(BmContext context) {
		super(context, MAINTENANCE_OPERATION_ID);
	}

	@Override
	protected void checkMailbox(String domainUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		checkAndRepair(domainUid, report, monitor);
	}

	@Override
	protected void repairMailbox(String domainUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		checkAndRepair(domainUid, report, monitor);
	}

	private void checkAndRepair(String domainUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		monitor.begin(1, String.format("Check mailbox filesystem for %s", mailboxToString(domainUid)));

		boolean success = true;
		try {
			MailboxesStorageFactory.getMailStorage().checkAndRepairFilesystem(context, domainUid, mailbox);

			monitor.progress(1, String.format("Mailbox %s filesystem checked", mailboxToString(domainUid)));
			report.ok(MAINTENANCE_OPERATION_ID, String.format("Mailbox %s filesystem ok", mailboxToString(domainUid)));
		} catch (ServerFault sf) {
			logger.error(String.format("Error on checking mailbox filesystem for %s: %s", mailboxToString(domainUid),
					sf.getMessage()), sf);

			report.ko(MAINTENANCE_OPERATION_ID, String.format("Error on checking mailbox filesystem for %s: %s",
					mailboxToString(domainUid), sf.getMessage()));
			monitor.end(false, null, null);
			throw sf;
		}

		monitor.end(success, null, null);
	}
}
