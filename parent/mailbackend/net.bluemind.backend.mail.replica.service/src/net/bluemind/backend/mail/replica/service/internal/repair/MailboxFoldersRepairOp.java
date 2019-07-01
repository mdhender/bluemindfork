/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.mail.replica.service.internal.repair;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.IDirEntryRepairSupport.InternalMaintenanceOperation;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public abstract class MailboxFoldersRepairOp extends InternalMaintenanceOperation {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final BmContext context;

	protected MailboxFoldersRepairOp(BmContext ctx, String identifier, String beforeOp, String afterOp, int cost) {
		super(identifier, beforeOp, afterOp, cost);
		this.context = ctx;
	}

	@Override
	public final void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
		run(false, domainUid, entry, report, monitor);
	}

	@Override
	public final void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
		run(true, domainUid, entry, report, monitor);
	}

	public void run(boolean repair, String domainUid, DirEntry entry, DiagnosticReport report,
			IServerTaskMonitor monitor) {
		logger.info("Repair subtree {} {}", domainUid, entry);

		IMailboxes mboxApi = context.provider().instance(IMailboxes.class, domainUid);
		ItemValue<Mailbox> mbox = mboxApi.getComplete(entry.entryUid);
		if (mbox == null) {
			logger.warn("{} does not have a mailbox, nothing to repair", entry);
			return;
		}
		if (mbox.value.dataLocation == null) {
			logger.error("{} lacks a dataLocation, can't repair", mbox);
			return;
		}
		String subUid = IMailReplicaUids.subtreeUid(domainUid, mbox);
		IDbByContainerReplicatedMailboxes foldersApi = context.provider()
				.instance(IDbByContainerReplicatedMailboxes.class, subUid);

		List<ItemValue<MailboxReplica>> fullList = foldersApi.allReplicas();
		runOnFolders(repair, monitor, report, subUid, domainUid, mbox, fullList);
	}

	protected abstract void runOnFolders(boolean repair, IServerTaskMonitor mon, DiagnosticReport report,
			String subTree, String domainUid, ItemValue<Mailbox> mbox, List<ItemValue<MailboxReplica>> fullList);
}
