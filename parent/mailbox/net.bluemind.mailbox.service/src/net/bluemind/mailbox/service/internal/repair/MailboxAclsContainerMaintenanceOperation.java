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
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.service.internal.MailboxesService;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;

public class MailboxAclsContainerMaintenanceOperation extends MailboxMaintenanceOperation {
	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxAclsContainer.name();

	public MailboxAclsContainerMaintenanceOperation(BmContext context) {
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
		monitor.begin(1, String.format("Check mailbox %s acls container exists", mailboxToString(domainUid)));
		IContainers contApi = context.su().provider().instance(IContainers.class);
		ContainerDescriptor exiting = contApi.getIfPresent(IMailboxAclUids.uidForMailbox(mailbox.uid));
		if (exiting != null) {
			monitor.progress(1, String.format("Mailbox %s acls container exists", mailboxToString(domainUid)));
			report.ok(MAINTENANCE_OPERATION_ID,
					String.format("Mailbox %s acls container exists", mailboxToString(domainUid)));
		} else {
			if (!repair) {
				monitor.progress(1,
						String.format("Mailbox %s acls container does not exists", mailboxToString(domainUid)));
				monitor.end(false, null, null);

				report.ko(MAINTENANCE_OPERATION_ID,
						String.format("Mailbox %s acls container does not exists", mailboxToString(domainUid)));
			} else {
				monitor.log(
						String.format("Mailbox %s acls container not found, creating it", mailboxToString(domainUid)));

				MailboxesService.Helper.createMailboxesAclsContainer(context, domainUid, mailbox.uid, mailbox.value);

				monitor.progress(1,
						String.format("Mailbox %s acls container repair finished", mailboxToString(domainUid)));
				report.ok(MAINTENANCE_OPERATION_ID,
						String.format("Mailbox %s acls container repair finished", mailboxToString(domainUid)));
			}
		}
		monitor.end(true, null, null);
	}
}
