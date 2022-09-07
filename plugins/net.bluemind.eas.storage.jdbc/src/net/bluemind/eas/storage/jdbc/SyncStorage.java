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
package net.bluemind.eas.storage.jdbc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.common.task.Tasks;
import net.bluemind.config.Token;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.IOwnerSubscriptions;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.device.api.IDevice;
import net.bluemind.device.api.IDevices;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.eas.api.Account;
import net.bluemind.eas.api.FolderSyncVersions;
import net.bluemind.eas.api.Heartbeat;
import net.bluemind.eas.api.IEas;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.MailFolder;
import net.bluemind.eas.dto.device.DeviceId;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.network.topology.Topology;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.user.api.IUserSubscription;

/**
 * Store device infos, id mappings & last sync dates into BM database
 * 
 */
public class SyncStorage implements ISyncStorage {
	private static final Logger logger = LoggerFactory.getLogger(SyncStorage.class);

	private String locateCore() {
		return Topology.getIfAvailable().map(t -> "http://" + t.core().value.address() + ":8090")
				.orElse("http://127.0.0.1:8090");
	}

	private IServiceProvider admin0Provider() {
		return ClientSideServiceProvider.getProvider(locateCore(), Token.admin0()).setOrigin("bm-eas-syncStorage");
	}

	private ClientSideServiceProvider provider(BackendSession bs) {
		return ClientSideServiceProvider.getProvider(locateCore(), bs.getSid())
				.setOrigin("bm-eas-syncStorage-" + bs.getUniqueIdentifier());
	}

	// SystemConf
	@Override
	public String getSystemConf(String key) {
		return MQ.<String, String>sharedMap("system.configuration").get(key);
	}

