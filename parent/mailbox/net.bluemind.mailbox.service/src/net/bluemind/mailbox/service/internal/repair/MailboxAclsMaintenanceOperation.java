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

import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.service.RepairTaskMonitor;
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
	protected void checkMailbox(String domainUid, RepairTaskMonitor monitor) {
		checkAndRepair(false, domainUid, monitor);
	}

	@Override
	protected void repairMailbox(String domainUid, RepairTaskMonitor monitor) {
		checkAndRepair(true, domainUid, monitor);
	}

	private void checkAndRepair(boolean repair, String domainUid, RepairTaskMonitor monitor) {
		monitor.begin(1, String.format("Check mailbox %s acls", mailboxToString(domainUid)));

		List<AccessControlEntry> acls = ServerSideServiceProvider.getProvider(context)
				.instance(IMailboxes.class, domainUid).getMailboxAccessControlList(mailbox.uid);

		List<MailFolder> folders = MailboxesStorageFactory.getMailStorage().checkAndRepairAcl(context, domainUid,
				mailbox, acls, repair);

		monitor.progress(1, String.format("Mailbox %s acls checked", mailboxToString(domainUid)));

		for (MailFolder f : folders) {
			if (repair) {
				monitor.log(String.format("%s imap acl fixed", f.name));
			}
		}

		monitor.end();
	}
}
