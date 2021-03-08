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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore.SubtreeLocation;
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
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.imap.CreateMailboxResult;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.vertx.IAsyncStoreClient;

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
	public Ack updateById(long id, MailboxFolder value) {
		rbac.check(Verb.Write.name());

		ItemValue<MailboxFolder> current = getCompleteById(id);
		if (current == null) {
			throw ServerFault.notFound("mailboxReplica with id " + id + " not found.");
		}
		if (value.fullName != null && value.name == null) {
			int end = value.fullName.lastIndexOf('/');
			if (end == -1) {
				throw new ServerFault("Invalid name(s) " + value);
			}
			value.name = value.fullName.substring(end + 1);
		}
		String oldName = current.value.fullName;
		String newName = value.fullName;
		String toWatch = container.uid;
		if (root.ns == Namespace.shared && value.parentUid != null) {
			String parentRecs = IMailReplicaUids.mboxRecords(value.parentUid);
			ReplicasStore repStore = new ReplicasStore(DataSourceRouter.get(context, parentRecs));
			Optional<SubtreeLocation> optRecordsLocation = SubtreeLocations.getById(repStore, value.parentUid);
			if (!optRecordsLocation.isPresent()) {
				throw ServerFault.notFound("subtree loc not found for parent " + value.parentUid);
			}
			SubtreeLocation recLoc = optRecordsLocation.get();
			toWatch = recLoc.subtreeContainer;
			oldName = recLoc.imapPath(context) + "/" + current.value.name;
			newName = recLoc.imapPath(context) + "/" + value.name;
		}
		final String fnOld = oldName;
		final String fnNew = newName;

		if (fnOld.equals(fnNew)) {
			logger.warn("Rename attempt to same name '{}'", fnOld);
			storeService.touch(current.uid);
			ItemValue<MailboxFolder> touched = getCompleteById(id);
			return Ack.create(touched.version);
		}
		CompletableFuture<ItemIdentifier> future = ReplicationEvents.onSubtreeUpdate(toWatch);
		return imapContext.withImapClient((sc, fast) -> {
			logger.info("Rename attempt of '{}' to '{}'", fnOld, fnNew);
			selectInbox(sc, fast);
			sc.rename(fnOld, fnNew);
			long version = future.get(10, TimeUnit.SECONDS).version;
			return Ack.create(version);
		});
	}

	@Override
	public ItemIdentifier createForHierarchy(long hierId, MailboxFolder value) {
		rbac.check(Verb.Write.name());

		sanitizeNames(value);
		UpdatedName newName = updateName(value, container.uid);

		ItemValue<MailboxFolder> folder = byName(newName.fullName);
		if (folder != null) {
			return ItemIdentifier.of(folder.uid, folder.internalId, folder.version);
		}

		FolderInternalIdCache.storeExpectedRecordId(container, value.fullName, hierId);
		final String computedName = newName.fullName;

		CompletableFuture<ItemIdentifier> future = ReplicationEvents.onMailboxCreated(newName.subtreeContainer,
				newName.fullName);
		logger.info("{} Should create '{}'", root, computedName);
		return imapContext.withImapClient((sc, fast) -> {
			boolean ok = sc.create(computedName);
			if (ok) {
				return future.get(10, TimeUnit.SECONDS);
			} else {
				throw new ServerFault("IMAP create of '" + value.name + "' failed.");
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

		public UpdatedName(String subtreeContainer, String fullName) {
			this.subtreeContainer = subtreeContainer;
			this.fullName = fullName;
		}

	}

	private UpdatedName updateName(MailboxFolder folder, String containerUid) {
		if (root.ns == Namespace.shared && folder.parentUid != null) {
			String parentRecs = IMailReplicaUids.mboxRecords(folder.parentUid);
			ReplicasStore repStore = new ReplicasStore(DataSourceRouter.get(context, parentRecs));
			Optional<SubtreeLocation> optRecordsLocation = SubtreeLocations.getById(repStore, folder.parentUid);
			if (!optRecordsLocation.isPresent()) {
				throw ServerFault.notFound("subtree loc not found for parent " + folder.parentUid);
			}
			SubtreeLocation recLoc = optRecordsLocation.get();
			return new UpdatedName(recLoc.subtreeContainer, recLoc.imapPath(context) + "/" + folder.name);
		} else {
			return new UpdatedName(containerUid, folder.fullName);
		}
	}

	@Override
	public void deleteById(long id) {
		rbac.check(Verb.Write.name());

		ItemValue<MailboxFolder> toDelete = getCompleteById(id);
		if (toDelete.value.deleted) {
			throw ServerFault.notFound("Folder with id " + id + " has already been deleted.");
		}
		logger.info("toDelete: {}", toDelete);
		UpdatedName newName = updateName(toDelete.value, container.uid);
		CompletableFuture<ItemIdentifier> future = ReplicationEvents.onSubtreeUpdate(newName.subtreeContainer);
		final String fnName = newName.fullName;
		final String fnToWath = newName.subtreeContainer;
		imapContext.withImapClient((sc, fast) -> {
			logger.info("Deleting {}", fnName);
			selectInbox(sc, fast);
			CreateMailboxResult delRes = sc.deleteMailbox(fnName);
			if (delRes.isOk()) {
				try {
					return future.get(20, TimeUnit.SECONDS);
				} catch (Exception e) {
					logger.warn("Failed to delete folder {} {}", fnName, fnToWath);
					throw new ServerFault(e);
				}
			} else {
				logger.warn("Delete of {} failed: {}", fnName, delRes.getMessage());
				return null;
			}
		});
	}

	private void selectInbox(StoreClient sc, IAsyncStoreClient fast)
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException {
		sc.select("INBOX");
		fast.select("INBOX").get(5, TimeUnit.SECONDS);
	}

	@Override
	public void deepDelete(long id) {
		rbac.check(Verb.Write.name());

		ItemValue<MailboxFolder> toDelete = getCompleteById(id);
		logger.info("Start deepDelete of {}...", toDelete);
		CompletableFuture<?> rootPromise = imapContext.withImapClient((sc, fast) -> {
			selectInbox(sc, fast);
			return deleteChildFolders(toDelete, sc);
		}).thenApply(v -> {
			deleteById(id);
			return null;
		});
		try {
			rootPromise.get(15, TimeUnit.SECONDS);
		} catch (Exception e) {
			logger.warn("Failed to deep delete folder {} of container {}", id, container);
			throw new ServerFault(e);
		}
	}

	public void emptyFolder(long id) {
		emptyFolder(id, true);
	}

	public void removeMessages(long id) {
		emptyFolder(id, false);
	}

	private void emptyFolder(long id, boolean deleteChildFolders) {
		ItemValue<MailboxFolder> folder = getCompleteById(id);
		ItemFlagFilter filter = ItemFlagFilter.create().mustNot(ItemFlag.Deleted);
		Count count = context.provider().instance(IDbMailboxRecords.class, folder.uid).count(filter);
		if (count.total == 0) {
			logger.info("Folder {} already empty ..", folder);
			return;
		}
		logger.info("Start emptying {} (deleteChildFolders={})...", folder, deleteChildFolders);
		imapContext.withImapClient((storeClient, vxStoreClient) -> {
			selectInbox(storeClient, vxStoreClient);
			CompletableFuture<?> promise = deleteChildFolders ? deleteChildFolders(folder, storeClient)
					: CompletableFuture.completedFuture(null);
			return promise.thenCompose(v -> {
				logger.info("On purge of '{}'", folder.value.fullName);
				return this.flag(storeClient, folder, Flag.DELETED, storeClient::expunge);
			}).get(15, TimeUnit.SECONDS);
		});
	}

	public void markFolderAsRead(long id) {
		ItemValue<MailboxFolder> folder = getCompleteById(id);
		ItemFlagFilter filter = ItemFlagFilter.create().mustNot(ItemFlag.Seen);
		Count count = context.provider().instance(IDbMailboxRecords.class, folder.uid).count(filter);
		if (count.total != 0) {
			logger.info("Start marking as read {}...", folder);
			imapContext.withImapClient((storeClient, vxStoreClient) -> {
				selectInbox(storeClient, vxStoreClient);
				return this.flag(storeClient, folder, Flag.SEEN, null).get(15, TimeUnit.SECONDS);
			});
		} else {
			logger.info("No item to mark as seen in folder {}", folder);
		}
	}

	private CompletableFuture<ItemIdentifier> flag(StoreClient storeClient, ItemValue<MailboxFolder> folder, Flag flag,
			Runnable onSuccess) {
		CompletableFuture<ItemIdentifier> future = ReplicationEvents.onSubtreeUpdate(container.uid);
		try {
			FlagsList flags = new FlagsList();
			flags.add(flag);

			String folderName = root.ns == Namespace.shared ? computeSharedFolderFullName(folder)
					: folder.value.fullName;
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
			future.completeExceptionally(e);
		}
		return future;
	}

	private String computeSharedFolderFullName(ItemValue<MailboxFolder> folder) {
		String parentUid = folder.value.parentUid != null ? folder.value.parentUid : folder.uid;
		String parentRecs = IMailReplicaUids.mboxRecords(parentUid);
		ReplicasStore repStore = new ReplicasStore(DataSourceRouter.get(context, parentRecs));
		return SubtreeLocations.getById(repStore, parentUid)
				.map(sl -> sl.imapPath(context) + (folder.value.parentUid != null ? "/" + folder.value.name : ""))
				.orElseThrow(() -> new ServerFault("subtree loc not found for parent " + parentUid));
	}

	private CompletableFuture<?> deleteChildFolders(ItemValue<MailboxFolder> toClean, StoreClient sc) {
		String childPrefix = toClean.value.fullName + "/";
		ListResult allFolders = sc.listAll();
		List<String> toRemove = allFolders.stream()//
				.filter(li -> {
					String check = li.getName();
					if (root.ns == Namespace.shared) {
						check = check.replace("Dossiers partagés/" + imapRoot() + "/", "");
					}
					return li.isSelectable() && check.startsWith(childPrefix);
				}).map(li -> li.getName()).sorted(Comparator.reverseOrder())//
				.collect(Collectors.toList());

		CompletableFuture<?> rootPromise = CompletableFuture.completedFuture(null);
		for (String childFolder : toRemove) {
			rootPromise = rootPromise.thenCompose(v -> {
				logger.info("On deletion of child folder '{}'", childFolder);
				CompletableFuture<ItemIdentifier> future = ReplicationEvents.onSubtreeUpdate(container.uid);
				try {
					sc.deleteMailbox(childFolder);
				} catch (IMAPException e) {
					future.completeExceptionally(e);
				}
				return future;
			});
		}
		return rootPromise;
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

			Map<Long, Long> imapUidItemId = new HashMap<Long, Long>();
			Map<Long, Long> imapUidExpectedId = new HashMap<Long, Long>();

			List<Integer> allImapUids = new ArrayList<>(itemsSlice.size());
			itemsSlice.forEach(item -> {
				MailboxItemId expected = expectedIdsIterator.next();
				imapUidItemId.put(item.imapUid, item.itemId);
				imapUidExpectedId.put(item.imapUid, expected.id);
				allImapUids.add((int) item.imapUid);
				GuidExpectedIdCache.store(destinationFolder.uid + ":" + item.bodyGuid, expected.id);
			});

			ImportMailboxItemsStatus copyRes = imapContext.withImapClient((sc, fast) -> {
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
		Namespace ns = root.ns;
		if (ns == Namespace.users) {
			return folder.fullName;
		} else {
			String root = imapRoot();
			if (root.equals(folder.fullName)) {
				// root
				return "Dossiers partagés/" + root;
			} else {
				return "Dossiers partagés/" + root + "/" + folder.fullName;
			}
		}
	}

	private String imapRoot() {
		return container.name.substring(7);
	}

}
