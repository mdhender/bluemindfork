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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchy;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.IDirEntryRepairSupport.InternalMaintenanceOperation;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;

public class ReplicationDeletedMailboxRepair extends InternalMaintenanceOperation {

	public static class ReplicationDeletedMailboxRepairFactory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new IDirEntryRepairSupport() {

				@Override
				public Set<MaintenanceOperation> availableOperations(Kind kind) {
					if (kind == Kind.USER) {
						return Sets.newHashSet(op);
					} else {
						return Collections.emptySet();
					}
				}

				@Override
				public Set<InternalMaintenanceOperation> ops(Kind kind) {
					if (kind == Kind.USER) {
						return Sets.newHashSet(new ReplicationDeletedMailboxRepair(context));
					} else {
						return Collections.emptySet();
					}
				}

			};
		}
	}

	private static final String ID = "mailbox.deletions";
	private static final MaintenanceOperation op = MaintenanceOperation.create(ID,
			"Re-apply mailbox deletes missed by replication");

	private final BmContext context;

	public ReplicationDeletedMailboxRepair(BmContext ctx) {
		super(ID, null, null, 1);
		this.context = ctx;
	}

	@Override
	public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		run(false, domainUid, entry, monitor);
		monitor.end();
	}

	@Override
	public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		run(true, domainUid, entry, monitor);
		monitor.end();
	}

	private void run(boolean repair, String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
		ItemValue<Mailbox> mbox = context.provider().instance(IMailboxes.class, domainUid).getComplete(entry.entryUid);
		if (mbox == null) {
			return;
		}

		IServiceProvider provider = context.getServiceProvider();

		String latd = mbox.value.name + "@" + domainUid;
		ItemValue<Server> backend = Topology.get().datalocation(mbox.value.dataLocation);
		CyrusPartition part = CyrusPartition.forServerAndDomain(mbox.value.dataLocation, domainUid);
		IDbReplicatedMailboxes mboxFoldersService = context.su(mbox.uid, domainUid).getServiceProvider()
				.instance(IDbReplicatedMailboxes.class, part.name, "user." + mbox.uid.replace('.', '^'));

		Map<String, ItemValue<MailboxFolder>> toRepair = new HashMap<>();
		mboxFoldersService.all().forEach(folder -> {
			toRepair.put(folder.value.fullName, folder);
		});

		LoginResponse resp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IAuthentication.class).su(latd);

		try (StoreClient sc = new StoreClient(backend.value.address(), 1143, latd, resp.authKey)) {

			boolean loginOk = sc.login();
			if (!loginOk) {
				monitor.notify("IMAP Login failed for " + latd);
				return;
			}

			ListResult all = sc.listAll();
			all.forEach(f -> {
				String fn = f.getName();
				if (f.isSelectable()) {
					toRepair.remove(fn);
				}
			});

		}

		if (!toRepair.isEmpty()) {

			IInternalContainersFlatHierarchy contFlatH = provider.instance(IInternalContainersFlatHierarchy.class,
					domainUid, mbox.uid);

			toRepair.values().forEach(node -> {
				monitor.notify("Deleted mailbox '" + node.value.fullName + "' found");
				if (repair) {
					String uid = ContainerHierarchyNode.uidFor(IMailReplicaUids.mboxRecords(node.uid),
							IMailReplicaUids.MAILBOX_RECORDS, domainUid);

					ItemValue<MailboxReplica> mboxReplica = mboxFoldersService.byReplicaName(node.value.fullName);
					mboxReplica.value.deleted = true;
					mboxFoldersService.update(node.uid, mboxReplica.value);

					ItemValue<ContainerHierarchyNode> hierarchyNode = contFlatH.getComplete(uid);
					if (hierarchyNode != null) {
						hierarchyNode.value.deleted = true;
						contFlatH.update(uid, hierarchyNode.value);
					}

				}
			});
		}

	}

}
