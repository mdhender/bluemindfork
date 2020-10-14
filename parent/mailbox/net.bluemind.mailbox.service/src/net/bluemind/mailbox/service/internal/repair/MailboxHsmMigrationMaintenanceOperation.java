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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.api.ISecurityToken;
import net.bluemind.backend.mail.replica.indexing.IDSet;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.hsm.api.IHSM;
import net.bluemind.hsm.api.Promote;
import net.bluemind.imap.Flag;
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

	private static final Logger logger = LoggerFactory.getLogger(MailboxHsmMigrationMaintenanceOperation.class);
	private static final String MAINTENANCE_OPERATION_ID = DiagnosticReportCheckId.mailboxHsm.name();

	public MailboxHsmMigrationMaintenanceOperation(BmContext context) {
		super(context, MAINTENANCE_OPERATION_ID, null, "replication.subtree");
	}

	public static final class WalkResult implements AutoCloseable {
		private final StoreClient sc;
		private final List<ListInfo> folders;
		private final ISecurityToken securityToken;
		private boolean upgradedToken = false;
		public final Sudo sudo;

		public WalkResult(StoreClient sc, Sudo sudo) {
			this.sc = sc;
			this.sudo = sudo;
			this.folders = new LinkedList<>();
			this.securityToken = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(ISecurityToken.class, sudo.context.getSessionId());
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

		public void upgradeToken() {
			securityToken.upgrade();
			upgradedToken = true;
		}

		public void close() {
			if (upgradedToken) {
				securityToken.destroy();
			}
		}
	}

	private static final class MailboxWalk {
		protected final ItemValue<Mailbox> mailbox;
		protected final String domainUid;
		protected final Server srv;

		private MailboxWalk(ItemValue<Mailbox> mailbox, String domainUid, Server srv) {
			this.srv = srv;
			this.mailbox = mailbox;
			this.domainUid = domainUid;
		}

		public WalkResult folders() {
			String login = mailbox.value.name + "@" + domainUid;
			Sudo sudo = new Sudo(mailbox.value.name, domainUid);
			StoreClient sc = new StoreClient(srv.address(), 1143, login, sudo.context.getSessionId());
			WalkResult wr = new WalkResult(sc, sudo);
			if (!sc.login()) {
				logger.error("Unable to login as {} on {}", login, mailbox.value.name);
			} else {
				sc.listAll().stream().forEach(wr::add);
			}
			return wr;
		}
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
		monitor.begin(1, String.format("Check mailbox %s HSM migration", mailboxToString(domainUid)));

		if (!hsmCompleted(domainUid)) {
			traverseFolders(mailbox, domainUid, monitor);
			if (repair) {
				markAsFinished(domainUid);
			}
		}

		monitor.progress(1, String.format("Mailbox %s HSM migration finished", mailboxToString(domainUid)));
		report.ok(MAINTENANCE_OPERATION_ID,
				String.format("Mailbox %s HSM migration finished", mailboxToString(domainUid)));
		monitor.end(true, null, null);
	}

	private void markAsFinished(String domainUid) {
		String dir = String.format("/var/spool/bm-hsm/snappy/user/%s/%s", domainUid, mailbox.uid);
		new File(dir).mkdirs();

		try {
			getMarkerFile(domainUid).createNewFile();
		} catch (Exception e) {
			logger.warn("Cannot create hsm marker file {}", getMarkerFile(domainUid).getAbsolutePath(), e);
		}
	}

	private boolean hsmCompleted(String domainUid) {
		return getMarkerFile(domainUid).exists();
	}

	private File getMarkerFile(String domain) {
		return new File(
				String.format("/var/spool/bm-hsm/snappy/user/%s/%s/hsm.promote.completed", domain, mailbox.uid));
	}

	private void traverseFolders(ItemValue<Mailbox> mailbox, String domainUid, IServerTaskMonitor monitor) {
		logger.info("Traversing folders of mailbox {} type {}, routing: {}", mailbox.displayName, mailbox.value.type,
				mailbox.value.routing);
		ItemValue<Server> server = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).getComplete(mailbox.value.dataLocation);
		if (mailbox.value.routing == Routing.internal && !mailbox.value.archived && !mailbox.value.type.sharedNs) {
			MailboxWalk mailboxwalk = new MailboxWalk(mailbox, domainUid, server.value);
			try (WalkResult wr = mailboxwalk.folders()) {
				IServerTaskMonitor foldersMonitor = monitor.subWork("folders", wr.folders.size());
				foldersMonitor.begin(wr.folders.size(),
						"Mailbox " + mailbox.displayName + ": found " + wr.folders.size() + " folders");
				wr.upgradeToken();
				for (ListInfo f : wr.folders) {
					foldersMonitor.progress(1, "Promote folder " + f.getName());
					logger.info("Promote folder {}", f.getName());
					promoteFolder(domainUid, monitor, wr, f, foldersMonitor);
				}
				foldersMonitor.end(true, null, null);
			}
		}
	}

	private void promoteFolder(String domainUid, IServerTaskMonitor monitor, WalkResult wr, ListInfo folder,
			IServerTaskMonitor foldersMonitor) {
		String folderName = folder.getName();
		StoreClient sc = wr.sc;
		try {
			sc.select(folderName);
		} catch (IMAPException e) {
			logger.warn("Cannot select folder {}", folderName, e);
			return;
		}
		SearchQuery sq = new SearchQuery();
		sq.setKeyword(Flag.BMARCHIVED.toString());
		Collection<Integer> archived = sc.uidSearch(sq);
		IDSet idset = IDSet.create(archived.iterator());

		logger.info("Found {} archived entries in folder {} ", archived.size(), folderName);
		monitor.log(archived.size() + " archived messages in folder " + folderName);

		if (!archived.isEmpty()) {
			IServerTaskMonitor messagesMonitor = foldersMonitor.subWork("messages", archived.size());
			messagesMonitor.begin(archived.size(), "promoting HSM messages in folder " + folderName);
			idset.forEach(idRange -> {
				logger.info("Promoting from {} to {}", idRange.from(), idRange.to());

				if (idRange.from() > 0) {
					String smallerRange = idRange.toString();
					Collection<Summary> imapSummaries = sc.uidFetchSummary(smallerRange);
					logger.info("Promoting {} summaries", imapSummaries.size());
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
		final SecurityContext hsmcontext = wr.sudo.context;
		ServerSideServiceProvider.getProvider(hsmcontext).instance(IHSM.class, domainUid).promoteMultiple(toPromote);
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
