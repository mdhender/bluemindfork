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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.IOwnerSubscriptions;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
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

	protected FolderBackend(ISyncStorage storage) {
		this.storage = storage;
	}

	/**
	 * @param bs
	 * @param sf
	 */
	public Long createMailFolder(BackendSession bs, HierarchyNode parent, SyncFolder sf) {

		IMailboxFolders mboxFolders = getIMailboxFoldersService(bs);

		MailboxFolder folder = new MailboxFolder();
		if (parent != null) {
			folder.parentUid = IMailReplicaUids.uniqueId(parent.containerUid);
		}
		folder.name = sf.getDisplayName();
		ItemIdentifier itemId = mboxFolders.createBasic(folder);

		// ContainerNode internalId as collectionId
		IContainersFlatHierarchy flatH = getService(bs, IContainersFlatHierarchy.class, bs.getUser().getDomain(),
				bs.getUser().getUid());
		String nodeUid = ContainerHierarchyNode.uidFor(IMailReplicaUids.mboxRecords(itemId.uid),
				IMailReplicaUids.MAILBOX_RECORDS, bs.getUser().getDomain());
		ItemValue<ContainerHierarchyNode> node = flatH.getComplete(nodeUid);

		return node.internalId;

	}

	public boolean deleteMailFolder(BackendSession bs, HierarchyNode node) {
		IMailboxFolders mboxFolders = getIMailboxFoldersService(bs);

		String uniqueId = IMailReplicaUids.uniqueId(node.containerUid);
		ItemValue<MailboxFolder> folder = mboxFolders.getComplete(uniqueId);

		mboxFolders.deleteById(folder.internalId);

		return true;
	}

	/**
	 * @param bs
	 * @param sf
	 * @return
	 */
	public boolean updateMailFolder(BackendSession bs, HierarchyNode node, String displayName) {

		IMailboxFolders mboxFolders = getIMailboxFoldersService(bs);

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
	public Long createFolder(BackendSession bs, ItemDataType type, String displayName) {
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
		IContainersFlatHierarchy flatH = getService(bs, IContainersFlatHierarchy.class, bs.getUser().getDomain(),
				bs.getUser().getUid());

		FolderChanges ret = new FolderChanges();

		List<String> acceptedContainers = new ArrayList<String>();
		acceptedContainers.add(ICalendarUids.TYPE);
		acceptedContainers.add(IAddressBookUids.TYPE);
		acceptedContainers.add(ITodoUids.TYPE);

		IMailboxFolders mboxFolders = getIMailboxFoldersService(bs);

		ContainerChangeset<ItemVersion> changes = flatH.filteredChangesetById(state.version,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));

		IOwnerSubscriptions subs = getService(bs, IOwnerSubscriptions.class, bs.getUser().getDomain(),
				bs.getUser().getUid());

		List<String> offlineContainers = subs.list().stream().filter(container -> container.value.offlineSync)
				.map(c -> c.value.containerUid).collect(Collectors.toList());

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

		// owner subscription
		ContainerChangeset<String> allSubs = subs.changeset(state.subscriptionVersion);
		ret.subscriptionVersion = allSubs.version;

		List<ItemValue<ContainerSubscriptionModel>> brandNew = subs.getMultiple(allSubs.created);
		brandNew.stream().filter(c -> !"mailboxacl".equals(c.value.containerType)).forEach(container -> {
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

		List<ItemValue<ContainerSubscriptionModel>> brandNewUpdated = subs.getMultiple(allSubs.updated);
		brandNewUpdated.stream().filter(c -> !"mailboxacl".equals(c.value.containerType)).forEach(container -> {
			String nodeUid = ContainerHierarchyNode.uidFor(container.value.containerUid, container.value.containerType,
					bs.getUser().getDomain());

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

		if (!allSubs.deleted.isEmpty()) {
			IContainers containers = getService(bs, IContainers.class);
			allSubs.deleted.forEach(uid -> {
				String containerUid = uid.replace(String.format("sub-of-%s-to-", bs.getUser().getUid()), "");
				try {
					ContainerDescriptor cd = containers.get(containerUid);
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

		switch (h.displayName) {
		case "INBOX":
			folderChangeRef.itemType = FolderType.DEFAULT_INBOX_FOLDER;
			dn = getTranslatedDisplayName(bs, h);
			break;
		case "Sent":
			folderChangeRef.itemType = FolderType.DEFAULT_SENT_EMAIL_FOLDER;
			dn = getTranslatedDisplayName(bs, h);
			break;
		case "Drafts":
			folderChangeRef.itemType = FolderType.DEFAULT_DRAFTS_FOLDERS;
			dn = getTranslatedDisplayName(bs, h);
			break;
		case "Trash":
			folderChangeRef.itemType = FolderType.DEFAULT_DELETED_ITEMS_FOLDERS;
			dn = getTranslatedDisplayName(bs, h);
			break;
		case "Outbox":
			folderChangeRef.itemType = FolderType.DEFAULT_OUTBOX_FOLDER;
			dn = getTranslatedDisplayName(bs, h);
			break;
		default:
			folderChangeRef.itemType = FolderType.USER_CREATED_EMAIL_FOLDER;
			break;
		}

		folderChangeRef.displayName = dn;
		folderChangeRef.changeType = changeType;

		long parentId = 0;
		if (folderChangeRef.itemType == FolderType.USER_CREATED_EMAIL_FOLDER) {

			if (Translate.isTranslated(bs.getLang(), dn)) {
				logger.error("Folder '{}' conflicts with system folder, skip it", dn);
				return null;
			}

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
		}
		folderChangeRef.parentId = Long.toString(parentId);
		folderChangeRef.folderId = Long.toString(h.internalId);

		logger.debug("Add mail folder {} {}", changeType, folderChangeRef);

		return folderChangeRef;
	}

	private String getTranslatedDisplayName(BackendSession bs, ItemValue<ContainerHierarchyNode> h) {
		String dn = UTF7Converter.decode(Translate.toUser(bs.getLang(), h.displayName));
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

	public HierarchyNode getHierarchyNode(BackendSession bs, Integer collectionId) throws ActiveSyncException {
		return storage.getHierarchyNode(bs, collectionId);
	}

}
