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
import java.util.Arrays;
import java.util.function.Consumer;

import javax.sql.DataSource;

import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.user.api.IUserSubscription;

public class ContainerRepairUtil {

	public static void verifyContainerIsMarkedAsDefault(String containerUid, RepairTaskMonitor monitor, Runnable op) {
		ContainerDescriptor container = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainers.class).getIfPresent(containerUid);
		if (container != null && !container.defaultContainer) {
			monitor.notify("Default container {} is not marked as default", containerUid);
			op.run();
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

	public static void verifyContainerSubscription(String userUid, String domainUid, RepairTaskMonitor monitor,
			Consumer<String> op, String... containers) {
		IUserSubscription userSubService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSubscription.class, domainUid);

		for (String container : containers) {
			ContainerDescriptor desc = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainers.class).getIfPresent(container);
			if (desc != null) {
				if (!userSubService.subscribers(container).contains(userUid)) {
					monitor.notify("User {} is not subscribed to container {}", userUid, container);
					op.accept(container);
				}

			}
		}

	}

	public static void subscribe(String userUid, String domainUid, String container) {
		IUserSubscription userSubService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSubscription.class, domainUid);

		userSubService.subscribe(userUid, Arrays.asList(ContainerSubscription.create(container, true)));
	}

}
