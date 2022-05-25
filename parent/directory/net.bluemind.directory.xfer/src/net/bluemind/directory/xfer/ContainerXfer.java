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

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ChangelogStore;
import net.bluemind.core.container.persistence.ContainerCache;
import net.bluemind.core.container.persistence.ContainerPersonalSettingsStore;
import net.bluemind.core.container.persistence.ContainerSettingsStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ContainerSyncStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.exchange.mapi.api.MapiFAIContainer;
import net.bluemind.exchange.mapi.api.MapiFolderContainer;

public class ContainerXfer {
	private static final Logger logger = LoggerFactory.getLogger(ContainerXfer.class);

	private final ContainerStore directoryContainerStore;
	private final ContainerStore containerStoreOrigin;
	private final ContainerStore containerStoreTarget;

	private final DataSource dataSourceOrigin;
	private final DataSource dataSourceTarget;

	private final BmContext dataContext;

	private final String targetDataLocation;

	private final DirEntry dirEntry;
	private final CleanupOpsAccumulator cleanupOps;

	private final List<String> nonTransferableData = Lists.newArrayList(//
			IMailReplicaUids.MAILBOX_RECORDS, // synced by replication
			IMailReplicaUids.REPLICATED_CONVERSATIONS, // synced by replication
			MapiFolderContainer.TYPE, // useless if transferred (as item ids are changed)
			MapiFAIContainer.TYPE // useless if transferred (as item ids are changed
	);

	public ContainerXfer(DataSource dataSourceOrigin, DataSource dataSourceTarget, BmContext dataContext,
			DirEntry dirEntry) {
		this.dataSourceOrigin = dataSourceOrigin;
		this.dataSourceTarget = dataSourceTarget;

		directoryContainerStore = new ContainerStore(dataContext, dataContext.getDataSource(),
				dataContext.getSecurityContext());
		containerStoreOrigin = new ContainerStore(null, dataSourceOrigin, dataContext.getSecurityContext());
		containerStoreTarget = new ContainerStore(null, dataSourceTarget, dataContext.getSecurityContext());

		this.dirEntry = dirEntry;
		this.dataContext = dataContext;
		this.targetDataLocation = dataContext.dataSourceLocation(dataSourceTarget);
		this.cleanupOps = new CleanupOpsAccumulator();
	}

	public void xfer(IServerTaskMonitor monitor, String containerType, Function<String, IDataShardSupport> fn)
			throws SQLException {
		List<Container> containers = containerStoreOrigin.findByTypeAndOwner(containerType, dirEntry.entryUid);
		if (containers.isEmpty()) {
			return;
		}

		logger.info("[{}] xfer {} {} ({})", dirEntry.entryUid, containers.size(), containerType, containers);
		monitor.log("[{}] xfer {} {} ({})", dirEntry.entryUid, containers.size(), containerType, containers);

		for (Container c : containers) {
			logger.info("[{}] xfer container {}", dirEntry.entryUid, c.uid);

			IDataShardSupport service = fn.apply(c.uid);
			xferContainer(service, c);
			monitor.progress(1, c.uid + " tranferred.");
		}

		if (!containers.isEmpty()) {
			monitor.end(true, "Containers " + containerType + " transferred", "");
		}
	}

