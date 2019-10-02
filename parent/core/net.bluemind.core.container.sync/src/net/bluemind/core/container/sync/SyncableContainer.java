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
package net.bluemind.core.container.sync;

import java.util.List;
import java.util.Map;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerSyncResult;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class SyncableContainer {

	private static final List<ISyncableContainerFactory> impl = load();

	private final BmContext context;

	public SyncableContainer(BmContext context) {
		this.context = context;
	}

	public ContainerSyncResult sync(Container container, Map<String, String> syncTokens, IServerTaskMonitor monitor)
			throws ServerFault {
		if (container == null) {
			return null;
		}

		ContainerSyncResult ret = null;
		for (ISyncableContainerFactory syncContainersFactory : impl) {
			if (container.type.equals(syncContainersFactory.support())) {
				// FIXME more than one impl ?
				ret = syncContainersFactory.create(context, container).sync(syncTokens, monitor);
			}
		}
		return ret;
	}

	private static List<ISyncableContainerFactory> load() {
		RunnableExtensionLoader<ISyncableContainerFactory> rel = new RunnableExtensionLoader<ISyncableContainerFactory>();
		List<ISyncableContainerFactory> stores = rel.loadExtensions("net.bluemind.core.container",
				"containersyncfactory", "containersyncfactory", "implementation");

		return stores;
	}
}
