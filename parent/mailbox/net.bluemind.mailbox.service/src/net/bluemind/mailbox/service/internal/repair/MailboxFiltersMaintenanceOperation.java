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

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;

public class MailboxFiltersMaintenanceOperation extends MailboxMaintenanceOperation {
	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxFilters.name();

	public MailboxFiltersMaintenanceOperation(BmContext context) {
		super(context, MAINTENANCE_OPERATION_ID);
	}

	@Override
	protected void checkMailbox(String domainUid, RepairTaskMonitor monitor) {
		monitor.end();
	}

	@Override
	protected void repairMailbox(String domainUid, RepairTaskMonitor monitor) {
		monitor.begin(1, String.format("Repair filters for mailbox %s", mailboxToString(domainUid)));

		try {
			ItemValue<Domain> domain = ServerSideServiceProvider.getProvider(context).instance(IDomains.class)
					.get(domainUid);

			MailFilter filters = ServerSideServiceProvider.getProvider(context).instance(IMailboxes.class, domainUid)
					.getMailboxFilter(mailbox.uid);

			MailboxesStorageFactory.getMailStorage().changeFilter(context, domain, mailbox, filters);

			monitor.progress(1, String.format("Mailbox %s filters repaired successfully", mailboxToString(domainUid)));
		} catch (Exception e) {
			String msg = String.format("Fail to repair mailbox %s filters: %s", mailboxToString(domainUid),
					e.getMessage());
			monitor.notify(msg);
		}

		monitor.end();
	}
}
