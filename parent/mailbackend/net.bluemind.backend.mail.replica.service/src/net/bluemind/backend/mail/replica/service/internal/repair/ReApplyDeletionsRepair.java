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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer;
import net.bluemind.config.Token;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.IDirEntryRepairSupport.InternalMaintenanceOperation;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.NameSpaceInfo;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.TaggedResult;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;

public class ReApplyDeletionsRepair extends InternalMaintenanceOperation {

	private static final Logger logger = LoggerFactory.getLogger(ReApplyDeletionsRepair.class);

	private static final String ID = "missed.deletions";
	private static final MaintenanceOperation op = MaintenanceOperation.create(ID,
			"Re-apply deletes missed by replication");

	public static class RepairFactory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new IDirEntryRepairSupport() {

				@Override
				public Set<MaintenanceOperation> availableOperations(Kind kind) {
					if (kind == Kind.USER || kind == Kind.MAILSHARE) {
						return Sets.newHashSet(op);
					} else {
						return Collections.emptySet();
					}
				}

				@Override
				public Set<InternalMaintenanceOperation> ops(Kind kind) {
					if (kind == Kind.USER || kind == Kind.MAILSHARE) {
						return Sets.newHashSet(new ReApplyDeletionsRepair(context));
					} else {
						return Collections.emptySet();
					}
				}

			};
		}
	}

	private final BmContext context;

	public ReApplyDeletionsRepair(BmContext ctx) {
		super(ID, null, null, 1);
		this.context = ctx;
	}

	@Override
	public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
		ReApplyDeletion op = new ReApplyDeletion() {

			@Override
			public void info(DirEntry entry) {
				monitor.log("Checking " + entry);
			}

			@Override
			public void applyDeletion(Collection<Integer> uids, String flag, StoreClient sc, ListInfo listInfo) {
				monitor.log("Should re-flag " + uids.size() + " message(s)");
			}

			@Override
			public void markContainerAsDeleted(IContainers service, String containerUid,
					ContainerDescriptor containerDescriptor) {
				monitor.log("Should mark container " + containerUid + " as deleted");
			}
		};
		run(op, domainUid, entry, monitor);
	}

	@Override
	public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
		ReApplyDeletion op = new ReApplyDeletion() {

			@Override
			public void info(DirEntry entry) {
				monitor.log("Repairing " + entry);
			}

			@Override
			public void applyDeletion(Collection<Integer> uids, String flag, StoreClient sc, ListInfo listInfo) {
				String set = uids.stream().map(Object::toString).collect(Collectors.joining(","));
				String cmd = "UID STORE " + set + " +FLAGS.SILENT (" + flag + ")";
				TaggedResult ok = sc.tagged(cmd);
				monitor.log("Updated " + uids.size() + " message(s) => " + ok.isOk());
				report.ok(ID, "Folder " + listInfo.getName() + " fixed");
			}

			@Override
			public void postProcessing(MboxContext mailApi, String flag, StoreClient sc, List<ListInfo> folders)
					throws IMAPException {
				for (ListInfo li : folders) {
					monitor.progress(1, "Cleanup " + li.getName());
					if (sc.select(li.getName())) {
						Collection<Integer> deleted = sc.uidSearchDeleted();
						if (deleted.isEmpty()) {
							continue;
						}
						String set = deleted.stream().map(Object::toString).collect(Collectors.joining(","));
						String cmd = "UID STORE " + set + " -FLAGS.SILENT (" + flag + ")";
						TaggedResult ok = sc.tagged(cmd);
						monitor.log("(imap) Updated " + deleted.size() + " message(s) => " + ok.isOk());
						sc.expunge();
						monitor.log("(imap) Resync mailbox record(s)");
					}
				}
				IMailboxFoldersByContainer subApi = mailApi.ctx.provider().instance(IMailboxFoldersByContainer.class,
						mailApi.subtree);
				for (ItemValue<MailboxFolder> f : subApi.all()) {
					IMailboxItems recsApi = mailApi.ctx.provider().instance(IMailboxItems.class, f.uid);
					recsApi.resync();
					monitor.log("Resync mailbox " + f.displayName);
				}
			}

			@Override
			public void markContainerAsDeleted(IContainers service, String containerUid,
					ContainerDescriptor containerDescriptor) {
				monitor.log("Marking container " + containerUid + " as deleted");
				ContainerModifiableDescriptor desc = new ContainerModifiableDescriptor();
				desc.defaultContainer = containerDescriptor.defaultContainer;
				desc.name = containerDescriptor.name;
				desc.deleted = true;
				service.update(containerUid, desc);
			}

		};

		run(op, domainUid, entry, monitor);
	}

	private static class MboxContext {
		private final BmContext ctx;
		private final String subtree;

		public MboxContext(String subtree, BmContext userCtx) {
			this.subtree = subtree;
			this.ctx = userCtx;
		}

	}

	private void run(ReApplyDeletion op, String domainUid, DirEntry entry, IServerTaskMonitor monitor) {
		ItemValue<Mailbox> mbox = context.provider().instance(IMailboxes.class, domainUid).getComplete(entry.entryUid);
		if (mbox == null) {
			return;
		}
		op.info(entry);

		processDbMailboxData(op, domainUid, mbox);

		if (mbox.value.type.sharedNs) {
			runOnSharedMailbox(op, domainUid, monitor, mbox);
		} else {
			runOnUserMailbox(op, domainUid, monitor, mbox);
		}

	}

	private void runOnSharedMailbox(ReApplyDeletion op, String domainUid, IServerTaskMonitor monitor,
			ItemValue<Mailbox> mbox) {
		ItemValue<Server> backend = Topology.get().datalocation(mbox.value.dataLocation);

		String entryUid = getMailshareWriter(domainUid, mbox);

		if (entryUid == null) {
			monitor.log("mailshare is not writable");
			return;
		}

		BmContext userCtx = context.su("repair-" + UUID.randomUUID().toString(), entryUid, domainUid);

		try (StoreClient sc = new StoreClient(backend.value.address(), 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				logger.error("Failed to connect {}", mbox.value.name);
				monitor.log("Failed to connect " + mbox.value.name);
				return;
			}

			String mboxName = mbox.value.name + "@" + domainUid;

			ListResult folders = sc.listSubFoldersMailbox(mboxName);
			// add mbox root
			folders.add(new ListInfo(mboxName, true));

			String flag = "ReApplyDeletionsRepair" + Long.toHexString(System.currentTimeMillis());
			String subtree = SubtreeContainer.mailSubtreeUid(domainUid, Namespace.shared, mbox.uid).subtreeUid();
			MboxContext mailApi = new MboxContext(subtree, userCtx);
			processFolders(op, mailApi, monitor, flag, sc, folders);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			monitor.log("Failed to repair mailshare " + mbox.uid + ": " + e.getMessage());

		}

	}

	/**
	 * Returns one user with write right on the mailshare
	 * 
	 * @param domainUid
	 * @param mbox
	 * @return
	 */
	private String getMailshareWriter(String domainUid, ItemValue<Mailbox> mbox) {
		IContainerManagement service = context.su().provider().instance(IContainerManagement.class,
				"mailbox:acls-" + mbox.uid);
		List<AccessControlEntry> accessControlList = new ArrayList<>(service.getAccessControlList());

		List<String> writers = accessControlList.stream().filter(entity -> entity.verb.can(Verb.Write))
				.map(entry -> entry.subject).collect(Collectors.toList());

		if (writers.isEmpty()) {
			return null;
		}
		IDirectory dir = context.su().provider().instance(IDirectory.class, domainUid);
		List<ItemValue<DirEntry>> entries = dir.getMultiple(writers);

		Optional<ItemValue<DirEntry>> dirEntry = entries.stream().filter(de -> de.value.kind == Kind.USER).findFirst();
		String entryUid = null;
		if (!dirEntry.isPresent()) {
			IGroup groupService = context.su().provider().instance(IGroup.class, domainUid);
			List<ItemValue<DirEntry>> groups = entries.stream().filter(de -> de.value.kind == Kind.GROUP)
					.collect(Collectors.toList());

			for (int i = 0; i < groups.size(); i++) {
				ItemValue<DirEntry> g = groups.get(i);
				List<Member> members = groupService.getExpandedUserMembers(g.uid);
				if (!members.isEmpty()) {
					entryUid = members.get(0).uid;
					break;
				}

			}

		} else {
			entryUid = dirEntry.get().uid;
		}
		return entryUid;
	}

	private void runOnUserMailbox(ReApplyDeletion op, String domainUid, IServerTaskMonitor monitor,
			ItemValue<Mailbox> mbox) {
		String latd = mbox.value.name + "@" + domainUid;
		LoginResponse resp = context.provider().instance(IAuthentication.class).su(latd);
		if (resp.authKey != null) {
			String flag = "fix" + Long.toHexString(System.currentTimeMillis());

			ItemValue<Server> backend = Topology.get().datalocation(mbox.value.dataLocation);
			try (StoreClient sc = new StoreClient(backend.value.address(), 1143, latd, resp.authKey)) {
				boolean loginOk = sc.login();
				if (!loginOk) {
					monitor.log("IMAP Login failed for " + latd);
					return;
				}
				NameSpaceInfo ni = sc.namespace();
				String shared = ni.getMailShares().get(0);
				String others = ni.getOtherUsers().get(0);

				List<ListInfo> folders = sc.listAll().stream().filter(li -> {
					if (!li.isSelectable() || li.getName().startsWith(shared) || li.getName().startsWith(others)) {
						return false;
					}
					return true;
				}).collect(Collectors.toList());
				String subtree = SubtreeContainer.mailSubtreeUid(domainUid, Namespace.users, mbox.uid).subtreeUid();
				BmContext userCtx = context.su("repair-" + UUID.randomUUID().toString(), mbox.uid, domainUid);
				MboxContext mailApi = new MboxContext(subtree, userCtx);
				processFolders(op, mailApi, monitor, flag, sc, folders);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			monitor.log("Sudo failed for " + latd);
		}
	}

	private void processDbMailboxData(ReApplyDeletion op, String domainUid, ItemValue<Mailbox> mbox) {
		IDbReplicatedMailboxes mboxFolders = context.provider().instance(IDbReplicatedMailboxes.class,
				partition(domainUid), mboxRoot(mbox));
		IContainers service = context.provider().instance(IContainers.class);
		mboxFolders.all().forEach(folder -> {
			if (folder.flags.contains(ItemFlag.Deleted)) {
				String containerUid = IMailReplicaUids.mboxRecords(folder.uid);
				ContainerDescriptor containerDescriptor = service.get(containerUid);
				if (containerDescriptor != null && !containerDescriptor.deleted) {
					op.markContainerAsDeleted(service, containerUid, containerDescriptor);
				}
			}
		});
	}

	private String mboxRoot(ItemValue<Mailbox> mbox) {
		if (mbox.value.type.sharedNs) {
			return mbox.value.name.replace(".", "^");
		}
		return "user." + mbox.value.name.replace(".", "^");
	}

	private String partition(String domainUid) {
		return domainUid.replace(".", "_");
	}

	private void processFolders(ReApplyDeletion op, MboxContext mailApi, IServerTaskMonitor monitor, String flag,
			StoreClient sc, List<ListInfo> folders) throws IMAPException {
		monitor.begin(2d * folders.size(), "Processing " + folders.size() + " folder(s)");
		for (ListInfo li : folders) {
			monitor.progress(1, "On " + li.getName());
			if (sc.select(li.getName())) {
				Collection<Integer> deleted = sc.uidSearchDeleted();
				if (deleted.isEmpty()) {
					monitor.log("No deletions in " + li.getName());
					continue;
				}
				op.applyDeletion(deleted, flag, sc, li);
			}
		}
		op.postProcessing(mailApi, flag, sc, folders);
	}

	private static interface ReApplyDeletion {

		public void info(DirEntry entry);

		public void applyDeletion(Collection<Integer> uids, String flag, StoreClient sc, ListInfo listInfo);

		public default void postProcessing(MboxContext mailApi, String flag, StoreClient sc, List<ListInfo> folders)
				throws IMAPException {
		}

		public void markContainerAsDeleted(IContainers service, String containerUid,
				ContainerDescriptor containerDescriptor);

	}

}
