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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.backend.mail.api.IItemsTransfer;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.ImportMailboxItemSet;
import net.bluemind.backend.mail.api.ImportMailboxItemSet.MailboxItemId;
import net.bluemind.backend.mail.api.ImportMailboxItemsStatus;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecord.InternalFlag;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplica.Acl;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.WithId;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.backend.mail.replica.service.internal.BodyInternalIdCache.ExpectedId;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;

public class ImapReplicatedMailboxesService extends BaseReplicatedMailboxesService
		implements IMailboxFolders, IMailboxFoldersByContainer {

	private static final Logger logger = LoggerFactory.getLogger(ImapReplicatedMailboxesService.class);

	public ImapReplicatedMailboxesService(MailboxReplicaRootDescriptor root, Container cont, BmContext context,
			MailboxReplicaStore store, ContainerStoreService<MailboxReplica> mboxReplicaStore,
			ContainerStore contStore) {
		super(root, cont, context, store, mboxReplicaStore, contStore);
		logger.debug("Created.");
	}

	@Override
	public ItemValue<MailboxFolder> getCompleteById(long id) {
		rbac.check(Verb.Read.name());

		return adapt(storeService.get(id, null));
	}

	@Override
	public List<ItemValue<MailboxFolder>> multipleGetById(List<Long> ids) {
		rbac.check(Verb.Read.name());

		return storeService.getMultipleById(ids).stream().map(this::adapt).collect(Collectors.toList());
	}

	@Override
	public Ack updateById(long id, MailboxFolder v) {
		rbac.check(Verb.Write.name());

		ItemValue<MailboxFolder> current = getCompleteById(id);
		if (current == null) {
			throw ServerFault.notFound("mailboxReplica with id " + id + " not found.");
		}

		MailboxFolder value = nameSanitizer.sanitizeNames(v);

		if (value.fullName.equals(current.value.fullName)) {
			logger.warn("Rename attempt to same name '{}'", value.fullName);
			storeService.touch(current.uid);
			ItemValue<MailboxFolder> touched = getCompleteById(id);
			return Ack.create(touched.version);
		}

		FolderTree fullTree = FolderTree.of(all());
		List<ItemValue<MailboxFolder>> renameTargets = fullTree.children(current);
		IDbByContainerReplicatedMailboxes writeDelegate = context.provider()
				.instance(IDbByContainerReplicatedMailboxes.class, container.uid);
		ItemValue<MailboxReplica> curReplica = writeDelegate.getCompleteById(current.internalId);
		MailboxReplica curCopy = MailboxReplica.from(curReplica.value);
		curCopy.name = null;
		curCopy.parentUid = null;
		curCopy.fullName = value.fullName;
		writeDelegate.update(curReplica.uid, curCopy);
		for (ItemValue<MailboxFolder> tgt : renameTargets) {
			ItemValue<MailboxReplica> parent = getCompleteReplica(tgt.value.parentUid);
			ItemValue<MailboxReplica> replicaTgt = getCompleteReplica(tgt.uid);
			MailboxReplica tgtCopy = MailboxReplica.from(replicaTgt.value);
			tgtCopy.name = null;
			tgtCopy.parentUid = null;
			tgtCopy.fullName = parent.value.fullName + "/" + replicaTgt.value.name;
			writeDelegate.update(tgt.uid, tgtCopy);
		}
		return Ack.create(storeService.getVersion());
	}

	@Override
	public ItemIdentifier createForHierarchy(long hierId, MailboxFolder v) {
		rbac.check(Verb.Write.name());

		MailboxFolder value = nameSanitizer.sanitizeNames(v);

		ItemValue<MailboxReplica> folder = byReplicaName(value.fullName);
		if (folder != null) {
			return ItemIdentifier.of(folder.uid, folder.internalId, folder.version);
		}

		FolderInternalIdCache.storeExpectedRecordId(container, value.fullName, hierId);
		IDbByContainerReplicatedMailboxes writeDelegate = context.provider()
				.instance(IDbByContainerReplicatedMailboxes.class, container.uid);
		MailboxReplica mr = new MailboxReplica();
		mr.fullName = value.fullName;
		mr.highestModSeq = 0;
		mr.xconvModSeq = 0;
		mr.lastUid = 0;
		mr.recentUid = 0;
		mr.options = "";
		mr.syncCRC = 0;
		mr.quotaRoot = null;
		mr.uidValidity = 0;
		mr.lastAppendDate = new Date();
		mr.pop3LastLogin = new Date();
		mr.recentTime = new Date();
		mr.acls = new LinkedList<>();
		mr.acls.add(Acl.create(container.owner + "@" + container.domainUid, "lrswipkxtecda"));
		mr.acls.add(Acl.create("admin0", "lrswipkxtecda"));
		mr.deleted = false;
		String pipoUid = UUID.randomUUID().toString();
		writeDelegate.create(pipoUid, mr);
		ItemValue<MailboxReplica> created = getCompleteReplica(pipoUid);
		return ItemIdentifier.of(created.uid, created.internalId, created.version);
	}

	@Override
	public ItemIdentifier createBasic(MailboxFolder value) {
		rbac.check(Verb.Write.name());

		IOfflineMgmt offlineApi = context.provider().instance(IOfflineMgmt.class, container.domainUid, container.owner);
		IdRange alloc = offlineApi.allocateOfflineIds(1);
		return createForHierarchy(alloc.globalCounter, value);
	}

	@Override
	public void deleteById(long id) {
		rbac.check(Verb.Write.name());

		ItemValue<MailboxFolder> toDelete = getCompleteById(id);
		if (toDelete == null || toDelete.value == null) {
			throw ServerFault.notFound("Folder with id " + id + " not found");
		}
		if (toDelete.value.deleted) {
			throw ServerFault.notFound("Folder with id " + id + " has already been deleted.");
		}
		MboxReplicasCache.invalidate(toDelete.uid);
		ItemVersion delResult = storeService.delete(toDelete.internalId);
		notifyUpdate(toDelete.value.parentUid != null ? getComplete(toDelete.value.parentUid) : toDelete,
				delResult.version);
		logger.info("Deleting {} -> {}", toDelete, delResult);
	}

	private void notifyUpdate(ItemValue<MailboxFolder> parent, long version) {
		EmitReplicationEvents.subtreeUpdated(container.uid, container.owner,
				ItemIdentifier.of(parent.uid, parent.internalId, version), false);
	}

	@Override
	public void deepDelete(long id) {
		rbac.check(Verb.Write.name());

		ItemValue<MailboxFolder> toDelete = getCompleteById(id);
		if (toDelete == null || toDelete.value == null) {
			throw ServerFault.notFound("Folder with id " + id + " not found");
		}
		logger.info("Start deepDelete of {}...", toDelete);
		deleteChildFolders(toDelete);
		deleteById(id);
	}

	public void emptyFolder(long id) {
		rbac.check(Verb.Write.name());
		emptyFolder(id, true);
	}

	public void removeMessages(long id) {
		rbac.check(Verb.Write.name());
		emptyFolder(id, false);
	}

	private void emptyFolder(long id, boolean deleteChildFolders) {
		ItemValue<MailboxFolder> folder = getCompleteById(id);
		if (folder == null || folder.value == null) {
			throw ServerFault.notFound("Folder with id " + id + " not found");
		}
		ItemFlagFilter filter = ItemFlagFilter.create().mustNot(ItemFlag.Deleted);
		Count count = context.provider().instance(IDbMailboxRecords.class, folder.uid).count(filter);
		logger.info("Start emptying {} (deleteChildFolders={})...", folder, deleteChildFolders);
		if (deleteChildFolders) {
			deleteChildFolders(folder);
		}
		logger.info("On purge of '{}'", folder.value.fullName);
		if (count.total > 0) {
			flag(folder, MailboxItemFlag.System.Deleted);
		}
	}

	public void markFolderAsRead(long id) {
		rbac.check(Verb.Write.name());
		ItemValue<MailboxFolder> folder = getCompleteById(id);
		if (folder == null || folder.value == null) {
			throw ServerFault.notFound("Folder with id " + id + " not found");
		}
		flag(folder, MailboxItemFlag.System.Seen);
	}

	private void flag(ItemValue<MailboxFolder> folder, MailboxItemFlag.System flag) {
		IDbMailboxRecords writeDelegate = context.provider().instance(IDbMailboxRecords.class, folder.uid);
		List<Long> allIds = writeDelegate.imapIdSet("1:*", "");
		for (List<Long> slice : Lists.partition(allIds, 500)) {
			List<WithId<MailboxRecord>> recSlice = writeDelegate.slice(slice);
			List<MailboxRecord> updates = recSlice.stream().map(wid -> {
				MailboxRecord mr = wid.value;
				mr.flags.add(flag.value());
				if (flag == MailboxItemFlag.System.Deleted) {
					mr.internalFlags.add(InternalFlag.expunged);
				}
				return mr;
			}).toList();
			writeDelegate.updates(updates);
		}
		if (allIds.isEmpty()) {
			logger.info("No item to mark as {} in folder {}", flag, folder);
		} else {
			ItemVersion touched = storeService.touch(folder.uid);
			notifyUpdate(folder, touched.version);
		}
	}

	private void deleteChildFolders(ItemValue<MailboxFolder> toClean) {
		FolderTree fullTree = FolderTree.of(all());
		List<ItemValue<MailboxFolder>> children = fullTree.children(toClean);
		Collections.reverse(children);
		children.forEach(child -> {
			MboxReplicasCache.invalidate(child.uid);
			ItemVersion delVersion = storeService.delete(child.internalId);
			notifyUpdate(toClean, delVersion.version);
		});
	}

	@Override
	public ImportMailboxItemsStatus importItems(long id, ImportMailboxItemSet mailboxItems) throws ServerFault {
		rbac.check(Verb.Write.name());

		List<MailboxItemId> expectedIds = mailboxItems.expectedIds;
		String importOpID = UUID.randomUUID().toString();

		int len = mailboxItems.ids.size();

		ItemValue<MailboxFolder> destinationFolder = getCompleteById(id);
		if (destinationFolder == null) {
			throw new ServerFault("Cannot find destination mailboxfolder");
		}

		ItemValue<MailboxFolder> sourceFolder = getCompleteById(mailboxItems.mailboxFolderId);
		if (sourceFolder == null) {
			throw new ServerFault("Cannot find source mailboxfolder");
		}

		// context.provider().instance(, null)

		IDbMailboxRecords sourceMailboxItemsService = context.provider().instance(IDbMailboxRecords.class,
				sourceFolder.uid);

		logger.info("[{}] Op {} to import {} item(s) from {} into {}", container.name, importOpID,
				mailboxItems.ids.size(), sourceFolder.value.fullName, destinationFolder.value.fullName);

		if (expectedIds != null && !expectedIds.isEmpty()) {
			if (expectedIds.size() != len) {
				throw new ServerFault("expectedIds size does not match with itemIds size", ErrorCode.INVALID_PARAMETER);
			}
			ListIterator<MailboxItemId> expectedIdsIterator = expectedIds.listIterator();

			Lists.partition(mailboxItems.ids, 200).forEach(ids -> {

				List<Long> idSlice = ids.stream().map(k -> k.id).toList();

				List<ImapBinding> itemsSlice = sourceMailboxItemsService.imapBindings(idSlice);
				if (itemsSlice.isEmpty()) {
					return;
				}

				Map<Long, Long> imapUidItemId = new HashMap<>();
				Map<Long, Long> imapUidExpectedId = new HashMap<>();

				List<Integer> allImapUids = new ArrayList<>(itemsSlice.size());
				itemsSlice.forEach(item -> {
					MailboxItemId expected = expectedIdsIterator.next();
					imapUidItemId.put(item.imapUid, item.itemId);
					imapUidExpectedId.put(item.imapUid, expected.id);
					allImapUids.add((int) item.imapUid);
					BodyInternalIdCache.storeExpectedRecordId(item.bodyGuid,
							new ExpectedId(expected.id, container.owner, null));
					GuidExpectedIdCache.store(destinationFolder.uid + ":" + item.bodyGuid, expected.id);
				});
			});
		}

		IItemsTransfer transferApi = context.provider().instance(IItemsTransfer.class, sourceFolder.uid,
				destinationFolder.uid);
		boolean move = mailboxItems.deleteFromSource;
		List<Long> sourceObjects = mailboxItems.ids.stream().map(k -> k.id).toList();
		List<ItemIdentifier> copies = move ? transferApi.move(sourceObjects) : transferApi.copy(sourceObjects);
		return ImportMailboxItemsStatus.fromTransferResult(sourceObjects, copies);

	}

}
