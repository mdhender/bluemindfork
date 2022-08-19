/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.core.container.repair;

import java.sql.SQLException;

import javax.sql.DataSource;

import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.service.RepairTaskMonitor;

public class ContainerRepairUtil {

	public static void verifyContainerIsMarkedAsDefault(String containerUid, RepairTaskMonitor monitor,
			Runnable runnable) {
		ContainerDescriptor container = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainers.class).getIfPresent(containerUid);
		if (container != null && !container.defaultContainer) {
			monitor.notify("Default container {} is not marked as default", containerUid);
			runnable.run();
		}
	}

	public static void setAsDefault(String containerUid, BmContext context, RepairTaskMonitor monitor) {

		IContainers service = context.provider().instance(IContainers.class);
		ContainerDescriptor container = service.getIfPresent(containerUid);

		DataSource dataSource = DataSourceRouter.get(context, containerUid);
		ContainerStore containerStore = new ContainerStore(context, dataSource, SecurityContext.SYSTEM);
		try {
			containerStore.update(containerUid, container.name, true);
		} catch (SQLException e) {
			monitor.notify("Cannot mark container {} as default: {}", container, e.getMessage());
		}

	}

}
