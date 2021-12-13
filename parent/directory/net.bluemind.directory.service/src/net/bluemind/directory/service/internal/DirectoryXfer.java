/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.directory.service.internal;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationArtifacts;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IInternalMailConversation;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.api.IOwnerSubscriptionUids;
import net.bluemind.core.container.api.IOwnerSubscriptions;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ChangelogStore;
import net.bluemind.core.container.persistence.ContainerPersonalSettingsStore;
import net.bluemind.core.container.persistence.ContainerSettingsStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ContainerSyncStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.deferredaction.api.IDeferredAction;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirEntryMaintenance;
import net.bluemind.directory.service.IInCoreDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.exchange.mapi.api.IMapiFolder;
import net.bluemind.exchange.mapi.api.IMapiFolderAssociatedInformation;
import net.bluemind.exchange.mapi.api.MapiFAIContainer;
import net.bluemind.exchange.mapi.api.MapiFolderContainer;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxMgmt;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.hook.IMailboxHook;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.ITags;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoUids;

public class DirectoryXfer implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(DirectoryXfer.class);

	private final String domainUid;
	private final String targetServerUid;
	private final TransactionalContext transactionalContext;
	private final BmContext context;

	private final ItemValue<Server> targetServer;
	private final ItemValue<DirEntry> dirEntry;

	private final ContainerStore dirContainerStore;
	private final ContainerStore containerStoreOrig;
	private final ContainerStore containerStoreTarget;

	private final DataSource directoryDs;
	private final DataSource origDs;
	private final DataSource targetDs;

	private static final List<IMailboxHook> hooks = getMailboxHooks();

	public DirectoryXfer(BmContext context, ItemValue<Domain> domain, DirEntryStoreService itemStore, String entryUid,
			String serverUid) {
		this.domainUid = domain.uid;
		this.context = context;
		this.targetServer = ServerSideServiceProvider.getProvider(context).instance(IServer.class, "default")
				.getComplete(serverUid);
		this.dirEntry = itemStore.get(entryUid, null);
		if (dirEntry == null) {
			throw ServerFault.notFound("directory entry " + entryUid + " not found");
		}
		this.targetServerUid = serverUid;

		transactionalContext = new TransactionalContext(context.getSecurityContext());

		this.directoryDs = transactionalContext.getDataSource();
		this.origDs = transactionalContext.getMailboxDataSource(dirEntry.value.dataLocation);
		this.targetDs = transactionalContext.getMailboxDataSource(serverUid);
		this.dirContainerStore = new ContainerStore(transactionalContext, transactionalContext.getDataSource(),
				transactionalContext.getSecurityContext());
		this.containerStoreOrig = new ContainerStore(null, origDs, transactionalContext.getSecurityContext());
		this.containerStoreTarget = new ContainerStore(null, targetDs, transactionalContext.getSecurityContext());
	}

	private void commitAll() throws SQLException {
		logger.info("commit connections: directory:{} origin datasource: {} target datasource: {}",
				directoryDs.getConnection(), origDs.getConnection(), targetDs.getConnection());
		// Warning: the order is important, some constraints can fail to commit, so
		// directoryDs
		// MUST be commited last
		origDs.getConnection().commit();
		targetDs.getConnection().commit();
		directoryDs.getConnection().commit();
	}

	private void rollbackAll() {
		try {
			logger.info("rollback connections: directory:{} origin datasource: {} target datasource: {}",
					directoryDs.getConnection(), origDs.getConnection(), targetDs.getConnection());
		} catch (SQLException e) {
		}
		try {
			directoryDs.getConnection().rollback();
		} catch (SQLException e) {
		}
		try {
			origDs.getConnection().rollback();
		} catch (SQLException e) {
		}
		try {
			targetDs.getConnection().rollback();
		} catch (SQLException e) {
		}
		CacheRegistry.get().invalidateAll();
	}

	public void doXfer(String entryUid, IServerTaskMonitor monitor) {
		logger.warn("transfer {} from datasource {} ({}) to datasource {} ({}", dirEntry.displayName, origDs,
				dirEntry.value.dataLocation, targetDs, targetServerUid);

		if (targetServer == null) {
			logger.error("fail to transfert data. entryUid {}, serverUid {}. Server not found", entryUid,
					targetServerUid);
			monitor.end(false, "destination server not found", "{}");
			return;
		}

		if (dirEntry.value.kind != BaseDirEntry.Kind.USER) {
			logger.error("fail to transfert data. entryUid {}, serverUid {}. Unsupported kind {}", entryUid,
					targetServerUid, dirEntry.value.kind);
			monitor.end(false, "source is not a user", "{}");
			return;
		}

		IServiceProvider sp = ServerSideServiceProvider.getProvider(transactionalContext);
		ItemValue<Mailbox> mailbox = sp.instance(IMailboxes.class, domainUid).getComplete(entryUid);

		try {
			monitor.begin(12, "moving containers");

			doPreMove(mailbox);
			doXferContainers(entryUid, mailbox, monitor);

			doXferCyrusArtifacts(entryUid, domainUid, mailbox, monitor);

			// We must set the dataLocation before even trying to move
			// the data, because otherwise, the replication service will try to access
			// the mailbox using the wrong location, and will take the wrong decisions
			dirEntry.value.dataLocation = targetServerUid;
			sp.instance(IInCoreDirectory.class, domainUid).update(entryUid, dirEntry.value);

			commitAll();

			/* The transactional context is not used anymore here */
			doXferMailbox(entryUid, mailbox, monitor);
			doPostMove(mailbox);
			if (mailbox != null) {
				VertxPlatform.eventBus().publish("postfix.map.dirty", new JsonObject());
			}
			monitor.end(true, "user transfered", "{}");
			logger.info("Ending xfer of {} to {}", entryUid, targetServerUid);

		} catch (Exception e) {
			logger.error("transfer failed: {}", e.getMessage(), e);
			rollbackAll();
			monitor.end(false, "transfer failed: " + e, null);
			throw new ServerFault(e);
		}

	}

	private void doXferCyrusArtifacts(String entryUid, String domainUid, ItemValue<Mailbox> mailbox,
			IServerTaskMonitor monitor) {
		String userId = mailbox.value.name + "@" + domainUid;
		IServiceProvider sp = ServerSideServiceProvider.getProvider(transactionalContext);
		ICyrusReplicationArtifacts cyrusArtifcatsService = sp.instance(ICyrusReplicationArtifacts.class, userId);
		cyrusArtifcatsService.xfer(targetServerUid);
	}

	private void doXferContainers(String entryUid, ItemValue<Mailbox> mailbox, IServerTaskMonitor monitor)
			throws SQLException {
		logger.info("[{}] xfer cleanup containers on target", dirEntry.uid);
		monitor.progress(0, "removing stale containers on target");
		List<Container> toRemoveContainers = containerStoreTarget.findByTypeOwnerReadOnly(null, dirEntry.uid, null);
		cleanupTargetContainers(toRemoveContainers);

		IServiceProvider sp = ServerSideServiceProvider.getProvider(transactionalContext);
		xferContainers(monitor.subWork(1), IAddressBookUids.TYPE,
				containerUid -> sp.instance(IAddressBook.class, containerUid));
		xferContainers(monitor.subWork(1), ICalendarUids.TYPE,
				containerUid -> sp.instance(ICalendar.class, containerUid));
		xferContainers(monitor.subWork(1), IDeferredActionContainerUids.TYPE,
				containerUid -> sp.instance(IDeferredAction.class, containerUid));
		xferContainers(monitor.subWork(1), ITodoUids.TYPE, containerUid -> sp.instance(ITodoList.class, containerUid));
		xferContainers(monitor.subWork(1), ITagUids.TYPE, containerUid -> sp.instance(ITags.class, containerUid));
		xferContainers(monitor.subWork(1), IFlatHierarchyUids.TYPE,
				containerUid -> sp.instance(IContainersFlatHierarchy.class, domainUid, entryUid));
		xferContainers(monitor.subWork(1), IOwnerSubscriptionUids.TYPE,
				containerUid -> sp.instance(IOwnerSubscriptions.class, domainUid, entryUid));
		// Theses containers can't be migrated: item_id are changed, so all the data is
		// useless and lost
		xferContainers(monitor.subWork(1), IMailReplicaUids.MAILBOX_RECORDS,
				containerUid -> sp.instance(IDbMailboxRecords.class, IMailReplicaUids.uniqueId(containerUid)));
		xferContainers(monitor.subWork(1), IMailReplicaUids.REPLICATED_CONVERSATIONS,
				containerUid -> sp.instance(IInternalMailConversation.class, containerUid));
		xferContainers(monitor.subWork(1), MapiFolderContainer.TYPE,
				containerUid -> sp.instance(IMapiFolder.class, containerUid));
		xferContainers(monitor.subWork(1), MapiFAIContainer.TYPE, containerUid -> sp
				.instance(IMapiFolderAssociatedInformation.class, MapiFAIContainer.getIdentifier(containerUid)));
		if (mailbox != null) {
			xferContainers(monitor.subWork(1), IMailReplicaUids.REPLICATED_MBOXES, containerUid -> {
				CyrusPartition part = CyrusPartition.forServerAndDomain(targetServerUid, domainUid);
				return sp.instance(IDbReplicatedMailboxes.class, part.name, "user." + mailbox.value.name);
			});
		} else {
			logger.info("mailbox is empty, not moving replicated_mboxes");
		}
	}

	private void cleanupTargetContainers(List<Container> containers) {
		for (Container c : containers) {
			logger.info("Try to clean container {}", c);
			logger.info("try to delete container {}", c.uid);
			try {
				new AclStore(transactionalContext, targetDs).deleteAll(c);
				new ContainerSyncStore(targetDs, c).delete();
				new ContainerPersonalSettingsStore(targetDs, transactionalContext.getSecurityContext(), c).deleteAll();
				new ContainerSettingsStore(targetDs, c).delete();
				new ChangelogStore(targetDs, c).deleteLog();
				if (containerStoreTarget.get(c.uid) != null) {
					containerStoreTarget.delete(c.uid);
				}
			} catch (Exception e) {
				logger.warn("containerStore {} cleanup failed: {}", c, e.getMessage(), e);
			}
		}
	}

	private void xferContainers(IServerTaskMonitor monitor, String containerType,
			Function<String, IDataShardSupport> fn) throws SQLException {
		List<Container> containers = containerStoreOrig.findByTypeAndOwner(containerType, dirEntry.uid);
		logger.info("[{}] xfer {} {} ({})", dirEntry.uid, containers.size(), containerType, containers);
		monitor.begin(containers.size(), "processing " + containers.size() + " container(s)");

		for (Container c : containers) {
			logger.info("[{}] xfer container {}", dirEntry.uid, c.uid);

			IDataShardSupport service = fn.apply(c.uid);

			Container oldContainer = containerStoreOrig.get(c.uid);
			Container newContainer = null;
			dirContainerStore.deleteContainerLocation(oldContainer);

//			if (transferData) {
			newContainer = containerStoreTarget.create(c);
			dirContainerStore.createContainerLocation(newContainer, targetServerUid);
			service.xfer(targetServerUid);
//			}

			dirContainerStore.invalidateCache(c.uid, c.id);
			DataSourceRouter.invalidateContainer(c.uid);

			AclStore aclStoreOrig = new AclStore(transactionalContext, origDs);
			ContainerSyncStore containerSyncStoreOrig = new ContainerSyncStore(origDs, oldContainer);
			ContainerPersonalSettingsStore containerPersonalSettingStoreOrig = new ContainerPersonalSettingsStore(
					origDs, transactionalContext.getSecurityContext(), oldContainer);
			ContainerSettingsStore containerSettingStoreOrig = new ContainerSettingsStore(origDs, oldContainer);

			if (newContainer != null) {
				List<AccessControlEntry> acls = aclStoreOrig.get(oldContainer);
				if (acls != null && !acls.isEmpty()) {
					AclStore aclStoreTarget = new AclStore(transactionalContext, targetDs);
					if (newContainer != null) {
						aclStoreTarget.store(newContainer, acls);
					}
				}

				ContainerSyncStatus ss = containerSyncStoreOrig.getSyncStatus();
				if (ss != null) {
					ContainerSyncStore containerSyncStoreTarget = new ContainerSyncStore(targetDs, newContainer);
					containerSyncStoreTarget.setSyncStatus(ss);
				}

				Map<String, String> personnelsettings = containerPersonalSettingStoreOrig.get();
				if (personnelsettings != null && !personnelsettings.isEmpty()) {
					ContainerPersonalSettingsStore containerPersonalSettingStoreTarget = new ContainerPersonalSettingsStore(
							targetDs, transactionalContext.getSecurityContext(), newContainer);
					containerPersonalSettingStoreTarget.set(personnelsettings);
				}

				Map<String, String> settings = containerSettingStoreOrig.getSettings();
				if (settings != null && !settings.isEmpty()) {
					ContainerSettingsStore containerSettingStoreTarget = new ContainerSettingsStore(targetDs,
							newContainer);
					containerSettingStoreTarget.setSettings(settings);
				}
			}

			containerSyncStoreOrig.delete();
			aclStoreOrig.deleteAll(oldContainer);
			containerPersonalSettingStoreOrig.deleteAll();
			containerSettingStoreOrig.delete();
			new ChangelogStore(origDs, oldContainer).deleteLog();
			containerStoreOrig.delete(c.uid);

			dirContainerStore.invalidateCache(c.uid, c.id);
			DataSourceRouter.invalidateContainer(c.uid);
			monitor.progress(1, c.uid + " tranferred.");
		}
	}

	private void doXferMailbox(String entryUid, ItemValue<Mailbox> mailbox, IServerTaskMonitor monitor) {
		if (mailbox != null) {
			logger.info("[{}] xfer mailbox", entryUid);
			context.provider().instance(IMailboxMgmt.class, domainUid).move(mailbox, targetServer);

			// At this step, the mailbox is transfered between cyrus backends, but the
			// replication must be re-synced
			IDirEntryMaintenance dirEntryMaintenanceService = context.provider().instance(IDirEntryMaintenance.class,
					domainUid, entryUid);
			dirEntryMaintenanceService.repair(Sets.newHashSet("replication.subtree", "replication.parentUid"));
			monitor.progress(1, "mailbox moved.");
		} else {
			monitor.progress(2, "no mailbox to move.");
		}
	}

	private void doPreMove(ItemValue<Mailbox> mailbox) {
		for (IMailboxHook hook : hooks) {
			try {
				hook.preMailboxMoved(context, domainUid, mailbox);
			} catch (Exception e) {
				logger.error("error during call to hook (preMailboxMoved) {}: {} ", hook.getClass(), e.getMessage(), e);
			}
		}
	}

	private void doPostMove(ItemValue<Mailbox> mailbox) {
		for (IMailboxHook hook : hooks) {
			try {
				hook.postMailboxMoved(transactionalContext, domainUid, mailbox);
			} catch (Exception e) {
				logger.error("error during call to hook (preMailboxMoved) {}: {} ", hook.getClass(), e.getMessage(), e);
			}
		}
	}

	private static List<IMailboxHook> getMailboxHooks() {
		RunnableExtensionLoader<IMailboxHook> loader = new RunnableExtensionLoader<>();
		return loader.loadExtensions("net.bluemind.mailbox", "hook", "hook", "class");
	}

	@Override
	public void close() throws Exception {
		transactionalContext.stop();
	}
}
