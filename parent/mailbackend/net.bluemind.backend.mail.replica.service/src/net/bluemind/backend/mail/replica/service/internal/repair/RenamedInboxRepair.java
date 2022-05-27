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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import com.google.common.collect.ImmutableSet;

import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.index.mail.Sudo;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;

public class RenamedInboxRepair implements IDirEntryRepairSupport {

	public static final String BROKEN_NAME = "Messages reçus";

	public static final MaintenanceOperation op = MaintenanceOperation.create(IMailReplicaUids.REPAIR_RENAMED_INBOX_OP,
			"Fixes mailboxes with a '" + BROKEN_NAME + "' folder");

	private final BmContext context;

	public RenamedInboxRepair(BmContext context) {
		this.context = context;
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.USER) {
			return ImmutableSet.of(op);
		}
		return Collections.emptySet();
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.USER) {
			return ImmutableSet.of(new RenamedInboxMaintenance(context));
		}
		return Collections.emptySet();

	}

	public static class RepairFactory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new RenamedInboxRepair(context);
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
			return new UserMailboxWalk(context, mbox, domainUid, srv);
		}

		public abstract void folders(BiConsumer<StoreClient, ListResult> process, IServerTaskMonitor monitor);
	}

	public static final class UserMailboxWalk extends MailboxWalk {

		public UserMailboxWalk(BmContext context, ItemValue<Mailbox> mbox, String domainUid, Server srv) {
			super(context, mbox, domainUid, srv);
		}

		public void folders(BiConsumer<StoreClient, ListResult> process, IServerTaskMonitor monitor) {
			String login = mbox.value.name + "@" + domainUid;

			try (Sudo sudo = new Sudo(mbox.value.name, domainUid);
					StoreClient sc = new StoreClient(srv.address(), 1143, login, sudo.context.getSessionId())) {
				if (!sc.login()) {
					monitor.log("Fail to connect", mbox.value.name);
					return;
				}
				ListResult allFolders = sc.listAll();
				process.accept(sc, allFolders);
			}
		}
	}

	private static class RenamedInboxMaintenance extends InternalMaintenanceOperation {

		private final BmContext context;

		public RenamedInboxMaintenance(BmContext ctx) {
			super(op.identifier, null, IMailReplicaUids.REPAIR_SUBTREE_OP, 1);
			this.context = ctx;
		}

		@FunctionalInterface
		private interface FolderAction {

			void process(ItemValue<Mailbox> mbox, ListInfo folder, StoreClient sc, RepairTaskMonitor monitor);

		}

		public void runOperation(String domainUid, DirEntry entry, RepairTaskMonitor monitor, FolderAction action) {

			if (entry.archived) {
				monitor.log("DirEntry is archived, skipping it");
				monitor.end();
				return;
			}

			IMailboxes iMailboxes = context.getServiceProvider().instance(IMailboxes.class, domainUid);
			ItemValue<Mailbox> mbox = iMailboxes.getComplete(entry.entryUid);
			monitor.log("Checking {} {}", domainUid, mbox.value.name);

			ItemValue<Server> server = Topology.get().datalocation(entry.dataLocation);

			MailboxWalk moonWalk = MailboxWalk.create(context, mbox, domainUid, server.value);
			AtomicBoolean completed = new AtomicBoolean();
			moonWalk.folders((sc, allFolders) -> {
				allFolders.stream().filter(li -> BROKEN_NAME.equals(li.getName())).findAny().ifPresent(f -> {
					try {
						action.process(mbox, f, sc, monitor);
						completed.set(true);
					} catch (Exception e) {
						monitor.log(e.getMessage());
					}
				});
			}, monitor);
		}

		@Override
		public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			runOperation(domainUid, entry, monitor, (mbox, folder, sc, mon) -> {
				try {
					if (sc.select(folder.getName())) {
						monitor.log(mbox.value.name + "@" + domainUid + " has an extra 'Message reçus' folder");
					}
				} catch (IMAPException e) {
					monitor.log("ERROR " + e.getMessage());
				}
			});
			monitor.end();
		}

		@Override
		public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			runOperation(domainUid, entry, monitor, (mbox, folder, sc, mon) -> {
				try {
					sc.select(folder.getName());
					sc.expunge();
					Map<Integer, Integer> copied = sc.uidCopy("1:*", "INBOX");
					sc.select("INBOX");
					sc.deleteMailbox(folder.getName());
					monitor.notify(copied.size() + " email(s) in BOX " + BROKEN_NAME + " instead of INBOX");
				} catch (IMAPException e) {
					monitor.notify("IMAP ERROR " + e.getMessage());
				}
			});
			monitor.end();
		}

	}

}
