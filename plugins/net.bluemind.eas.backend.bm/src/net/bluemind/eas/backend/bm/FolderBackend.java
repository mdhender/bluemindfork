/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.eas.backend.bm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.IOwnerSubscriptions;
import net.bluemind.core.container.model.ChangeLogEntry;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.eas.api.Account;
import net.bluemind.eas.api.FolderSyncVersions;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.FolderChangeReference;
import net.bluemind.eas.backend.FolderChanges;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.SyncFolder;
import net.bluemind.eas.backend.bm.impl.CoreConnect;
import net.bluemind.eas.dto.base.ChangeType;
import net.bluemind.eas.dto.foldersync.FolderType;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.i18n.labels.I18nLabels;
import net.bluemind.imap.translate.Translate;
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.todolist.api.ITodoUids;

public class FolderBackend extends CoreConnect {

	private final ISyncStorage storage;
	private static final String OTHER_MAILBOXES = "OTHER_MAILBOXES";

	protected FolderBackend(ISyncStorage storage) {
		this.storage = storage;
	}

	/**
	 * @param bs
	 * @param sf
	 */
	public CollectionId createMailFolder(BackendSession bs, HierarchyNode parent, SyncFolder sf) {

		MailboxFolder folder = new MailboxFolder();
		if (parent != null) {
			folder.parentUid = IMailReplicaUids.uniqueId(parent.containerUid);
		}
		folder.name = sf.getDisplayName();
		ItemIdentifier itemId = getIMailboxFoldersService(bs, sf.getParentId()).createBasic(folder);

		long parentId = 0;
		String mailboxUid = bs.getUser().getUid();
		if (sf.getParentId().getSubscriptionId().isPresent()) {
			IOwnerSubscriptions subscriptionsService = getService(bs, IOwnerSubscriptions.class,
					bs.getUser().getDomain(), bs.getUser().getUid());
			ItemValue<ContainerSubscriptionModel> sub = subscriptionsService
					.getCompleteById(sf.getParentId().getSubscriptionId().get());
			mailboxUid = sub.value.owner;
			parentId = sf.getParentId().getSubscriptionId().get();
		}
		IContainersFlatHierarchy flatH = getAdmin0Service(bs, IContainersFlatHierarchy.class, bs.getUser().getDomain(),
				mailboxUid);
		String nodeUid = ContainerHierarchyNode.uidFor(IMailReplicaUids.mboxRecords(itemId.uid),
				IMailReplicaUids.MAILBOX_RECORDS, bs.getUser().getDomain());
		ItemValue<ContainerHierarchyNode> node = flatH.getComplete(nodeUid);

		return CollectionId.of(parentId, Long.toString(node.internalId));

	}

	public boolean deleteMailFolder(BackendSession bs, CollectionId collectionId, String containerUid) {
		IMailboxFolders mboxFolders = getIMailboxFoldersService(bs, collectionId);

		String uniqueId = IMailReplicaUids.uniqueId(containerUid);
		ItemValue<MailboxFolder> folder = mboxFolders.getComplete(uniqueId);

		mboxFolders.deleteById(folder.internalId);

		return true;
	}

	/**
	 * @param bs
	 * @param sf
	 * @return
	 */
	public boolean updateMailFolder(BackendSession bs, HierarchyNode node, CollectionId collectionId,
			String displayName) {

		IMailboxFolders mboxFolders = getIMailboxFoldersService(bs, collectionId);

		String uniqueId = IMailReplicaUids.uniqueId(node.containerUid);
		ItemValue<MailboxFolder> folder = mboxFolders.getComplete(uniqueId);

		folder.value.name = displayName;

		mboxFolders.updateById(folder.internalId, folder.value);

		return true;
	}

	/**
	 * @param bs
	 * @param type
	 * @param sf
	 * @return
	 */
	public CollectionId createFolder(BackendSession bs, ItemDataType type, String displayName) {
		return storage.createFolder(bs, type, displayName);
	}

	/**
	 * @param bs
	 * @param type
	 * @param folder
	 * @return
	 */
	public boolean deleteFolder(BackendSession bs, ItemDataType type, HierarchyNode node) {
		return storage.deleteFolder(bs, type, node);
	}