	public void xferContainer(IDataShardSupport service, Container container) throws SQLException {
		Container oldContainer = containerStoreOrigin.get(container.uid);
		Container newContainer = null;

		directoryContainerStore.deleteContainerLocation(oldContainer);
		newContainer = containerStoreTarget.create(container);
		directoryContainerStore.createOrUpdateContainerLocation(newContainer, targetDataLocation);

		if (!nonTransferableData.contains(container.type)) {
			logger.info("Launch xfer {} -> {}", container, targetDataLocation);
			service.xfer(targetDataLocation);
		}

		directoryContainerStore.invalidateCache(container.uid, container.id);

		AclStore aclStoreOrig = new AclStore(dataContext, dataSourceOrigin);
		ContainerSyncStore containerSyncStoreOrig = new ContainerSyncStore(dataSourceOrigin, oldContainer);
		ContainerPersonalSettingsStore containerPersonalSettingStoreOrig = new ContainerPersonalSettingsStore(
				dataSourceOrigin, dataContext.getSecurityContext(), oldContainer);
		ContainerSettingsStore containerSettingStoreOrig = new ContainerSettingsStore(dataSourceOrigin, oldContainer);

		if (newContainer != null) {
			List<AccessControlEntry> acls = aclStoreOrig.get(oldContainer);
			if (acls != null && !acls.isEmpty()) {
				AclStore aclStoreTarget = new AclStore(dataContext, dataSourceTarget);
				aclStoreTarget.store(newContainer, acls);
			}

			ContainerSyncStatus ss = containerSyncStoreOrig.getSyncStatus();
			if (ss != null) {
				ContainerSyncStore containerSyncStoreTarget = new ContainerSyncStore(dataSourceTarget, newContainer);
				containerSyncStoreTarget.setSyncStatus(ss);
			}

			Map<String, String> personnelsettings = containerPersonalSettingStoreOrig.get();
			if (personnelsettings != null && !personnelsettings.isEmpty()) {
				ContainerPersonalSettingsStore containerPersonalSettingStoreTarget = new ContainerPersonalSettingsStore(
						dataSourceTarget, dataContext.getSecurityContext(), newContainer);
				containerPersonalSettingStoreTarget.set(personnelsettings);
			}

			Map<String, String> settings = containerSettingStoreOrig.getSettings();
			if (settings != null && !settings.isEmpty()) {
				ContainerSettingsStore containerSettingStoreTarget = new ContainerSettingsStore(dataSourceTarget,
						newContainer);
				containerSettingStoreTarget.setSettings(settings);
			}
		}

		cleanupOps.accept(containerSyncStoreOrig::delete);
		cleanupOps.accept(() -> aclStoreOrig.deleteAll(oldContainer));
		cleanupOps.accept(containerPersonalSettingStoreOrig::deleteAll);
		cleanupOps.accept(containerSettingStoreOrig::delete);
		cleanupOps.accept(() -> new ChangelogStore(dataSourceOrigin, oldContainer).deleteLog());
		cleanupOps.accept(() -> containerStoreOrigin.delete(container.uid));
		cleanupOps.accept(() -> {
			directoryContainerStore.invalidateCache(container.uid, container.id);
			DataSourceRouter.invalidateContainer(container.uid);
		});
		cleanupOps.accept(() -> {
			BmContext systemCtx = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
			for (DataSource ds : Iterables.concat(Collections.singleton(systemCtx.getDataSource()),
					systemCtx.getAllMailboxDataSource())) {
				ContainerCache.get(dataContext, ds).invalidate(container.uid, container.id);
			}
		});

	}

	public void removeTargetContainers(List<Container> containers) {
		removeTargetContainers(dataContext, dataSourceTarget, containers);
	}

	public static void removeTargetContainers(BmContext context, DataSource dataSource, List<Container> containers) {
		ContainerStore containerStoreTarget = new ContainerStore(null, dataSource, context.getSecurityContext());
		ContainerStore directoryContainerStore = new ContainerStore(null, context.getDataSource(),
				context.getSecurityContext());

		for (Container c : containers) {
			logger.info("Try to remove container {}@{}", c, dataSource);
			CleanupOpsAccumulator removeOps = new CleanupOpsAccumulator();
			removeOps.accept(() -> new AclStore(context, dataSource).deleteAll(c));
			removeOps.accept(() -> new ContainerSyncStore(dataSource, c).delete());
			removeOps.accept(
					() -> new ContainerPersonalSettingsStore(dataSource, context.getSecurityContext(), c).deleteAll());
			removeOps.accept(() -> new ContainerSettingsStore(dataSource, c).delete());
			removeOps.accept(() -> new ChangelogStore(dataSource, c).deleteLog());
			removeOps.accept(() -> {
				if (containerStoreTarget.get(c.uid) != null) {
					containerStoreTarget.delete(c.uid);
				}
			});
			removeOps.accept(() -> {
				directoryContainerStore.deleteContainerLocation(c.uid);
				DataSourceRouter.invalidateContainer(c.uid);
			});
			removeOps.accept(() -> CacheRegistry.get().invalidateAll());
			removeOps.executeAll(logger);
		}
	}

	public void executeCleanups(Logger logger) {
		cleanupOps.executeAll(logger);
	}
}