	// Device/Auth stuff
	@Override
	public List<String> getWipedDevices() {
		try {
			IDevices service = admin0Provider().instance(IDevices.class);
			return service.listWiped().stream().map(d -> d.identifier).collect(Collectors.toList());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public void updateLastSync(BackendSession bs) {
		try {
			IDevice deviceService = admin0Provider().instance(IDevice.class, bs.getUser().getUid());
			deviceService.updateLastSync(bs.getDeviceId().getInternalId());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	// Heartbeat stuff
	@Override
	public long findLastHeartbeat(DeviceId deviceId) {
		try {
			Heartbeat heartbeat = getService().getHeartbeat(deviceId.getIdentifier());
			if (heartbeat != null) {
				return heartbeat.value;
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return 0L;
	}

	@Override
	public synchronized void updateLastHearbeat(DeviceId deviceId, long value) {
		Heartbeat heartbeat = new Heartbeat();
		heartbeat.deviceUid = deviceId.getIdentifier();
		heartbeat.value = value;
		try {
			getService().setHeartbeat(heartbeat);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public HierarchyNode getHierarchyNode(String origin, String domainUid, String userUid, String nodeUid)
			throws CollectionNotFoundException {
		ItemValue<ContainerHierarchyNode> folder = getIContainersFlatHierarchyService(origin, domainUid, userUid)
				.getComplete(nodeUid);
		if (folder == null) {
			throw new CollectionNotFoundException("Container " + nodeUid + " not found");
		}

		return new HierarchyNode(CollectionId.of(Long.toString(folder.internalId)), folder.value.containerUid,
				folder.value.containerType);
	}

	@Override
	public HierarchyNode getHierarchyNode(BackendSession bs, CollectionId collectionId)
			throws CollectionNotFoundException {
		if (collectionId.getSubscriptionId().isPresent()) {
			IOwnerSubscriptions subs = provider(bs).instance(IOwnerSubscriptions.class, bs.getUser().getDomain(),
					bs.getUser().getUid());
			ItemValue<ContainerSubscriptionModel> subscription = subs
					.getCompleteById(collectionId.getSubscriptionId().get());
			return getHierarchyNode(bs, subscription.value.owner, collectionId);
		}
		return getHierarchyNode(bs, bs.getUser().getUid(), collectionId);

	}

	private HierarchyNode getHierarchyNode(BackendSession bs, String owner, CollectionId collectionId)
			throws CollectionNotFoundException {

		// FIXME
		if (owner == null) {
			owner = bs.getUser().getUid();
		}

		ItemValue<ContainerHierarchyNode> folder = getIContainersFlatHierarchyService(bs.getUniqueIdentifier(),
				bs.getUser().getDomain(), owner).getCompleteById(collectionId.getFolderId());

		if (folder == null) {
			throw new CollectionNotFoundException("Collection " + owner + ":" + collectionId + " not found");
		}

		long subscriptionId = collectionId.getSubscriptionId().isPresent() ? collectionId.getSubscriptionId().get() : 0;
		return new HierarchyNode(CollectionId.of(subscriptionId, Long.toString(folder.internalId)),
				folder.value.containerUid, folder.value.containerType);
	}

	@Override
	public MailFolder getMailFolder(BackendSession bs, CollectionId collectionId) throws CollectionNotFoundException {
		HierarchyNode folder = getHierarchyNode(bs, collectionId);
		String uniqueId = IMailReplicaUids.uniqueId(folder.containerUid);
		ItemValue<MailboxFolder> mailFolder = getIMailboxFoldersService(bs, collectionId).getComplete(uniqueId);

		if (mailFolder == null) {
			throw new CollectionNotFoundException("Collection " + collectionId + " not found");
		}

		return new MailFolder(collectionId, mailFolder.uid, mailFolder.value.name, mailFolder.value.fullName,
				mailFolder.value.parentUid);
	}

	@Override
	public MailFolder getMailFolderByName(BackendSession bs, String name) throws CollectionNotFoundException {
		ItemValue<MailboxFolder> mailFolder = getIMailboxFoldersService(bs).byName(name);
		if (mailFolder == null) {
			throw new CollectionNotFoundException("mailbox '" + name + "' not found");
		}
		String cont = IMailReplicaUids.mboxRecords(mailFolder.uid);
		String hNodeUid = ContainerHierarchyNode.uidFor(cont, IMailReplicaUids.MAILBOX_RECORDS,
				bs.getUser().getDomain());
		ItemValue<ContainerHierarchyNode> hNode = getIContainersFlatHierarchyService(bs.getUniqueIdentifier(),
				bs.getUser().getDomain(), bs.getUser().getUid()).getComplete(hNodeUid);
		if (hNode == null) {
			throw new CollectionNotFoundException("mailbox '" + name + "' not found");
		}
		return new MailFolder(CollectionId.of(Long.toString(hNode.internalId)), mailFolder.uid, mailFolder.value.name,
				mailFolder.value.fullName, mailFolder.value.parentUid);
	}

	@Override
	public CollectionId createFolder(BackendSession bs, ItemDataType type, String folderName) {
		Long folderUid = null;
		try {
			String uid = UUID.randomUUID().toString();
			if (type == ItemDataType.TASKS || type == ItemDataType.CALENDAR) {
				ContainerDescriptor descriptor = null;
				String containerNodeUid = null;
				if (type == ItemDataType.TASKS) {
					descriptor = ContainerDescriptor.create(uid, folderName, bs.getUser().getUid(), "todolist",
							bs.getUser().getDomain(), false);
					containerNodeUid = ContainerHierarchyNode.uidFor(uid, "todolist", bs.getUser().getDomain());

				} else {
					descriptor = ContainerDescriptor.create(uid, folderName, bs.getUser().getUid(), "calendar",
							bs.getUser().getDomain(), false);
					containerNodeUid = ContainerHierarchyNode.uidFor(uid, "calendar", bs.getUser().getDomain());

				}

				IContainers containers = admin0Provider().instance(IContainers.class);
				containers.create(uid, descriptor);
				IContainerManagement manager = admin0Provider().instance(IContainerManagement.class, uid);
				manager.setAccessControlList(
						Arrays.asList(AccessControlEntry.create(bs.getUser().getUid(), Verb.Write)));

				IUserSubscription userSubService = admin0Provider().instance(IUserSubscription.class,
						bs.getUser().getDomain());
				userSubService.subscribe(bs.getUser().getUid(),
						Arrays.asList(ContainerSubscription.create(uid, false)));

				ItemValue<ContainerHierarchyNode> folder = getIContainersFlatHierarchyService(bs.getUniqueIdentifier(),
						bs.getUser().getDomain(), bs.getUser().getUid()).getComplete(containerNodeUid);

				folderUid = folder.internalId;
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return CollectionId.of(Long.toString(folderUid));

	}

	@Override
	public boolean deleteFolder(BackendSession bs, ItemDataType type, HierarchyNode node) {
		boolean ret = false;
		ClientSideServiceProvider cssp = provider(bs);

		IContainers containers = cssp.instance(IContainers.class);

		try {
			ContainerDescriptor container = containers.get(node.containerUid);

			if (type == ItemDataType.TASKS) {
				if (container.defaultContainer) {
					logger.warn("Cannot delete default todolist {}", node.containerUid);
					return true;
				}

				ITodoList service = cssp.instance(ITodoList.class, node.containerUid);
				// ITodoList.reset should return a TaskRef.
				// see BM-13104
				service.reset();
			} else if (type == ItemDataType.CALENDAR) {
				if (container.defaultContainer) {
					logger.warn("Cannot delete default calendar {}", node.containerUid);
					return true;
				}

				ICalendar service = cssp.instance(ICalendar.class, node.containerUid);
				TaskRef tr = service.reset();
				Tasks.followStream(cssp, logger, null, tr);
			}

			containers.delete(node.containerUid);

			ret = true;
		} catch (Exception e) {
			logger.error("Fail to delete folder {}", node.containerUid, e);
		}
		return ret;
	}

	@Override
	public boolean updateFolder(BackendSession bs, ItemDataType type, HierarchyNode node, String folderName) {
		boolean ret = false;

		try {
			IContainers containers = provider(bs).instance(IContainers.class);
			if (type == ItemDataType.CALENDAR || type == ItemDataType.TASKS) {
				ContainerDescriptor container = containers.get(node.containerUid);
				if (container.defaultContainer) {
					logger.warn("Cannot update default {} {}", type, node.containerUid);
					return true;
				}
			}

			ContainerModifiableDescriptor descriptor = new ContainerModifiableDescriptor();
			descriptor.name = folderName;
			descriptor.defaultContainer = false;

			containers.update(node.containerUid, descriptor);

			ret = true;
		} catch (Exception e) {
			logger.error("Fail to update folder {}", node.containerUid, e);
		}
		return ret;
	}

	// FolderState
	@Override
	public boolean needReset(BackendSession bs) {
		boolean ret = false;
		try {
			ret = getService().needReset(Account.create(bs.getUser().getUid(), bs.getDeviceId().getIdentifier()));

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return ret;
	}

	@Override
	public void resetFolder(BackendSession bs) {
		try {
			getService().deletePendingReset(Account.create(bs.getUser().getUid(), bs.getDeviceId().getIdentifier()));

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	@Override
	public void insertClientId(String clientId) {
		try {
			getService().insertClientId(clientId);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public boolean isKnownClientId(String clientId) {
		boolean ret = true;
		try {
			ret = getService().isKnownClientId(clientId);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return ret;
	}

	@Override
	public void setFolderSyncVersions(FolderSyncVersions versions) {
		try {
			getService().setFolderSyncVersions(versions);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	@Override
	public Map<String, String> getFolderSyncVersions(Account account) {
		try {
			return getService().getFolderSyncVersions(account);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * @param authKey
	 * @return
	 */
	private IEas getService() {
		return admin0Provider().instance(IEas.class);
	}

	private IContainersFlatHierarchy getIContainersFlatHierarchyService(String origin, String domainUid, String owner) {
		return ClientSideServiceProvider.getProvider(locateCore(), Token.admin0())
				.setOrigin("bm-eas-syncStorage-" + origin).instance(IContainersFlatHierarchy.class, domainUid, owner);
	}

	private IMailboxFolders getIMailboxFoldersService(BackendSession bs, CollectionId collectionId) {
		CyrusPartition part = CyrusPartition.forServerAndDomain(bs.getUser().getDataLocation(),
				bs.getUser().getDomain());
		String mailboxRoot = "user." + bs.getUser().getUid().replace('.', '^');
		if (collectionId.getSubscriptionId().isPresent()) {
			IOwnerSubscriptions subscriptionsService = provider(bs).instance(IOwnerSubscriptions.class,
					bs.getUser().getDomain(), bs.getUser().getUid());
			ItemValue<ContainerSubscriptionModel> subscription = subscriptionsService
					.getCompleteById(collectionId.getSubscriptionId().get());

			IDirectory directoryService = admin0Provider().instance(IDirectory.class, bs.getUser().getDomain());
			DirEntry dirEntry = directoryService.findByEntryUid(subscription.value.owner);
			if (dirEntry.kind == Kind.USER) {
				mailboxRoot = "user." + dirEntry.entryUid.replace('.', '^');
			} else {
				mailboxRoot = dirEntry.entryUid.replace('.', '^');
			}
			part = CyrusPartition.forServerAndDomain(dirEntry.dataLocation, bs.getUser().getDomain());
		}

		return provider(bs).instance(IMailboxFolders.class, part.name, mailboxRoot);
	}

	private IMailboxFolders getIMailboxFoldersService(BackendSession bs) {
		CyrusPartition part = CyrusPartition.forServerAndDomain(bs.getUser().getDataLocation(),
				bs.getUser().getDomain());
		// FIXME
		// FIXME
		// FIXME
		// FIXME
		// FIXME
		return provider(bs).instance(IMailboxFolders.class, part.name,
				"user." + bs.getUser().getUid().replace('.', '^'));
	}

}
