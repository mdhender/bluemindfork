/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.directory.xfer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationArtifacts;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IInternalMailConversation;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.core.api.fault.ErrorCode;
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
import net.bluemind.core.container.service.IContainerStoreService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.deferredaction.api.IDeferredAction;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.device.api.Device;
import net.bluemind.device.api.IDevice;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirEntryMaintenance;
import net.bluemind.domain.api.Domain;
import net.bluemind.eas.api.Account;
import net.bluemind.eas.api.IEas;
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
import net.bluemind.notes.api.INote;
import net.bluemind.notes.api.INoteUids;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.ITags;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoListsMgmt;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class DirectoryXfer implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(DirectoryXfer.class);

	private final String domainUid;
	private final String targetServerUid;
	private final TransactionalContext transactionalContext;
	private final BmContext dataContext;
	private final BmContext context;

	private final ItemValue<Server> targetServer;
	private final ItemValue<DirEntry> dirEntry;

	private final ContainerStore dirContainerStore;
	private final ContainerStore containerStoreOrig;
	private final ContainerStore containerStoreTarget;

	private final DataSource directoryDs;
	private final DataSource origDs;
	private final DataSource targetDs;

	private final IUser userApi;
	private final ItemValue<User> originalUser;

	private final CleanupOpsAccumulator cleanupOps;

	private static final List<IMailboxHook> hooks = getMailboxHooks();

	public DirectoryXfer(BmContext context, ItemValue<Domain> domain, IContainerStoreService<DirEntry> itemStore,
			String entryUid, String serverUid) {
		this.domainUid = domain.uid;
		this.context = context;
		this.targetServer = ServerSideServiceProvider.getProvider(context).instance(IServer.class, "default")
				.getComplete(serverUid);
		this.dirEntry = itemStore.get(entryUid, null);
		if (dirEntry == null) {
			throw ServerFault.notFound("directory entry " + entryUid + " not found");
		}
		this.targetServerUid = serverUid;

		if (!Boolean.getBoolean("bluemind.testmode")) {
			transactionalContext = new TransactionalContext(context.getSecurityContext());
			dataContext = transactionalContext;
		} else {
			transactionalContext = null;
			dataContext = context;
		}

		this.directoryDs = dataContext.getDataSource();
		this.origDs = dataContext.getMailboxDataSource(dirEntry.value.dataLocation);
		this.targetDs = dataContext.getMailboxDataSource(serverUid);
		this.dirContainerStore = new ContainerStore(dataContext, dataContext.getDataSource(),
				dataContext.getSecurityContext());
		this.containerStoreOrig = new ContainerStore(null, origDs, dataContext.getSecurityContext());
		this.containerStoreTarget = new ContainerStore(null, targetDs, dataContext.getSecurityContext());

		userApi = ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);
		originalUser = userApi.getComplete(dirEntry.uid);

		this.cleanupOps = new CleanupOpsAccumulator();
	}

	public void doXfer(String entryUid, IServerTaskMonitor monitor, Consumer<ItemValue<DirEntry>> updateDirEntry) {
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

		IServiceProvider sp = ServerSideServiceProvider.getProvider(dataContext);
		ItemValue<Mailbox> mailbox = sp.instance(IMailboxes.class, domainUid).getComplete(entryUid);

		try (UserSessionUtility userSessionUtility = new UserSessionUtility(context,
				originalUser.value.login + "@" + domainUid, originalUser.value.dataLocation)) {
			monitor.begin(17, "moving containers");

			// Logout user
			userSessionUtility.logoutUser(monitor);

			// Wait for the replication to complete the queue
			monitor.log("Waiting for the replication to complete...");

			LoginResponse lr = sp.instance(IAuthentication.class).su(originalUser.value.login + "@" + domainUid);
			CompletableFuture<Long> waitReplication = WaitReplicationFinished.doProbe(VertxPlatform.getVertx(),
					new Probe(originalUser.value.dataLocation, lr.latd, lr.authKey));
			try {
				waitReplication.get(31, TimeUnit.MINUTES);
			} catch (TimeoutException te) {
				logger.error("Timeout waiting for the replication queue to complete. Please retry later.");
				monitor.log("Timeout waiting for the replication queue to complete. Please retry later.");
				throw te;
			}
			monitor.progress(1, "Replication is synced");

			// Lock the user out of IMAP (cyr_deny)
			userSessionUtility.lockoutUser(monitor);

			// Wait the replication do to stuff...
			Thread.sleep(5000);

			doPreMove(mailbox);
			doXferContainers(entryUid, mailbox, monitor);

			doXferCyrusArtifacts(entryUid, domainUid, mailbox, monitor);

			// We must set the dataLocation before even trying to move
			// the data, because otherwise, the replication service will try to access
			// the mailbox using the wrong location, and will take the wrong decisions
			dirEntry.value.dataLocation = targetServerUid;
			updateDirEntry.accept(dirEntry);

			cleanupOps.executeAll();

			commitAll();

			/* The transactional context is not used anymore here */
			doXferMailbox(entryUid, mailbox, monitor);
			doPostMove(mailbox);
			if (mailbox != null) {
				VertxPlatform.eventBus().publish("postfix.map.dirty", new JsonObject());
			}
			postProcess(entryUid, mailbox, monitor);

			// Logout user again (force SessionData to be updated)
			userSessionUtility.logoutUser(monitor);

			logger.info("Ending xfer of {} ({}) to {}", entryUid, dirEntry.displayName, targetServerUid);
			monitor.end(true, "Transfer finished: all tasks completed", "");
		} catch (Exception e) {
			logger.error("transfer failed: {}", e.getMessage(), e);
			rollbackAll();
			monitor.end(false, "transfer failed: " + e, null);
			throw new ServerFault(e);
		}
	}

	private void postProcess(String entryUid, ItemValue<Mailbox> mailbox, IServerTaskMonitor monitor) {
		// At this step, the mailbox is transfered between cyrus backends, but the
		// replication must be re-synced
		IServiceProvider serviceProvider = ServerSideServiceProvider.getProvider(context);
		monitor.log("re-syncing replication");
		IDirEntryMaintenance dirEntryMaintenanceService = context.provider().instance(IDirEntryMaintenance.class,
				domainUid, entryUid);
		dirEntryMaintenanceService.repair(Sets.newHashSet("replication.subtree", "replication.parentUid"));

		// Reset elasticsearch indexes
		doReindex(IAddressBookUids.TYPE, dirEntry.uid, monitor.subWork(1),
				containerUid -> serviceProvider.instance(IAddressBooksMgmt.class).reindex(containerUid));
		doReindex(ICalendarUids.TYPE, dirEntry.uid, monitor.subWork(1),
				containerUid -> serviceProvider.instance(ICalendarsMgmt.class).reindex(containerUid));
		doReindex(ITodoUids.TYPE, dirEntry.uid, monitor.subWork(1),
				containerUid -> serviceProvider.instance(ITodoListsMgmt.class).reindex(containerUid));

		// Reset EAS devices
		IEas easService = serviceProvider.instance(IEas.class);
		try {
			IDevice deviceService = serviceProvider.instance(IDevice.class, entryUid);
			for (ItemValue<Device> device : deviceService.list().values) {
				logger.info("reset EAS synchronization for device {}", device);
				monitor.log("reset EAS synchronization for device " + device.displayName);
				easService.insertPendingReset(Account.create(entryUid, device.value.identifier));
			}
		} catch (ServerFault sf) {
			if (ErrorCode.NOT_FOUND.equals(sf.getCode())) {
				logger.warn("No device container found for user uid {} ({})", entryUid,
						originalUser.value.login + "@" + domainUid);
			} else {
				throw sf;
			}
		}

		// Reset mail-app, calendar, contacts
		resetUserLocalData(monitor);

		logger.info("Post process done");
	}

	private void resetUserLocalData(IServerTaskMonitor monitor) {
		ItemValue<User> user = userApi.getComplete(dirEntry.uid);
		monitor.log("Suggest client applications to clear local cache");
		logger.info("Suggest client applications to clear local cache for {}", user.displayName);
		user.value.mailboxCopyGuid = UUID.randomUUID().toString();
		userApi.update(user.uid, user.value);
	}

	private void doReindex(String containerType, String entryUid, IServerTaskMonitor monitor,
			Function<String, TaskRef> reindex) {
		List<Container> containers;
		try {
			containers = containerStoreTarget.findByTypeAndOwner(containerType, entryUid);
		} catch (SQLException e) {
			logger.error("Unable to retrieve container list for {}: {}", entryUid, e.getMessage(), e);
			monitor.log("Unable to retrieve container list for " + entryUid);
			return;
		}
		monitor.log("re-indexing " + containers.size() + " container(s) of " + entryUid);
		for (Container c : containers) {
			try {
				logger.info("re-indexing container {}", c);
				TaskRef reindexTask = reindex.apply(c.uid);
				ITask task = context.provider().instance(ITask.class, reindexTask.id);
				TaskUtils.forwardProgress(task, monitor.subWork(1));
			} catch (ServerFault sf) {
				logger.error("Failed to reindex {} {}: {}", containerType, c.uid, sf.getMessage());
				monitor.end(false, "Failed to reindex " + containerType + " " + c.uid, "");
			}
		}
		monitor.end(true, "Reindex " + containerType + "done", "");
	}

	private void doXferCyrusArtifacts(String entryUid, String domainUid, ItemValue<Mailbox> mailbox,
			IServerTaskMonitor monitor) {
		String userId = mailbox.value.name + "@" + domainUid;
		IServiceProvider sp = ServerSideServiceProvider.getProvider(dataContext);
		ICyrusReplicationArtifacts cyrusArtifcatsService = sp.instance(ICyrusReplicationArtifacts.class, userId);
		cyrusArtifcatsService.xfer(targetServerUid);
	}

	private void doXferContainers(String entryUid, ItemValue<Mailbox> mailbox, IServerTaskMonitor monitor)
			throws SQLException {
		logger.info("[{}] xfer cleanup containers on target", dirEntry.uid);
		monitor.progress(0, "removing stale containers on target");
		List<Container> toRemoveContainers = containerStoreTarget.findByTypeOwnerReadOnly(null, dirEntry.uid, null);
		cleanupTargetContainers(toRemoveContainers);

		IServiceProvider sp = ServerSideServiceProvider.getProvider(dataContext);
		xferContainers(monitor.subWork(1), IAddressBookUids.TYPE,
				containerUid -> sp.instance(IAddressBook.class, containerUid));
		xferContainers(monitor.subWork(1), ICalendarUids.TYPE,
				containerUid -> sp.instance(ICalendar.class, containerUid));
		xferContainers(monitor.subWork(1), IDeferredActionContainerUids.TYPE,
				containerUid -> sp.instance(IDeferredAction.class, containerUid));
		xferContainers(monitor.subWork(1), ITodoUids.TYPE, containerUid -> sp.instance(ITodoList.class, containerUid));
		xferContainers(monitor.subWork(1), INoteUids.TYPE, containerUid -> sp.instance(INote.class, containerUid));
		xferContainers(monitor.subWork(1), ITagUids.TYPE, containerUid -> sp.instance(ITags.class, containerUid));
		xferContainers(monitor.subWork(1), IFlatHierarchyUids.TYPE,
				containerUid -> sp.instance(IContainersFlatHierarchy.class, domainUid, entryUid));
		xferContainers(monitor.subWork(1), IOwnerSubscriptionUids.TYPE,
				containerUid -> sp.instance(IOwnerSubscriptions.class, domainUid, entryUid));

		// Avoid removing future xfer mailbox records from object storage
		dataContext.getAllMailboxDataSource().stream().forEach(ds -> {
			try {
				Connection conn = ds.getConnection();
				try (Statement stmt = conn.createStatement()) {
					stmt.execute("SET bluemind.bypass_message_body_purge_queue = true");
				}
			} catch (SQLException e) {
				logger.error("Unable to bypass message_body_purge queue: {}", e.getMessage(), e);
			}
		});

		// Theses containers can't be migrated: item_id are changed, so all the data is
		// useless and lost
		xferContainers(monitor.subWork(1), IMailReplicaUids.MAILBOX_RECORDS,
				containerUid -> sp.instance(IDbMailboxRecords.class, IMailReplicaUids.uniqueId(containerUid)), false);
		xferContainers(monitor.subWork(1), IMailReplicaUids.REPLICATED_CONVERSATIONS,
				containerUid -> context.provider().instance(IInternalMailConversation.class,
						IMailReplicaUids.conversationSubtreeUid(domainUid, entryUid)),
				false);
		xferContainers(monitor.subWork(1), MapiFolderContainer.TYPE,
				containerUid -> sp.instance(IMapiFolder.class, containerUid), false);
		xferContainers(
				monitor.subWork(1), MapiFAIContainer.TYPE, containerUid -> sp
						.instance(IMapiFolderAssociatedInformation.class, MapiFAIContainer.getIdentifier(containerUid)),
				false);
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
			try {
				new AclStore(dataContext, targetDs).deleteAll(c);
				new ContainerSyncStore(targetDs, c).delete();
				new ContainerPersonalSettingsStore(targetDs, dataContext.getSecurityContext(), c).deleteAll();
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
		xferContainers(monitor, containerType, fn, true);
	}

	private void xferContainers(IServerTaskMonitor monitor, String containerType,
			Function<String, IDataShardSupport> fn, boolean transferData) throws SQLException {
		List<Container> containers = containerStoreOrig.findByTypeAndOwner(containerType, dirEntry.uid);
		if (containers.isEmpty()) {
			return;
		}

		logger.info("[{}] xfer {} {} ({})", dirEntry.uid, containers.size(), containerType, containers);
		monitor.log("[{}] xfer {} {} ({})", dirEntry.uid, containers.size(), containerType, containers);

		for (Container c : containers) {
			logger.info("[{}] xfer container {}", dirEntry.uid, c.uid);

			IDataShardSupport service = fn.apply(c.uid);

			Container oldContainer = containerStoreOrig.get(c.uid);
			Container newContainer = null;
			dirContainerStore.deleteContainerLocation(oldContainer);
			DataSourceRouter.invalidateContainer(oldContainer.uid);

			if (transferData) {
				newContainer = containerStoreTarget.create(c);
				dirContainerStore.createContainerLocation(newContainer, targetServerUid);
				service.xfer(targetServerUid);
			}

			dirContainerStore.invalidateCache(c.uid, c.id);
			DataSourceRouter.invalidateContainer(c.uid);

			AclStore aclStoreOrig = new AclStore(dataContext, origDs);
			ContainerSyncStore containerSyncStoreOrig = new ContainerSyncStore(origDs, oldContainer);
			ContainerPersonalSettingsStore containerPersonalSettingStoreOrig = new ContainerPersonalSettingsStore(
					origDs, dataContext.getSecurityContext(), oldContainer);
			ContainerSettingsStore containerSettingStoreOrig = new ContainerSettingsStore(origDs, oldContainer);

			if (newContainer != null) {
				List<AccessControlEntry> acls = aclStoreOrig.get(oldContainer);
				if (acls != null && !acls.isEmpty()) {
					AclStore aclStoreTarget = new AclStore(dataContext, targetDs);
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
							targetDs, dataContext.getSecurityContext(), newContainer);
					containerPersonalSettingStoreTarget.set(personnelsettings);
				}

				Map<String, String> settings = containerSettingStoreOrig.getSettings();
				if (settings != null && !settings.isEmpty()) {
					ContainerSettingsStore containerSettingStoreTarget = new ContainerSettingsStore(targetDs,
							newContainer);
					containerSettingStoreTarget.setSettings(settings);
				}
			}

			cleanupOps.accept(() -> {
				containerSyncStoreOrig.delete();
				aclStoreOrig.deleteAll(oldContainer);
				containerPersonalSettingStoreOrig.deleteAll();
				containerSettingStoreOrig.delete();
				new ChangelogStore(origDs, oldContainer).deleteLog();
				containerStoreOrig.delete(c.uid);
				dirContainerStore.invalidateCache(c.uid, c.id);
				DataSourceRouter.invalidateContainer(c.uid);
				return null;
			});
			monitor.progress(1, c.uid + " tranferred.");
		}
		if (!containers.isEmpty()) {
			monitor.end(true, "Containers " + containerType + " transferred", "");
		}
	}

	private void doXferMailbox(String entryUid, ItemValue<Mailbox> mailbox, IServerTaskMonitor monitor) {
		if (mailbox != null) {
			logger.info("[{}] xfer mailbox", entryUid);
			monitor.log("moving mailbox {} (can take hours)...", entryUid);
			context.provider().instance(IMailboxMgmt.class, domainUid).move(mailbox, targetServer);
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
				hook.postMailboxMoved(dataContext, domainUid, mailbox);
			} catch (Exception e) {
				logger.error("error during call to hook (preMailboxMoved) {}: {} ", hook.getClass(), e.getMessage(), e);
			}
		}
	}

	private static List<IMailboxHook> getMailboxHooks() {
		RunnableExtensionLoader<IMailboxHook> loader = new RunnableExtensionLoader<>();
		return loader.loadExtensions("net.bluemind.mailbox", "hook", "hook", "class");
	}

	private void commitAll() throws SQLException {
		if (transactionalContext == null) {
			// test mode
			return;
		}
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
		CacheRegistry.get().invalidateAll();

		if (transactionalContext == null) {
			// test mode
			return;
		}
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
	}

	@Override
	public void close() throws Exception {
		if (transactionalContext != null) {
			transactionalContext.stop();
		}
	}
}
