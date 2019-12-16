/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.service.internal;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.api.IReadOnlyMailboxFolders;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore.SubtreeLocation;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.hierarchy.hook.HierarchyIdsHints;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;

public class DbReplicatedMailboxesService extends BaseReplicatedMailboxesService
		implements IDbReplicatedMailboxes, IDbByContainerReplicatedMailboxes, IReadOnlyMailboxFolders {

	private static final Logger logger = LoggerFactory.getLogger(DbReplicatedMailboxesService.class);

	public DbReplicatedMailboxesService(MailboxReplicaRootDescriptor root, Container cont, BmContext context,
			MailboxReplicaStore store, ContainerStoreService<MailboxReplica> mboxReplicaStore,
			ContainerStore contStore) {
		super(root, cont, context, store, mboxReplicaStore, contStore);
	}

	@Override
	public void create(String uid, MailboxReplica replica) {
		logger.info("CREATE {} n:{} fn:{}", uid, replica.name, replica.fullName);
		sanitizeNames(replica);
		String recordsContainerUid = IMailReplicaUids.mboxRecords(uid);
		String domainUid = replicaStore.partition.replace('_', '.');

		Long expectedId = FolderInternalIdCache.expectedFolderId(container, replica.fullName);
		ItemVersion created = null;
		if (expectedId != null) {
			logger.info("Hierarchy will use expected id {}", expectedId);
			String hierUid = ContainerHierarchyNode.uidFor(recordsContainerUid, IMailReplicaUids.MAILBOX_RECORDS,
					domainUid);
			HierarchyIdsHints.putHint(hierUid, expectedId);
		}

		if (root.ns == Namespace.deleted || root.ns == Namespace.deletedShared) {
			replica.deleted = true;
		}
		created = storeService.create(uid, replica.name, replica);

		// create the records container
		IContainers containerService = context.provider().instance(IContainers.class);
		if (containerService.getIfPresent(recordsContainerUid) == null) {
			ContainerDescriptor toCreate = ContainerDescriptor.create(recordsContainerUid, replica.name,
					container.owner, IMailReplicaUids.MAILBOX_RECORDS, domainUid, false);
			containerService.create(recordsContainerUid, toCreate);
			logger.info("Records container created {}", recordsContainerUid);
		} else {
			logger.warn("Associated records container {} already exists", recordsContainerUid);
		}
		ItemIdentifier iid = ItemIdentifier.of(uid, created.id, created.version);
		EmitReplicationEvents.subtreeUpdated(container.uid, iid);
	}

	@Override
	public void update(String uid, MailboxReplica replica) {
		sanitizeNames(replica);
		ItemValue<MailboxReplica> previous = getCompleteReplica(uid);
		if (previous != null) {
			ItemVersion upd = storeService.update(uid, replica.name, replica);

			ItemValue<MailboxReplica> toCache = ItemValue.create(previous, replica);
			toCache.displayName = replica.name;
			toCache.value.dataLocation = previous.value.dataLocation;
			MboxReplicasCache.cache(toCache);

			boolean minorChange = isMinorChange(replica, previous);
			if (!minorChange) {
				String recordsContainerUid = IMailReplicaUids.mboxRecords(uid);
				SubtreeLocation sl = SubtreeLocations.locations.getIfPresent(uid);
				if (sl != null) {
					String oldName = sl.boxName;
					sl.boxName = replica.fullName;
					logger.info("Updating cached location for {} from {} to {}", uid, oldName, sl.boxName);
				}
				IContainers contApi = context.provider().instance(IContainers.class);
				ContainerModifiableDescriptor cmd = new ContainerModifiableDescriptor();
				cmd.deleted = replica.deleted;
				cmd.name = replica.name;
				contApi.update(recordsContainerUid, cmd);
			}

			EmitReplicationEvents.subtreeUpdated(container.uid, ItemIdentifier.of(uid, upd.id, upd.version),
					minorChange);
		}
	}

	private boolean isMinorChange(MailboxReplica replica, ItemValue<MailboxReplica> previous) {
		boolean sameName = previous.value.fullName.equals(replica.fullName);
		boolean deletionStatusUnchanged = previous.flags.contains(ItemFlag.Deleted) == replica.deleted;

		return sameName && deletionStatusUnchanged;
	}

	@Override
	public void delete(String uid) {
		IDbMailboxRecords recordsApi = null;
		try {
			recordsApi = context.provider().instance(IDbMailboxRecords.class, uid);
			recordsApi.prepareContainerDelete();
		} catch (ServerFault sf) {
			logger.warn("Records API does not work for {}: {}", uid, sf.getMessage());
		}

		ItemValue<MailboxReplica> replicaToDelete = getCompleteReplica(uid);

		if (replicaToDelete.value.fullName.equals("INBOX")) {
			logger.warn("Skip INBOX deletion {}", replicaToDelete);
			return;
		}

		logger.info("***** Will delete {}", replicaToDelete);
		// is it root ??? should we drop the subtree ?
		ItemVersion deleted = storeService.delete(uid);
		if (deleted != null) {
			MboxReplicasCache.invalidate(uid);
			String toDelete = IMailReplicaUids.mboxRecords(uid);
			if (recordsApi != null) {
				logger.info("Purge records in {} {}...", uid, toDelete);
				recordsApi.deleteAll();
			}

			context.provider().instance(IContainers.class).delete(toDelete);

			EmitReplicationEvents.subtreeUpdated(container.uid, ItemIdentifier.of(uid, deleted.id, deleted.version));
		}

	}

	@Override
	public ItemValue<MailboxReplica> byReplicaName(String name) {
		return super.byReplicaName(name);
	}

	@Override
	public List<ItemValue<MailboxReplica>> allReplicas() {
		List<ItemValue<MailboxReplica>> ret = storeService.all();
		ret.forEach(iv -> iv.value.dataLocation = dataLocation);
		return ret;
	}

	@Override
	public void xfer(String serverUid) throws ServerFault {

		DataSource ds = context.getMailboxDataSource(serverUid);
		ContainerStore cs = new ContainerStore(null, ds, context.getSecurityContext());
		Container c;
		try {
			c = cs.get(container.uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		storeService.xfer(ds, c, new MailboxReplicaStore(ds, c, replicaStore.partition));

	}

}
