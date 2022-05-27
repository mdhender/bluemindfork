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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;

public class MailboxQuotaMaintenanceOperation extends MailboxMaintenanceOperation {

	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxQuota.name();

	public MailboxQuotaMaintenanceOperation(BmContext context) {
		super(context, MAINTENANCE_OPERATION_ID);
	}

	@Override
	protected void checkMailbox(String domainUid, RepairTaskMonitor monitor) {
		checkAndRepair(domainUid, monitor);
	}

	@Override
	protected void repairMailbox(String domainUid, RepairTaskMonitor monitor) {
		checkAndRepair(domainUid, monitor);
	}

	private void checkAndRepair(String domainUid, RepairTaskMonitor monitor) {
		monitor.begin(1, String.format("Check mailbox quota for %s", mailboxToString(domainUid)));

		try {
			MailboxesStorageFactory.getMailStorage().checkAndRepairQuota(context, domainUid, mailbox);
		} catch (ServerFault sf) {
			monitor.notify("Error on checking mailbox quota for {}: {}", mailboxToString(domainUid), sf.getMessage());
		}

		monitor.end();
	}
}