	/**
	 * @param bs
	 * @param folder
	 * @param sf
	 * @return
	 */
	public boolean updateFolder(BackendSession bs, ItemDataType type, HierarchyNode node, String displayName) {
		return storage.updateFolder(bs, type, node, displayName);
	}

	/**
	 * @param bs
	 * @return
	 */
	public FolderChanges getChanges(BackendSession bs, SyncState state) throws Exception {

		Account account = Account.create(bs.getUser().getUid(), bs.getDevId());

		IContainersFlatHierarchy flatH = getService(bs, IContainersFlatHierarchy.class, bs.getUser().getDomain(),
				bs.getUser().getUid());

		FolderChanges ret = new FolderChanges();

		List<String> acceptedContainers = new ArrayList<String>();
		acceptedContainers.add(ICalendarUids.TYPE);
		acceptedContainers.add(IAddressBookUids.TYPE);
		acceptedContainers.add(ITodoUids.TYPE);

		CyrusPartition part = CyrusPartition.forServerAndDomain(bs.getUser().getDataLocation(),
				bs.getUser().getDomain());
		IMailboxFolders mboxFolders = getService(bs, IMailboxFolders.class, part.name,
				"user." + bs.getUser().getUid().replace('.', '^'));

		ContainerChangeset<ItemVersion> changes = flatH.filteredChangesetById(state.version,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));

		IOwnerSubscriptions subscriptionsService = getService(bs, IOwnerSubscriptions.class, bs.getUser().getDomain(),
				bs.getUser().getUid());

		List<String> offlineContainers = subscriptionsService.list().stream()
				.filter(container -> container.value.offlineSync).map(c -> c.value.containerUid)
				.collect(Collectors.toList());

		if (!changes.created.isEmpty()) {
			List<ItemValue<ContainerHierarchyNode>> created = flatH
					.getMultipleById(changes.created.stream().map(f -> f.id).collect(Collectors.toList()));
			created.forEach(h -> {
				if (acceptedContainers.contains(h.value.containerType)) {
					FolderChangeReference f = getHierarchyItemChange(bs, h, ChangeType.ADD, offlineContainers);
					Optional.ofNullable(f).ifPresent(item -> ret.items.add(item));
				} else if (IMailReplicaUids.MAILBOX_RECORDS.equals(h.value.containerType)) {
					FolderChangeReference f = getMailHierarchyItemChange(bs, mboxFolders, flatH, h, ChangeType.ADD);
					Optional.ofNullable(f).ifPresent(item -> ret.items.add(item));
				}
			});
		}

		if (!changes.updated.isEmpty()) {
			List<ItemValue<ContainerHierarchyNode>> updated = flatH
					.getMultipleById(changes.updated.stream().map(f -> f.id).collect(Collectors.toList()));
			updated.forEach(h -> {
				if (acceptedContainers.contains(h.value.containerType)) {
					FolderChangeReference f = getHierarchyItemChange(bs, h, ChangeType.CHANGE, offlineContainers);
					Optional.ofNullable(f).ifPresent(item -> ret.items.add(item));
				} else if (IMailReplicaUids.MAILBOX_RECORDS.equals(h.value.containerType)) {
					FolderChangeReference f = getMailHierarchyItemChange(bs, mboxFolders, flatH, h, ChangeType.CHANGE);
					Optional.ofNullable(f).ifPresent(item -> ret.items.add(item));
				}
			});
		}

		changes.deleted.forEach(itemVersion -> {
			FolderChangeReference f = getDeletedItemChange(Long.toString(itemVersion.id));
			ret.items.add(f);
		});

		ret.version = changes.version;

		final Map<String, String> subscribedMailboxVersions = new HashMap<>();

