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
package net.bluemind.mailbox.service.internal.repair;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.StoreClient;
import net.bluemind.index.mail.Sudo;
import net.bluemind.mailbox.service.IMailboxesStorage.MailFolder;
import net.bluemind.mailbox.service.MailboxesStorageFactory;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class AltFoldersMaintenanceOperation extends MailboxMaintenanceOperation {

	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.altFolders.name();

	private static final String INBOX = "INBOX";
	private static final String ALT_FOLDERS_INBOX = "Alt Folders/INBOX";

	public AltFoldersMaintenanceOperation(BmContext context) {
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

		String mailboxName = mailboxToString(domainUid);

		monitor.begin(1, String.format("Check '%s' for %s ", ALT_FOLDERS_INBOX, mailboxName));

		List<MailFolder> folders = MailboxesStorageFactory.getMailStorage().listFolders(context, domainUid, mailbox);
		Optional<MailFolder> altInbox = folders.stream().filter(folder -> ALT_FOLDERS_INBOX.equals(folder.name))
				.findFirst();

		if (!altInbox.isPresent()) {
			monitor.end(true, String.format("No '%s'. Nothing to do for %s", ALT_FOLDERS_INBOX, mailboxName), "");
			return;
		}

		ItemValue<Server> server = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).getComplete(mailbox.value.dataLocation);
		try (Sudo pass = new Sudo(mailbox.value.name, domainUid);
				StoreClient sc = new StoreClient(server.value.address(), 1143, mailbox.value.name + "@" + domainUid,
						pass.context.getSessionId())) {

			if (!sc.login()) {
				monitor.end(true, "Failed to login", "");
				return;
			}

			MailFolder folder = altInbox.get();
			if (!sc.select(folder.name)) {
				monitor.end(true, String.format("No '%s' for %s", ALT_FOLDERS_INBOX, mailboxName), "");
				return;
			}

			Collection<Integer> content = sc.uidSearch(new SearchQuery());

			if (content.isEmpty()) {
				monitor.end(true, String.format("Nothing to do for %s", mailboxName), "");
				return;
			}

			if (repair) {
				monitor.progress(1,
						String.format("'%s' has %d emails. Copy to %s", ALT_FOLDERS_INBOX, content.size(), INBOX));
				sc.uidCopy(content, INBOX);

				monitor.progress(1, String.format("remove '%s'", ALT_FOLDERS_INBOX));
				sc.select(INBOX);
				sc.deleteMailbox(folder.name);

			} else {
				monitor.end(true, String.format("%s needs repair", mailboxName), "");
			}
		} catch (IMAPException e) {
			monitor.end(false, e.getMessage(), "");

		}

	}
}
