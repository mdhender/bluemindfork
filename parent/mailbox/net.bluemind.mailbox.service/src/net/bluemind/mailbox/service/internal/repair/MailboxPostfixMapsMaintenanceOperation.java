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
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;
import net.bluemind.system.api.IMailDeliveryMgmt;

public class MailboxPostfixMapsMaintenanceOperation extends MailboxMaintenanceOperation {
	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxPostfixMaps.name();

	public MailboxPostfixMapsMaintenanceOperation(BmContext context) {
		super(context, MAINTENANCE_OPERATION_ID);
	}

	@Override
	protected void checkMailbox(String domainUid, DiagnosticReport report, IServerTaskMonitor monitor) {
	}

	@Override
	protected void repairMailbox(String domainUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		monitor.begin(1, String.format("Check mailbox %s postfix maps", mailboxToString(domainUid)));

		TaskRef taskRef = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailDeliveryMgmt.class).repair();

		ITask taskApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, taskRef.id);
		TaskUtils.forwardProgress(taskApi, monitor.subWork(1));

		report.ok(MAINTENANCE_OPERATION_ID,
				String.format("Postfix maps repaired for mailbox %s", mailboxToString(domainUid)));

		monitor.end(true, null, null);
	}
}