		// OTHER MAILBOXES
		if (state.version == 0) {
			FolderChangeReference otherMailboxes = new FolderChangeReference();
			otherMailboxes.displayName = getTranslatedDisplayName(bs, OTHER_MAILBOXES);
			otherMailboxes.changeType = ChangeType.ADD;
			otherMailboxes.parentId = "0";
			otherMailboxes.folderId = OTHER_MAILBOXES;
			otherMailboxes.itemType = FolderType.USER_CREATED_EMAIL_FOLDER;
			ret.items.add(otherMailboxes);
		} else {
			// fetch changes from subscribed mailbox
			Map<String, String> mailboxVersion = storage.getFolderSyncVersions(account);
			mailboxVersion.forEach((id, version) -> {
				ItemValue<ContainerSubscriptionModel> container = subscriptionsService
						.getCompleteById(Long.parseLong(id));
				try {
					mailboxSubscriptionChanges(bs, ret, subscribedMailboxVersions, container, Long.parseLong(version));
				} catch (Exception e) {
					logger.warn("Failed to fetch changes for shared mailbox {}", container);
				}
			});
		}

		// owner subscription
		ContainerChangeset<ItemIdentifier> userSubscriptions = subscriptionsService
				.fullChangesetById(state.subscriptionVersion);
		ret.subscriptionVersion = userSubscriptions.version;

		List<ItemValue<ContainerSubscriptionModel>> newUserSubscriptions = subscriptionsService
				.getMultipleById(userSubscriptions.created.stream().map(itemIdentifier -> itemIdentifier.id)
						.collect(Collectors.toList()));

		// new mailbox subscription
		newUserSubscriptions.stream().filter(c -> "mailboxacl".equals(c.value.containerType)).forEach(container -> {
			mailboxSubscriptionChanges(bs, ret, subscribedMailboxVersions, container, 0L);
		});

		newUserSubscriptions.stream().filter(c -> !"mailboxacl".equals(c.value.containerType)).forEach(container -> {
			if (container.value.offlineSync) {
				String nodeUid = ContainerHierarchyNode.uidFor(container.value.containerUid,
						container.value.containerType, bs.getUser().getDomain());

				IContainersFlatHierarchy ownerHierarchy = getAdmin0Service(bs, IContainersFlatHierarchy.class,
						bs.getUser().getDomain(), container.value.owner);

				ItemValue<ContainerHierarchyNode> node = ownerHierarchy.getComplete(nodeUid);
				if (node != null) {
					FolderChangeReference f = getHierarchyItemSubscriptionChange(bs, container, node);
					Optional.ofNullable(f).ifPresent(item -> ret.items.add(item));
				} else {
					logger.warn("[{}] new subscription: no node uid {} for container {} in {} hierarchy. type: {}",
							bs.getUser().getDefaultEmail(), nodeUid, container, container.value.owner,
							container.value.containerType);
				}
			}
		});

		List<ItemValue<ContainerSubscriptionModel>> updatedUserSubscriptions = subscriptionsService
				.getMultipleById(userSubscriptions.updated.stream().map(itemIdentifier -> itemIdentifier.id)
						.collect(Collectors.toList()));
		updatedUserSubscriptions.stream().filter(c -> !"mailboxacl".equals(c.value.containerType))
				.forEach(container -> {
					String nodeUid = ContainerHierarchyNode.uidFor(container.value.containerUid,
							container.value.containerType, bs.getUser().getDomain());

					IContainersFlatHierarchy ownerHierarchy = getAdmin0Service(bs, IContainersFlatHierarchy.class,
							bs.getUser().getDomain(), container.value.owner);

					ItemValue<ContainerHierarchyNode> node = ownerHierarchy.getComplete(nodeUid);
					if (node != null) {
						FolderChangeReference f = null;
						if (container.value.offlineSync) {
							f = getHierarchyItemSubscriptionChange(bs, container, node);
						} else {
							f = getDeletedItemChange(
									CollectionId.of(container.internalId, Long.toString(node.internalId)).getValue());
						}
						Optional.ofNullable(f).ifPresent(item -> ret.items.add(item));
					} else {
						logger.warn("[{}] update subscription: no node uid {} for container {} in hierarchy. type: {}",
								bs.getUser().getDefaultEmail(), nodeUid, container, container.value.containerType);
					}
				});

