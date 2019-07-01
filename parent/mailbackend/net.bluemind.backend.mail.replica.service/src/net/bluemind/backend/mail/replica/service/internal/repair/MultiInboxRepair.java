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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.mailbox.api.Mailbox;

public class MultiInboxRepair implements IDirEntryRepairSupport {

	public static class RepairFactory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new MultiInboxRepair(context);
		}
	}

	public static final MaintenanceOperation minboxOp = MaintenanceOperation.create(IMailReplicaUids.REPAIR_MINBOX_OP,
			"Multiple INBOX in subtree");

	private static class FullNameDuplicatesMaintenance extends MailboxFoldersRepairOp {

		public FullNameDuplicatesMaintenance(BmContext ctx) {
			super(ctx, minboxOp.identifier, null, IMailReplicaUids.REPAIR_SUBTREE_OP, 1);
		}

		@Override
		public void runOnFolders(boolean repair, IServerTaskMonitor monitor, DiagnosticReport report, String subTree,
				String domainUid, ItemValue<Mailbox> mbox, List<ItemValue<MailboxReplica>> fullList) {
			IDbByContainerReplicatedMailboxes foldersApi = context.provider()
					.instance(IDbByContainerReplicatedMailboxes.class, subTree);

			monitor.begin(1, "Inspecting subtree for mailbox " + mbox.value.name + "@" + domainUid);

			// get rid of orphan
			IContainers contApi = context.provider().instance(IContainers.class);
			Set<String> uniqueIdsOfRecords = contApi
					.allLight(ContainerQuery.ownerAndType(mbox.uid, IMailReplicaUids.MAILBOX_RECORDS)).stream()
					.map(c -> IMailReplicaUids.uniqueId(c.uid)).collect(Collectors.toSet());
			Sets.difference(uniqueIdsOfRecords, fullList.stream().map(iv -> iv.uid).collect(Collectors.toSet()))
					.forEach(uniqueId -> {
						monitor.log("Purging orphan records container with unique id " + uniqueId + "...");
						try {
							IDbMailboxRecords recsApi = context.provider().instance(IDbMailboxRecords.class, uniqueId);
							recsApi.prepareContainerDelete();
							contApi.delete(IMailReplicaUids.mboxRecords(uniqueId));
						} catch (ServerFault sf) {
							logger.warn("Cannot get rid of {}: {}. Wrong DS ?", uniqueId, sf.getMessage());
							monitor.log("Skipped " + uniqueId + ", wrong datasource ?");
						}
					});

			Multimap<String, ItemValue<MailboxReplica>> byFullName = ArrayListMultimap.create();
			fullList.forEach(iv -> byFullName.put(iv.value.fullName, iv));
			List<ItemValue<MailboxReplica>> toPurge = new LinkedList<>();
			for (String fn : byFullName.keySet()) {
				Collection<ItemValue<MailboxReplica>> shouldBeOne = byFullName.get(fn);
				int len = shouldBeOne.size();
				if (len > 1) {
					List<ItemValue<MailboxReplica>> bestFirst = shouldBeOne.stream()
							.sorted((i1, i2) -> Long.compare(i2.value.highestModSeq, i1.value.highestModSeq))
							.collect(Collectors.toList());
					toPurge.addAll(bestFirst.subList(1, bestFirst.size()));
				}
				monitor.progress(len, "process " + fn + " with " + (len - 1) + " duplicates");
			}
			for (ItemValue<MailboxReplica> itemValue : toPurge) {
				monitor.log("Purge " + itemValue);
				if (repair) {
					foldersApi.delete(itemValue.uid);
				}
			}
			report.ok(minboxOp.identifier, "Subtree duplicates fixed.");
		}

	}

	private final BmContext context;

	public MultiInboxRepair(BmContext context) {
		this.context = context;
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.USER || kind == Kind.MAILSHARE || kind == Kind.GROUP || kind == Kind.RESOURCE) {
			return ImmutableSet.of(minboxOp);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.USER || kind == Kind.MAILSHARE || kind == Kind.GROUP || kind == Kind.RESOURCE) {
			return ImmutableSet.of(new FullNameDuplicatesMaintenance(context));
		} else {
			return Collections.emptySet();
		}

	}
}
