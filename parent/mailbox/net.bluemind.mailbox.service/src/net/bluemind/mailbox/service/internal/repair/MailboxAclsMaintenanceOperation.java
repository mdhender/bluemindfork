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

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.service.IMailboxesStorage.MailFolder;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;

public class MailboxAclsMaintenanceOperation extends MailboxMaintenanceOperation {
	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxAcls.name();

	public MailboxAclsMaintenanceOperation(BmContext context) {
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
		monitor.begin(1, String.format("Check mailbox %s acls", mailboxToString(domainUid)));

		List<AccessControlEntry> acls = ServerSideServiceProvider.getProvider(context)
				.instance(IMailboxes.class, domainUid).getMailboxAccessControlList(mailbox.uid);

		List<MailFolder> folders = MailboxesStorageFactory.getMailStorage().checkAndRepairAcl(context, domainUid,
				mailbox, acls, repair);

		boolean success = true;

		monitor.progress(1, String.format("Mailbox %s acls checked", mailboxToString(domainUid)));
		if (folders.size() == 0) {
			report.ok(MAINTENANCE_OPERATION_ID, String.format("Mailbox %s acls ok", mailboxToString(domainUid)));
		} else {
			for (MailFolder f : folders) {
				if (repair) {
					report.ok(MAINTENANCE_OPERATION_ID,
							String.format("Mailbox %s: %s imap acl fixed", mailboxToString(domainUid), f.name));
					monitor.log(String.format("%s imap acl fixed", f.name));
				} else {
					success = false;

					report.ko(MAINTENANCE_OPERATION_ID,
							String.format("Mailbox %s: %s imap acl must be fixed", mailboxToString(domainUid), f.name));
					monitor.log(String.format("%s imap acl must be fixed", f.name));
				}
			}
		}

		monitor.end(success, null, null);
	}
}
