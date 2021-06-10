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
package net.bluemind.core.container.service;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchyMgmt;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ContainersHierarchyNodeStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.ContainerHierarchyFlagProvider;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.ContainersHierarchyEventProducer;
import net.bluemind.core.container.service.internal.InternalContainersHierarchyService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.lib.vertx.VertxPlatform;

public abstract class CommonContainersHierarchyServiceFactory<T> {

	public abstract Class<T> factoryClass();

	private static final Logger logger = LoggerFactory.getLogger(CommonContainersHierarchyServiceFactory.class);

	private static final ContainerHierarchyFlagProvider flagsProvider = new ContainerHierarchyFlagProvider();

	public CommonContainersHierarchyServiceFactory() {
	}

	public T instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 2) {
			throw new ServerFault("2 parameters expected, domainUid & ownerUid (got " + params + ")");
		}

		String domainUid = params[0];
		String ownerUid = params[1];
		String containerUid = IFlatHierarchyUids.getIdentifier(ownerUid, domainUid);

		DataSource ds = DataSourceRouter.get(context, containerUid);
		boolean tryInit = false;
		if (ds == context.getDataSource()) {
			DirEntry resolvedOwner = context.su().provider().instance(IDirectory.class, domainUid)
					.findByEntryUid(ownerUid);
			if (resolvedOwner != null && resolvedOwner.dataLocation != null) {
				ds = context.getMailboxDataSource(resolvedOwner.dataLocation);
				tryInit = true;
			}
			if (ds == context.getDataSource()) {
				logger.warn("directory datasource selected for {}", containerUid);
			}
		}
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());

		Container container = null;
		try {
			container = containerStore.get(containerUid);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}

		if (container == null && tryInit) {
			logger.warn("TryInit hierarchy for {} @ {}", ownerUid, domainUid);
			IInternalContainersFlatHierarchyMgmt mgmt = context.su().provider()
					.instance(IInternalContainersFlatHierarchyMgmt.class, domainUid, ownerUid);
			mgmt.init();
			try {
				container = containerStore.get(containerUid);
			} catch (SQLException e) {
				throw new ServerFault(e);
			}
		}

		if (container == null) {
			throw ServerFault.notFound("cont hierarchy '" + containerUid + "' is missing");
		}

		ContainersHierarchyNodeStore itemValueStore = new ContainersHierarchyNodeStore(ds, container);
		ContainerStoreService<ContainerHierarchyNode> storeService = new ContainerStoreService<>(ds,
				context.getSecurityContext(), container, itemValueStore, flagsProvider, v -> 0L, seed -> seed);

		ContainersHierarchyEventProducer events = new ContainersHierarchyEventProducer(domainUid, ownerUid,
				VertxPlatform.eventBus());
		return factoryClass()
				.cast(new InternalContainersHierarchyService(context, ds, container, events, storeService));
	}

}
