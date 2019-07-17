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
package net.bluemind.backend.mail.replica.service.internal.repair;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.config.Token;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.index.mail.Sudo;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class ReplicationParentUidRepair implements IDirEntryRepairSupport {

	private static final Logger logger = LoggerFactory.getLogger(ReplicationParentUidRepair.class);

	public static final MaintenanceOperation op = MaintenanceOperation.create("replication.parentUid",
			"Triggers cyrus replication on every IMAP folder");

	private final BmContext context;

	public ReplicationParentUidRepair(BmContext context) {
		this.context = context;
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.USER || kind == Kind.MAILSHARE) {
			return ImmutableSet.of(op);
		}
		return Collections.emptySet();
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.USER || kind == Kind.MAILSHARE) {
			return ImmutableSet.of(new ReplicationParentUidMaintenance(context));
		}
		return Collections.emptySet();

	}

	public static class RepairFactory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new ReplicationParentUidRepair(context);
		}
	}

	private static abstract class MailboxWalk {
		protected final ItemValue<Mailbox> mbox;
		protected final String domainUid;
		protected final BmContext context;
		protected final Server srv;

		private MailboxWalk(BmContext context, ItemValue<Mailbox> mbox, String domainUid, Server srv) {
			this.srv = srv;
			this.context = context;
			this.mbox = mbox;
			this.domainUid = domainUid;
		}

		public static MailboxWalk create(BmContext context, ItemValue<Mailbox> mbox, String domainUid, Server srv) {
			if (mbox.value.type.sharedNs) {
				return new SharedMailboxWalk(context, mbox, domainUid, srv);
			} else {
				return new UserMailboxWalk(context, mbox, domainUid, srv);
			}
		}

		public abstract void folders(BiConsumer<StoreClient, List<ListInfo>> process);
	}

	public static final class UserMailboxWalk extends MailboxWalk {

		public UserMailboxWalk(BmContext context, ItemValue<Mailbox> mbox, String domainUid, Server srv) {
			super(context, mbox, domainUid, srv);
		}

		public void folders(BiConsumer<StoreClient, List<ListInfo>> process) {
			String login = mbox.value.name + "@" + domainUid;

			try (Sudo sudo = new Sudo(mbox.value.name, domainUid);
					StoreClient sc = new StoreClient(srv.address(), 1143, login, sudo.context.getSessionId())) {
				if (!sc.login()) {
					logger.error("Fail to connect", mbox.value.name);
					return;
				}
				ListResult allFolders = sc.listAll();
				process.accept(sc, allFolders);
			}
		}
	}

	public static final class SharedMailboxWalk extends MailboxWalk {

		public SharedMailboxWalk(BmContext context, ItemValue<Mailbox> mbox, String domainUid, Server srv) {
			super(context, mbox, domainUid, srv);
		}

		public void folders(BiConsumer<StoreClient, List<ListInfo>> process) {
			try (StoreClient sc = new StoreClient(srv.address(), 1143, "admin0", Token.admin0())) {
				if (!sc.login()) {
					logger.error("Fail to connect", mbox.value.name);
					return;
				}
				List<ListInfo> mboxFoldersWithRoot = new LinkedList<>();
				ListInfo root = new ListInfo(mbox.value.name + "@" + domainUid, true);
				mboxFoldersWithRoot.add(root);
				ListResult shareChildren = sc.listSubFoldersMailbox(mbox.value.name + "@" + domainUid);
				mboxFoldersWithRoot.addAll(shareChildren);
				process.accept(sc, mboxFoldersWithRoot);
			}
		}
	}

	private static class ReplicationParentUidMaintenance extends InternalMaintenanceOperation {

		private final BmContext context;

		public ReplicationParentUidMaintenance(BmContext ctx) {
			super(op.identifier, null, IMailReplicaUids.REPAIR_SUBTREE_OP, 1);
			this.context = ctx;
		}

		@Override
		public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			if (entry.archived) {
				return;
			}

			logger.info("Check replication parentUid {} {}", domainUid, entry);
		}

		@Override
		public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {

			if (entry.archived) {
				logger.info("DirEntry is archived, skip it");
				return;
			}

			logger.info("Repair replication parentUid {} {}", domainUid, entry);

			IMailboxes iMailboxes = context.getServiceProvider().instance(IMailboxes.class, domainUid);
			ItemValue<Mailbox> mbox = iMailboxes.getComplete(entry.entryUid);

			IServer iServer = context.getServiceProvider().instance(IServer.class, "default");
			ItemValue<Server> server = iServer.getComplete(entry.dataLocation);

			MailboxWalk moonWalk = MailboxWalk.create(context, mbox, domainUid, server.value);

			byte[] eml = ("From: noreply@" + domainUid + "\r\n").getBytes(StandardCharsets.US_ASCII);
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			fl.add(Flag.SEEN);

			moonWalk.folders((sc, allFolders) -> {
				for (ListInfo f : allFolders) {
					String fn = f.getName();

					if (!f.isSelectable() || fn.startsWith("Dossiers partagés/")
							|| fn.startsWith("Autres utilisateurs/")) {
						continue;
					}
					try {
						sc.select(fn);
					} catch (IMAPException e) {
						logger.info("Fail to select {} on mailbox {}", fn, mbox.value.name);
					}
					sc.append(fn, new ByteArrayInputStream(eml), fl,
							new GregorianCalendar(1970, Calendar.JANUARY, 1).getTime());
					sc.expunge();
				}
			});

		}
	}

}
