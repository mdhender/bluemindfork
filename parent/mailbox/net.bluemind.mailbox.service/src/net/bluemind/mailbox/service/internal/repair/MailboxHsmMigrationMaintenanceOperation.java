/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
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

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.bluemind.backend.mail.replica.indexing.IDSet;
import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.hsm.api.IHSM;
import net.bluemind.hsm.api.Promote;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.Summary;
import net.bluemind.index.mail.Sudo;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.service.internal.repair.MailboxRepairSupport.MailboxMaintenanceOperation;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class MailboxHsmMigrationMaintenanceOperation extends MailboxMaintenanceOperation {

	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxHsm.name();

	public MailboxHsmMigrationMaintenanceOperation(BmContext context) {
		super(context, MAINTENANCE_OPERATION_ID, null, "replication.subtree");
	}

	public static final class WalkResult implements AutoCloseable {
		private final StoreClient sc;
		private final List<ListInfo> folders;
		public final Supplier<Sudo> sudo;

		public WalkResult(StoreClient sc, Supplier<Sudo> sudo) {
			this.sc = sc;
			this.sudo = sudo;
			this.folders = new LinkedList<>();
		}

		public boolean add(ListInfo folder) {
			String fn = folder.getName();
			if (!folder.isSelectable() || fn.startsWith("Dossiers partagés/")
					|| fn.startsWith("Autres utilisateurs/")) {
				return false;
			}
			this.folders.add(folder);
			return true;
		}

		public void close() {
			sc.close();
		}
	}

	private static final class MailboxWalk {
		protected final ItemValue<Mailbox> mailbox;
		protected final String domainUid;
		protected final Server srv;
		private final RepairTaskMonitor monitor;

		private MailboxWalk(ItemValue<Mailbox> mailbox, String domainUid, Server srv, RepairTaskMonitor monitor) {
			this.srv = srv;
			this.mailbox = mailbox;
			this.domainUid = domainUid;
			this.monitor = monitor;
		}

		public WalkResult folders() {
			String login = mailbox.value.name + "@" + domainUid;
			try (Sudo sudo = new Sudo(mailbox.value.name, domainUid);
					StoreClient sc = new StoreClient(srv.address(), 1143, login, sudo.context.getSessionId())) {
				WalkResult wr = new WalkResult(sc, () -> new Sudo(mailbox.value.name, domainUid));

				if (!sc.login()) {
					monitor.log("Unable to login as " + login + " on " + mailbox.value.name);
				} else {
					sc.listAll().stream().forEach(wr::add);
				}

				return wr;
			}
		}
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
		monitor.begin(1, String.format("Check mailbox %s HSM migration", mailboxToString(domainUid)));

		if (!hsmCompleted(domainUid)) {
			traverseFolders(mailbox, domainUid, monitor);
			if (repair) {
				markAsFinished(domainUid, monitor);
			}
		}

		monitor.progress(1, String.format("Mailbox %s HSM migration finished", mailboxToString(domainUid)));
		monitor.end();
	}

	private void markAsFinished(String domainUid, RepairTaskMonitor monitor) {
		String dir = String.format("/var/spool/bm-hsm/snappy/user/%s/%s", domainUid, mailbox.uid);
		new File(dir).mkdirs();

		try {
			getMarkerFile(domainUid).createNewFile();
		} catch (Exception e) {
			monitor.notify("Cannot create hsm marker file {}", getMarkerFile(domainUid).getAbsolutePath());
		}
	}

	private boolean hsmCompleted(String domainUid) {
		return getMarkerFile(domainUid).exists();
	}

	private File getMarkerFile(String domain) {
		return new File(
				String.format("/var/spool/bm-hsm/snappy/user/%s/%s/hsm.promote.completed", domain, mailbox.uid));
	}

	private void traverseFolders(ItemValue<Mailbox> mailbox, String domainUid, RepairTaskMonitor monitor) {
		monitor.log("Traversing folders of mailbox {} type {}, routing: {}", mailbox.displayName, mailbox.value.type,
				mailbox.value.routing);
		ItemValue<Server> server = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).getComplete(mailbox.value.dataLocation);
		if (mailbox.value.routing == Routing.internal && !mailbox.value.archived && !mailbox.value.type.sharedNs) {
			MailboxWalk mailboxwalk = new MailboxWalk(mailbox, domainUid, server.value, monitor);
			try (WalkResult wr = mailboxwalk.folders()) {
				RepairTaskMonitor foldersMonitor = (RepairTaskMonitor) monitor.subWork("folders", wr.folders.size());
				foldersMonitor.begin(wr.folders.size(),
						"Mailbox " + mailbox.displayName + ": found " + wr.folders.size() + " folders");
				for (ListInfo f : wr.folders) {
					foldersMonitor.progress(1, "Promote folder " + f.getName());
					monitor.log("Promoting folder {}", f.getName());
					promoteFolder(domainUid, monitor, wr, f, foldersMonitor);
				}
				foldersMonitor.end(true, null, null);
			}
		}
	}

	private void promoteFolder(String domainUid, IServerTaskMonitor monitor, WalkResult wr, ListInfo folder,
			RepairTaskMonitor foldersMonitor) {
		String folderName = folder.getName();
		StoreClient sc = wr.sc;
		try {
			sc.select(folderName);
		} catch (IMAPException e) {
			foldersMonitor.notify("Cannot select folder {}:{}", folderName, e.getMessage());
			return;
		}
		SearchQuery sq = new SearchQuery();
		sq.getHeaders().put("X-BM_HSM_ID", "");
		Collection<Integer> archived = sc.uidSearch(sq);
		IDSet idset = IDSet.create(archived.iterator(), 100);

		foldersMonitor.log("Found {} archived entries in folder {} ", archived.size(), folderName);
		monitor.log(archived.size() + " archived messages in folder " + folderName);

		if (!archived.isEmpty()) {
			IServerTaskMonitor messagesMonitor = foldersMonitor.subWork("messages", archived.size());
			messagesMonitor.begin(archived.size(), "promoting HSM messages in folder " + folderName);
			idset.forEach(idRange -> {
				monitor.log("Promoting from {} to {}", idRange.from(), idRange.to());

				if (idRange.from() > 0) {
					String smallerRange = idRange.toString();
					Collection<Summary> imapSummaries = sc.uidFetchSummary(smallerRange);
					messagesMonitor.log("Promoting {} summaries", imapSummaries.size());
					promoteSummaries(domainUid, mailbox, folder, wr, imapSummaries);
					messagesMonitor.progress(imapSummaries.size(), "Promoted " + imapSummaries.size() + " messages");
				}
			});
			messagesMonitor.end(true, null, null);
		}
	}

	private void promoteSummaries(String domainUid, ItemValue<Mailbox> mailbox, ListInfo folder, WalkResult wr,
			Collection<Summary> imapSummaries) {
		List<Promote> toPromote = imapSummaries.stream().map(s -> summaryToPromote(s, folder, mailbox.uid))
				.collect(Collectors.toList());
		try (Sudo sudo = wr.sudo.get()) {
			ServerSideServiceProvider.getProvider(sudo.context).instance(IHSM.class, domainUid)
					.promoteMultiple(toPromote);
		}
	}

	private Promote summaryToPromote(Summary sum, ListInfo folder, String mailboxUid) {
		final Promote promote = new Promote();
		promote.folder = folder.getName();
		promote.imapUid = sum.getUid();
		promote.mailboxUid = mailboxUid;
		promote.hsmId = sum.getHeaders().getRawHeader("X-BM_HSM_ID");
		promote.internalDate = sum.getDate();
		promote.flags = sum.getFlags().asTags();
		return promote;
	}
}