		if (!userSubscriptions.deleted.isEmpty()) {
			IContainers containers = getService(bs, IContainers.class);
			userSubscriptions.deleted.forEach(itemIdentifier -> {
				String containerUid = itemIdentifier.uid.replace(String.format("sub-of-%s-to-", bs.getUser().getUid()),
						"");
				try {
					ContainerDescriptor cd = containers.get(containerUid);
					if (cd.type.equals("mailboxacl")) {
						// get ContainerSubscriptionModel
						// Remove mailbox subscription
						IContainersFlatHierarchy hierarchyService = getAdmin0Service(bs, IContainersFlatHierarchy.class,
								bs.getUser().getDomain(), cd.owner);
						ContainerChangeset<Long> all = hierarchyService.changesetById(0L);
						all.created.forEach(folderId -> {
							String collectionId = CollectionId.of(itemIdentifier.id, Long.toString(folderId))
									.getValue();
							ret.items.add(getDeletedItemChange(collectionId));
						});
						subscribedMailboxVersions.remove(Long.toString(cd.internalId));
					} else {
						String nodeUid = ContainerHierarchyNode.uidFor(containerUid, cd.type, bs.getUser().getDomain());
						IContainersFlatHierarchy ownerHierarchy = getAdmin0Service(bs, IContainersFlatHierarchy.class,
								bs.getUser().getDomain(), cd.owner);

						ItemValue<ContainerHierarchyNode> h = ownerHierarchy.getComplete(nodeUid);
						if (h != null) {
							FolderChangeReference f = getDeletedItemChange(
									CollectionId.of(cd.internalId, Long.toString(h.internalId)).getValue());
							ret.items.add(f);
						} else {
							logger.warn("[{}] delete subscription: no node uid {} for container {} in hierarchy",
									bs.getUser().getDefaultEmail(), nodeUid, cd);
						}
					}
				} catch (ServerFault sf) {
					if (sf.getCode() == ErrorCode.NOT_FOUND) {
						logger.warn("[{}] delete subscription: container {} not found", bs.getUser().getDefaultEmail(),
								containerUid);
					} else {
						throw sf;
					}

				}
			});
		}

		if (!subscribedMailboxVersions.isEmpty()) {
			storage.setFolderSyncVersions(FolderSyncVersions.create(account, subscribedMailboxVersions));
		}

