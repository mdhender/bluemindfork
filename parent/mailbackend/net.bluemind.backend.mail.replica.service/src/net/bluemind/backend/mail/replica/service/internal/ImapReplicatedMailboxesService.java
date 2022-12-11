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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.ImportMailboxItemSet;
import net.bluemind.backend.mail.api.ImportMailboxItemSet.MailboxItemId;
import net.bluemind.backend.mail.api.ImportMailboxItemsStatus;
import net.bluemind.backend.mail.api.ImportMailboxItemsStatus.ImportStatus;
import net.bluemind.backend.mail.api.ImportMailboxItemsStatus.ImportedMailboxItem;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.backend.mail.replica.service.ReplicationEvents;
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
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;

public class ImapReplicatedMailboxesService extends BaseReplicatedMailboxesService
		implements IMailboxFolders, IMailboxFoldersByContainer {

	private static final Logger logger = LoggerFactory.getLogger(ImapReplicatedMailboxesService.class);
	private final ImapContext imapContext;

	public ImapReplicatedMailboxesService(MailboxReplicaRootDescriptor root, Container cont, BmContext context,
			MailboxReplicaStore store, ContainerStoreService<MailboxReplica> mboxReplicaStore,
			ContainerStore contStore) {
		super(root, cont, context, store, mboxReplicaStore, contStore);
		this.imapContext = ImapContext.of(context);
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

		String toWatch = container.uid;
		final String fnOld = imapPath(current.value);
		final String fnNew = imapPath(value);

		if (fnOld.equals(fnNew)) {
			logger.warn("Rename attempt to same name '{}'", fnOld);
			storeService.touch(current.uid);
			ItemValue<MailboxFolder> touched = getCompleteById(id);
			return Ack.create(touched.version);
		}

		CompletableFuture<ItemIdentifier> future = ReplicationEvents.onSubtreeUpdate(toWatch);
		return imapContext.withImapClient(sc -> {
			logger.info("Rename attempt of '{}' to '{}'", fnOld, fnNew);
			selectInbox(sc);
			sc.rename(fnOld, fnNew);
			long version = future.get(10, TimeUnit.SECONDS).version;
			return Ack.create(version);
		});
	}

	@Override
	public ItemIdentifier createForHierarchy(long hierId, MailboxFolder v) {
		rbac.check(Verb.Write.name());

		MailboxFolder value = nameSanitizer.sanitizeNames(v);
		UpdatedName newName = updateName(value, container.uid);

		ItemValue<MailboxFolder> folder = byName(newName.fullName);
		if (folder != null) {
			return ItemIdentifier.of(folder.uid, folder.internalId, folder.version);
		}

		FolderInternalIdCache.storeExpectedRecordId(container, value.fullName, hierId);
		final String computedName = newName.imapName;

		CompletableFuture<ItemIdentifier> future = ReplicationEvents.onMailboxCreated(newName.subtreeContainer,
				newName.fullName);
		logger.info("{} Should create fn: '{}' (parent: {})", root, newName.fullName, newName.parentUid);
		return imapContext.withImapClient(sc -> {
			boolean ok = sc.create(computedName);
			if (ok) {
				return future.get(10, TimeUnit.SECONDS);
			} else {
				throw new ServerFault("IMAP create of '" + computedName + "' in " + root + " failed.");
			}
		});
	}

	@Override
	public ItemIdentifier createBasic(MailboxFolder value) {
		rbac.check(Verb.Write.name());

		ImapContext ctx = imapContext;
		IOfflineMgmt offlineApi = context.provider().instance(IOfflineMgmt.class, ctx.user.domainUid, ctx.user.uid);
		IdRange alloc = offlineApi.allocateOfflineIds(1);
		return createForHierarchy(alloc.globalCounter, value);
	}

	private static class UpdatedName {
		String subtreeContainer;
		String fullName;
		String parentUid;
		String imapName;

		public UpdatedName(String subtreeContainer, String fullName, String imapName, String parentUid) {
			this.subtreeContainer = subtreeContainer;
			this.fullName = fullName;
			this.imapName = imapName;
			this.parentUid = parentUid;
		}

	}

	private UpdatedName updateName(MailboxFolder folder, String containerUid) {
		return new UpdatedName(containerUid, fullPath(folder), imapPath(folder), folder.parentUid);
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
		logger.info("Deleting {} -> {}", toDelete, delResult);
	}

	private void selectInbox(StoreClient sc) throws IMAPException {
		sc.select("INBOX");
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
		imapContext.withImapClient(storeClient -> {
			selectInbox(storeClient);
			logger.info("On purge of '{}'", folder.value.fullName);
			if (count.total > 0) {
				flag(storeClient, folder, Flag.DELETED, storeClient::expunge);
			}
			return null;
		});
	}

	public void markFolderAsRead(long id) {
		rbac.check(Verb.Write.name());
		ItemValue<MailboxFolder> folder = getCompleteById(id);
		if (folder == null || folder.value == null) {
			throw ServerFault.notFound("Folder with id " + id + " not found");
		}
		ItemFlagFilter filter = ItemFlagFilter.create().mustNot(ItemFlag.Seen);
		Count count = context.provider().instance(IDbMailboxRecords.class, folder.uid).count(filter);
		if (count.total != 0) {
			logger.info("Start marking as read {}...", folder);
			imapContext.withImapClient(storeClient -> {
				selectInbox(storeClient);
				flag(storeClient, folder, Flag.SEEN, null);
				return null;
			});
		} else {
			logger.info("No item to mark as seen in folder {}", folder);
		}
	}

	private void flag(StoreClient storeClient, ItemValue<MailboxFolder> folder, Flag flag, Runnable onSuccess) {
		try {
			FlagsList flags = new FlagsList();
			flags.add(flag);

			String folderName = imapPath(folder.value);
			logger.info("Add flag {} to '{}'", flag, folderName);
			if (storeClient.select(folderName)) {
				boolean flagApplied = storeClient.uidStore("1:*", flags, true);
				if (!flagApplied) {
					logger.warn("Could not apply flag {} to folder '{}'", flag, folderName);
				} else if (onSuccess != null) {
					onSuccess.run();
				}
			} else {
				logger.warn("Could not select folder '{}', flag {} not applied", folderName, flag);
			}
		} catch (IMAPException e) {
			throw new ServerFault(e);
		}
	}

	private void deleteChildFolders(ItemValue<MailboxFolder> toClean) {
		FolderTree fullTree = FolderTree.of(all());
		List<ItemValue<MailboxFolder>> children = fullTree.children(toClean);
		Collections.reverse(children);
		children.forEach(child -> {
			MboxReplicasCache.invalidate(child.uid);
			storeService.delete(child.internalId);
		});
	}

	@Override
	public ImportMailboxItemsStatus importItems(long id, ImportMailboxItemSet mailboxItems) throws ServerFault {
		rbac.check(Verb.Write.name());

		List<MailboxItemId> expectedIds = mailboxItems.expectedIds;
		String importOpID = UUID.randomUUID().toString();

		int len = mailboxItems.ids.size();
		if (expectedIds == null || expectedIds.isEmpty()) {
			expectedIds = new ArrayList<>(len);
			IOfflineMgmt idAllocator = context.provider().instance(IOfflineMgmt.class, container.domainUid,
					container.owner);
			IdRange idRange = idAllocator.allocateOfflineIds(len);
			for (int i = 0; i < idRange.count; i++) {
				expectedIds.add(MailboxItemId.of(idRange.globalCounter + i));
			}
		}

		if (expectedIds.size() != len) {
			throw new ServerFault("expectedIds size does not match with itemIds size", ErrorCode.INVALID_PARAMETER);
		}

		ItemValue<MailboxFolder> destinationFolder = getCompleteById(id);
		if (destinationFolder == null) {
			throw new ServerFault("Cannot find destination mailboxfolder");
		}

		ItemValue<MailboxFolder> sourceFolder = getCompleteById(mailboxItems.mailboxFolderId);
		if (sourceFolder == null) {
			throw new ServerFault("Cannot find source mailboxfolder");
		}

		IDbMailboxRecords sourceMailboxItemsService = context.provider().instance(IDbMailboxRecords.class,
				sourceFolder.uid);

		ImportMailboxItemsStatus finalResult = new ImportMailboxItemsStatus();
		finalResult.doneIds = new ArrayList<ImportedMailboxItem>(len);

		logger.info("[{}] Op {} to import {} item(s) from {} into {}", imapContext.latd, importOpID,
				mailboxItems.ids.size(), sourceFolder.value.fullName, destinationFolder.value.fullName);
		Iterator<MailboxItemId> expectedIdsIterator = expectedIds.iterator();

		String srcFolder = imapPath(sourceFolder.value);
		String dstFolder = imapPath(destinationFolder.value);

		Lists.partition(mailboxItems.ids, 200).forEach(ids -> {

			List<Long> idSlice = ids.stream().map(k -> k.id).collect(Collectors.toList());
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
				GuidExpectedIdCache.store(destinationFolder.uid + ":" + item.bodyGuid, expected.id);
			});

			ImportMailboxItemsStatus copyRes = imapContext.withImapClient(sc -> {
				ImportMailboxItemsStatus ret = new ImportMailboxItemsStatus();
				List<ImportedMailboxItem> doneIds = new ArrayList<>(allImapUids.size());
				ret.doneIds = doneIds;

				CompletableFuture<Long> rootPromise = ReplicationEvents.onMailboxChanged(destinationFolder.uid)
						.thenApply(version -> {
							logger.info("[{}] Op {} destination folder {} changed {}", imapContext.latd, importOpID,
									destinationFolder.value.fullName, version);
							return version;
						});

				logger.info("{} Copying {} items from {} to {}", importOpID, allImapUids.size(), srcFolder, dstFolder);

				if (sc.select(srcFolder)) {
					Map<Integer, Integer> result = sc.uidCopy(allImapUids, dstFolder);
					if (result.isEmpty()) {
						// nothing was copied, so the replication promise will
						// never resolve
						logger.warn("[{}] None of {} was copied to {}", imapContext.latd, allImapUids, dstFolder);
						return ret;
					} else {
						result.forEach((imapUid, newImapUid) -> {
							if (imapUidItemId.containsKey(Long.valueOf(imapUid))) {
								doneIds.add(ImportedMailboxItem.of(imapUidItemId.get(Long.valueOf(imapUid)),
										imapUidExpectedId.get(Long.valueOf(imapUid))));
							}
						});
					}

					if (mailboxItems.deleteFromSource) {
						rootPromise = rootPromise.thenCompose(destVersion -> {
							CompletableFuture<Long> future = ReplicationEvents.onMailboxChanged(sourceFolder.uid)
									.thenApply(version -> {
										logger.info("[{}] Op {} source folder {} changed {}", imapContext.latd,
												importOpID, sourceFolder.value.fullName, version);
										return version;
									});
							FlagsList fl = new FlagsList();
							fl.add(Flag.DELETED);

							try {
								sc.select(srcFolder);
								sc.uidStore(allImapUids, fl, true);
							} catch (IMAPException e) {
								logger.error(e.getMessage(), e);
								future.completeExceptionally(e);
							}

							return future;
						});
					}

					try {
						rootPromise.get(15, TimeUnit.SECONDS);
					} catch (TimeoutException timeoutException) {
						logger.warn(timeoutException.getMessage(), timeoutException);
					} catch (Exception e) {
						throw e;
					}
				}

				return ret;
			});

			finalResult.doneIds.addAll(copyRes.doneIds);
		});

		if (finalResult.doneIds.size() == expectedIds.size()) {
			finalResult.status = ImportStatus.SUCCESS;
		} else if (finalResult.doneIds.isEmpty()) {
			finalResult.status = ImportStatus.ERROR;
		} else {
			finalResult.status = ImportStatus.PARTIAL;
		}

		return finalResult;
	}

	private String imapPath(MailboxFolder folder) {
		return imapPath(folder.fullName);
	}

	private String imapPath(String fullName) {
		Namespace ns = root.ns;
		SecurityContext security = context.getSecurityContext();
		if (ns == Namespace.users && !security.fromGlobalVirt() && !container.owner.equals(security.getSubject())) {
			String root = container.name.substring(6);
			if (fullName.equals("INBOX")) {
				return "Autres utilisateurs/" + root;
			}
			return "Autres utilisateurs/" + root + "/" + fullName;

		} else {
			return fullPath(fullName);
		}
	}

	private String fullPath(MailboxFolder folder) {
		return fullPath(folder.fullName);
	}

	private String fullPath(String fullName) {
		if (root.ns == Namespace.users) {
			return fullName;
		} else {
			return "Dossiers partagés/" + fullName;
		}
	}

}
