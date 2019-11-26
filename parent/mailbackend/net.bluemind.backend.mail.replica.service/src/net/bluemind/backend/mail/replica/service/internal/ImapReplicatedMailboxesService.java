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
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.vertx.VXStoreClient;
import net.bluemind.mailbox.api.IMailboxAclUids;

public class ImapReplicatedMailboxesService extends BaseReplicatedMailboxesService
		implements IMailboxFolders, IMailboxFoldersByContainer {

	private static final Logger logger = LoggerFactory.getLogger(ImapReplicatedMailboxesService.class);
	private final ImapContext imapContext;
	private final RBACManager rbac;

	public ImapReplicatedMailboxesService(MailboxReplicaRootDescriptor root, Container cont, BmContext context,
			MailboxReplicaStore store, ContainerStoreService<MailboxReplica> mboxReplicaStore,
			ContainerStore contStore) {
		super(root, cont, context, store, mboxReplicaStore, contStore);
		this.imapContext = ImapContext.of(context);
		this.rbac = RBACManager.forContext(context).forContainer(IMailboxAclUids.uidForMailbox(container.owner));
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
			oldName = recLoc.imapPath() + "/" + current.value.name;
			newName = recLoc.imapPath() + "/" + value.name;
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
			newName = recLoc.imapPath() + "/" + value.name;
		}

		ItemValue<MailboxFolder> folder = byName(newName);
		if (folder != null) {
			return ItemIdentifier.of(folder.uid, folder.internalId, folder.version);
		}

		FolderInternalIdCache.storeExpectedRecordId(container, value.fullName, hierId);
		final String computedName = newName;
		CompletableFuture<ItemIdentifier> future = ReplicationEvents.onSubtreeUpdate(toWatch);
		logger.info("{} Should create '{}'", root, computedName);
		return imapContext.withImapClient((sc, fast) -> {
			boolean ok = sc.create(computedName);
			if (ok) {
				ItemIdentifier iid = future.get(10, TimeUnit.SECONDS);
				// boolean annotated = sc.setMailboxAnnotation(value.name,
				// "/vendor/blue-mind/replication/id",
				// ImmutableMap.of("value.priv", Long.toString(iid.id)));
				// logger.info("Created and tried to annotate, ok: {}",
				// annotated);
				return iid;
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

	@Override
	public void deleteById(long id) {
		rbac.check(Verb.Write.name());

		ItemValue<MailboxFolder> toDelete = getCompleteById(id);
		logger.info("toDelete: {}", toDelete);
		String newName = toDelete.value.fullName;
		String toWatch = container.uid;
		if (root.ns == Namespace.shared && toDelete.value.parentUid != null) {
			String parentRecs = IMailReplicaUids.mboxRecords(toDelete.value.parentUid);
			ReplicasStore repStore = new ReplicasStore(DataSourceRouter.get(context, parentRecs));
			Optional<SubtreeLocation> optRecordsLocation = SubtreeLocations.getById(repStore, toDelete.value.parentUid);
			if (!optRecordsLocation.isPresent()) {
				throw ServerFault.notFound("subtree loc not found for parent " + toDelete.value.parentUid);
			}
			SubtreeLocation recLoc = optRecordsLocation.get();
			toWatch = recLoc.subtreeContainer;
			newName = recLoc.imapPath() + "/" + toDelete.value.name;
		}
		CompletableFuture<ItemIdentifier> future = ReplicationEvents.onSubtreeUpdate(toWatch);
		final String fnName = newName;
		final String fnToWath = toWatch;
		imapContext.withImapClient((sc, fast) -> {
			logger.info("Deleting {}", fnName);
			selectInbox(sc, fast);
			sc.deleteMailbox(fnName);
			try {
				return future.get(10, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				logger.warn("Failed to delete folder {} {}", fnName, fnToWath);
				throw new ServerFault(e);
			}
		});
	}

	private void selectInbox(StoreClient sc, VXStoreClient fast)
			throws IMAPException, InterruptedException, ExecutionException, TimeoutException {
		sc.select("INBOX");
		fast.select("INBOX").get(5, TimeUnit.SECONDS);
	}

	@Override
	public void deepDelete(long id) {
		rbac.check(Verb.Write.name());

		ItemValue<MailboxFolder> toDelete = getCompleteById(id);
		logger.info("Start deepDelete of {}...", toDelete);
		imapContext.withImapClient((sc, fast) -> {
			selectInbox(sc, fast);
			CompletableFuture<?> rootPromise = deleteChildFolders(toDelete, sc);

			rootPromise = rootPromise.thenCompose(v -> {
				logger.info("On deletion of '{}'", toDelete.value.fullName);
				CompletableFuture<ItemIdentifier> future = ReplicationEvents.onSubtreeUpdate(container.uid);
				try {
					sc.deleteMailbox(toDelete.value.fullName);
				} catch (IMAPException e) {
					future.completeExceptionally(e);
				}
				return future;
			});
			return rootPromise.get(15, TimeUnit.SECONDS);
		});
	}

	public void emptyFolder(long id) {
		ItemValue<MailboxFolder> toDelete = getCompleteById(id);
		logger.info("Start emptying {}...", toDelete);
		imapContext.withImapClient((sc, fast) -> {
			selectInbox(sc, fast);
			CompletableFuture<?> rootPromise = deleteChildFolders(toDelete, sc);
			rootPromise = rootPromise.thenCompose(v -> {
				logger.info("On purge of '{}'", toDelete.value.fullName);
				CompletableFuture<ItemIdentifier> future = ReplicationEvents.onSubtreeUpdate(container.uid);
				try {
					FlagsList fl = new FlagsList();
					fl.add(Flag.DELETED);
					if (sc.select(toDelete.value.fullName)) {
						if (sc.uidStore("1:*", fl, true)) {
							sc.expunge();
						}
					}
				} catch (IMAPException e) {
					future.completeExceptionally(e);
				}
				return future;
			});
			return rootPromise.get(15, TimeUnit.SECONDS);
		});
	}

	private CompletableFuture<?> deleteChildFolders(ItemValue<MailboxFolder> toClean, StoreClient sc) {
		String childPrefix = toClean.value.fullName + "/";
		ListResult allFolders = sc.listAll();
		List<String> toRemove = allFolders.stream()//
				.filter(li -> li.isSelectable() && li.getName().startsWith(childPrefix)).map(li -> li.getName())
				.sorted(Comparator.reverseOrder())//
				.collect(Collectors.toList());

		CompletableFuture<?> rootPromise = CompletableFuture.completedFuture(null);
		for (String childFolder : toRemove) {
			rootPromise = rootPromise.thenCompose(v -> {
				logger.info("On deletion of '{}'", childFolder);
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

		logger.info("[{}] Preparing to import {} item(s) from {} into {}", imapContext.latd, mailboxItems.ids.size(),
				sourceFolder.value.fullName, destinationFolder.value.fullName);
		Iterator<MailboxItemId> expectedIdsIterator = expectedIds.iterator();

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
							logger.info("[{}] destination folder {} changed {}", imapContext.latd,
									destinationFolder.value.fullName, version);
							return version;
						});

				if (sc.select(sourceFolder.value.fullName)) {
					Map<Integer, Integer> result = sc.uidCopy(allImapUids, destinationFolder.value.fullName);
					if (result.isEmpty()) {
						// nothing was copied, so the replication promise will
						// never resolve
						logger.warn("[{}] None of {} was copied to {}", imapContext.latd, allImapUids,
								destinationFolder.value.fullName);
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
										logger.info("[{}] source folder {} changed {}", imapContext.latd,
												sourceFolder.value.fullName, version);
										return version;
									});
							FlagsList fl = new FlagsList();
							fl.add(Flag.DELETED);

							try {
								sc.select(sourceFolder.value.fullName);
								sc.uidStore(allImapUids, fl, true);
								sc.uidExpunge(allImapUids);
							} catch (IMAPException e) {
								logger.error(e.getMessage(), e);
								future.completeExceptionally(e);
							}

							return future;
						});
					}

					rootPromise.get(15, TimeUnit.SECONDS);
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

}