		return ret;
	}

	private void mailboxSubscriptionChanges(BackendSession bs, FolderChanges ret,
			final Map<String, String> subscribedMailboxVersions, ItemValue<ContainerSubscriptionModel> container,
			long version) {

		String mailboxRoot;
		IDirectory directoryService = getAdmin0Service(bs, IDirectory.class, bs.getUser().getDomain());
		DirEntry dirEntry = directoryService.findByEntryUid(container.value.owner);
		String rootFolderName;
		if (dirEntry.kind == Kind.USER) {
			mailboxRoot = "user." + dirEntry.entryUid.replace('.', '^');
			rootFolderName = "INBOX";
		} else {
			mailboxRoot = dirEntry.entryUid.replace('.', '^');
			rootFolderName = dirEntry.displayName;
		}
		IContainerManagement cmApi = getService(bs, IContainerManagement.class, container.value.containerUid);
		if (!cmApi.canAccess(Arrays.asList(Verb.Read.name()))) {
			logger.info("[{}] skip container {} without Read perms.", bs.getLoginAtDomain(),
					container.value.containerUid);
			return;
		}
		CyrusPartition part = CyrusPartition.forServerAndDomain(dirEntry.dataLocation, bs.getUser().getDomain());
		IMailboxFolders foldersService = getService(bs, IMailboxFolders.class, part.name, mailboxRoot);

		IContainersFlatHierarchy hierarchyService = getAdmin0Service(bs, IContainersFlatHierarchy.class,
				bs.getUser().getDomain(), container.value.owner);
		ContainerChangeset<Long> changes = hierarchyService.changesetById(version);

		// Fetch root folder
		ItemValue<MailboxFolder> folder = foldersService.all().stream()
				.filter(f -> f.value.parentUid == null && f.value.fullName.equals(rootFolderName)).findFirst().get();

		String cont = IMailReplicaUids.mboxRecords(folder.uid);
		String hNodeUid = ContainerHierarchyNode.uidFor(cont, IMailReplicaUids.MAILBOX_RECORDS,
				bs.getUser().getDomain());

		ItemValue<ContainerHierarchyNode> root = hierarchyService.getComplete(hNodeUid);

		// FIXME toString
		subscribedMailboxVersions.put(Long.toString(container.internalId), Long.toString(changes.version));

		List<ItemValue<ContainerHierarchyNode>> created = hierarchyService.getMultipleById(changes.created);
		ret.items.addAll(sharedMailboxHierarchyChange(bs, container, hierarchyService, foldersService, created,
				ChangeType.ADD, dirEntry, version, root));

		List<ItemValue<ContainerHierarchyNode>> updated = hierarchyService.getMultipleById(changes.updated);
		ret.items.addAll(sharedMailboxHierarchyChange(bs, container, hierarchyService, foldersService, updated,
				ChangeType.CHANGE, dirEntry, version, root));

		changes.deleted.forEach(folderId -> {
			String collectionId = CollectionId.of(container.internalId, Long.toString(folderId)).getValue();
			ret.items.add(getDeletedItemChange(collectionId));
		});
	}

	private LinkedHashSet<FolderChangeReference> sharedMailboxHierarchyChange(BackendSession bs,
			ItemValue<ContainerSubscriptionModel> container, IContainersFlatHierarchy sharedMboxFlatHierarchy,
			IMailboxFolders shadedMboxFolders, List<ItemValue<ContainerHierarchyNode>> items, ChangeType changeType,
			DirEntry dirEntry, long version, ItemValue<ContainerHierarchyNode> rootFolder) {

		LinkedHashSet<FolderChangeReference> ret = new LinkedHashSet<>();

		String root = CollectionId.of(container.internalId, Long.toString(rootFolder.internalId)).getValue();

		if (changeType == ChangeType.ADD && version == 0) {
			FolderChangeReference folder = new FolderChangeReference();
			folder.displayName = dirEntry.displayName;
			folder.itemType = FolderType.USER_CREATED_EMAIL_FOLDER;
			folder.folderId = root;
			folder.parentId = OTHER_MAILBOXES;
			folder.changeType = ChangeType.ADD;
			ret.add(folder);
			items.removeIf(item -> item.internalId == rootFolder.internalId);
		}

		items.stream().filter(item -> IMailReplicaUids.MAILBOX_RECORDS.equals(item.value.containerType))
				.forEach(item -> {
					FolderChangeReference folder = getMailHierarchyItemChange(bs, shadedMboxFolders,
							sharedMboxFlatHierarchy, item, changeType);
					if (folder.parentId.equals("0")) {
						folder.parentId = root;
					} else {
						folder.parentId = CollectionId.of(container.internalId, folder.parentId).getValue();
					}
					folder.folderId = CollectionId.of(container.internalId, folder.folderId).getValue();
					folder.itemType = FolderType.USER_CREATED_EMAIL_FOLDER;
					ret.add(folder);
				});
		return ret;
	}

	private FolderChangeReference getDeletedItemChange(String folderId) {
		FolderChangeReference deleted = new FolderChangeReference();
		deleted.folderId = folderId;
		deleted.changeType = ChangeType.DELETE;
		return deleted;
	}

	private FolderChangeReference getMailHierarchyItemChange(BackendSession bs, IMailboxFolders mboxFolders,
			IContainersFlatHierarchy flatH, ItemValue<ContainerHierarchyNode> h, ChangeType changeType) {

		FolderChangeReference folderChangeRef = new FolderChangeReference();
		String dn = h.displayName;

		long parentId = 0;

		String uniqueId = IMailReplicaUids.uniqueId(h.value.containerUid);
		ItemValue<MailboxFolder> folder = mboxFolders.getComplete(uniqueId);
		if (folder == null || folder.value == null) {
			logger.error("Fail to fetch folder {}", uniqueId);
			return null;
		}
		if (folder.flags.contains(ItemFlag.Deleted)) {
			logger.error("Mail folder '{}' is marked as deleted", h.value.name);
			return null;
		}
		if (folder.value.parentUid != null) {
			String parentUid = ContainerHierarchyNode.uidFor(IMailReplicaUids.mboxRecords(folder.value.parentUid),
					IMailReplicaUids.MAILBOX_RECORDS, bs.getUser().getDomain());
			parentId = flatH.getComplete(parentUid).internalId;
		}

		if (parentId == 0) {
			switch (h.displayName) {
			case "INBOX":
				folderChangeRef.itemType = FolderType.DEFAULT_INBOX_FOLDER;
				dn = getTranslatedDisplayName(bs, h.displayName);
				break;
			case "Sent":
				folderChangeRef.itemType = FolderType.DEFAULT_SENT_EMAIL_FOLDER;
				dn = getTranslatedDisplayName(bs, h.displayName);
				break;
			case "Drafts":
				folderChangeRef.itemType = FolderType.DEFAULT_DRAFTS_FOLDERS;
				dn = getTranslatedDisplayName(bs, h.displayName);
				break;
			case "Trash":
				folderChangeRef.itemType = FolderType.DEFAULT_DELETED_ITEMS_FOLDERS;
				dn = getTranslatedDisplayName(bs, h.displayName);
				break;
			case "Outbox":
				folderChangeRef.itemType = FolderType.DEFAULT_OUTBOX_FOLDER;
				dn = getTranslatedDisplayName(bs, h.displayName);
				break;
			default:
				folderChangeRef.itemType = FolderType.USER_CREATED_EMAIL_FOLDER;
				break;
			}
		} else {
			if (Translate.isTranslated(bs.getLang(), dn)) {
				logger.error("Folder '{}' conflicts with system folder, skip it", dn);
				return null;
			}
			folderChangeRef.itemType = FolderType.USER_CREATED_EMAIL_FOLDER;
		}

		folderChangeRef.displayName = dn;
		folderChangeRef.changeType = changeType;
		folderChangeRef.parentId = Long.toString(parentId);
		folderChangeRef.folderId = Long.toString(h.internalId);
		logger.debug("Add mail folder {} {}", changeType, folderChangeRef);

		return folderChangeRef;
	}

	private String getTranslatedDisplayName(BackendSession bs, String displayName) {
		String dn = UTF7Converter.decode(Translate.toUser(bs.getLang(), displayName));
		if (dn.startsWith("\"")) {
			return dn.substring(1, dn.length() - 1);
		}
		return dn;
	}

	private FolderChangeReference getHierarchyItemChange(BackendSession bs, ItemValue<ContainerHierarchyNode> folder,
			ChangeType changeType, List<String> offlineContainers) {
		FolderChangeReference f = new FolderChangeReference();

		String name = "";
		FolderType type = null;
		switch (folder.value.containerType) {
		case ICalendarUids.TYPE:
			if (ICalendarUids.defaultUserCalendar(bs.getUser().getUid()).equals(folder.value.containerUid)) {
				type = FolderType.DEFAULT_CALENDAR_FOLDER;
			} else {
				if (!bs.isMultiCal()) {
					logger.info("[{}] no multi cal, skip folder {}, uid {}", bs.getDeviceId().getUniqueIdentifier(),
							folder.displayName, folder.uid);
					return null;
				}
				if (!offlineContainers.contains(folder.value.containerUid)) {
					logger.info("[{}] no offline sync for folder {}, uid {}", bs.getDeviceId().getUniqueIdentifier(),
							folder.displayName, folder.uid);
					return null;
				}
				type = FolderType.USER_CREATED_CALENDAR_FOLDER;
			}
			name = folder.value.name;
			break;
		case IAddressBookUids.TYPE:
			if (IAddressBookUids.defaultUserAddressbook(bs.getUser().getUid()).equals(folder.value.containerUid)) {
				type = FolderType.DEFAULT_CONTACTS_FOLDER;
			} else {
				if (!bs.isMultiAB()) {
					logger.info("[{}] no multi ab, skip folder {}, uid {}", bs.getDeviceId().getUniqueIdentifier(),
							folder.displayName, folder.uid);
					return null;
				}
				if (!offlineContainers.contains(folder.value.containerUid)) {
					logger.info("[{}] no offline sync for folder {}, uid {}", bs.getDeviceId().getUniqueIdentifier(),
							folder.displayName, folder.uid);
					return null;
				}
				type = FolderType.USER_CREATED_CONTACTS_FOLDER;
			}
			name = I18nLabels.getInstance().translate(bs.getLang(), folder.value.name);
			break;
		case ITodoUids.TYPE:
			if (ITodoUids.defaultUserTodoList(bs.getUser().getUid()).equals(folder.value.containerUid)) {
				type = FolderType.DEFAULT_TASKS_FOLDER;
			} else {
				type = FolderType.USER_CREATED_TASKS_FOLDER;
			}
			name = I18nLabels.getInstance().translate(bs.getLang(), folder.value.name);
			break;
		default:
			break;
		}

		f.displayName = name;
		f.itemType = type;
		f.changeType = changeType;
		f.parentId = "0";
		f.folderId = Long.toString(folder.internalId);

		logger.debug("Add folder {} {}", changeType, f);

		return f;
	}

	private FolderChangeReference getHierarchyItemSubscriptionChange(BackendSession bs,
			ItemValue<ContainerSubscriptionModel> container, ItemValue<ContainerHierarchyNode> h) {
		FolderChangeReference f = new FolderChangeReference();

		String name = "";
		FolderType type = null;
		String folderId = Long.toString(h.internalId);
		switch (container.value.containerType) {
		case ICalendarUids.TYPE:
			if (ICalendarUids.defaultUserCalendar(bs.getUser().getUid()).equals(container.value.containerUid)) {
				type = FolderType.DEFAULT_CALENDAR_FOLDER;
			} else {
				if (!bs.isMultiCal()) {
					logger.info("[{}] no multi cal, skip folder {}, uid {}", bs.getDeviceId().getUniqueIdentifier(),
							h.value.name, h.uid);
					return null;
				}
				type = FolderType.USER_CREATED_CALENDAR_FOLDER;
				if (!bs.getUser().getUid().equals(container.value.owner)) {
					folderId = CollectionId.of(container.internalId, folderId).getValue();
				}
			}
			name = h.value.name;
			break;
		case IAddressBookUids.TYPE:
			if (IAddressBookUids.defaultUserAddressbook(bs.getUser().getUid()).equals(container.value.containerUid)) {
				type = FolderType.DEFAULT_CONTACTS_FOLDER;
			} else {
				if (!bs.isMultiAB()) {
					logger.info("[{}] no multi ab, skip folder {}, uid {}", bs.getDeviceId().getUniqueIdentifier(),
							h.value.name, h.uid);
					return null;
				}
				type = FolderType.USER_CREATED_CONTACTS_FOLDER;
				if (!bs.getUser().getUid().equals(container.value.owner)) {
					folderId = CollectionId.of(container.internalId, folderId).getValue();
				}
			}
			name = I18nLabels.getInstance().translate(bs.getLang(), h.value.name);
			break;
		case ITodoUids.TYPE:
			if (ITodoUids.defaultUserTodoList(bs.getUser().getUid()).equals(container.value.containerUid)) {
				type = FolderType.DEFAULT_TASKS_FOLDER;
			} else {
				type = FolderType.USER_CREATED_TASKS_FOLDER;
				if (!bs.getUser().getUid().equals(container.value.owner)) {
					folderId = CollectionId.of(container.internalId, folderId).getValue();
				}
			}
			name = I18nLabels.getInstance().translate(bs.getLang(), h.value.name);
			break;
		default:
			break;
		}

		f.displayName = name;
		f.itemType = type;
		f.changeType = ChangeType.ADD;
		f.parentId = "0";
		f.folderId = folderId;

		logger.debug("Add subscription {} {}", f.changeType, f);

		return f;
	}

	public HierarchyNode getHierarchyNode(BackendSession bs, CollectionId collectionId) throws ActiveSyncException {
		return storage.getHierarchyNode(bs, collectionId);
	}
}
