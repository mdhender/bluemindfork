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

package net.bluemind.server.service;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.server.api.IServer;
import net.bluemind.server.hook.IServerHook;
import net.bluemind.server.service.internal.ServerService;

public class ServerServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IServer> {

	private static final Logger logger = LoggerFactory.getLogger(ServerServiceFactory.class);

	private static final List<IServerHook> serverHooks = getHooks();

	public ServerServiceFactory() {
	}

	private static List<IServerHook> getHooks() {
		RunnableExtensionLoader<IServerHook> loader = new RunnableExtensionLoader<IServerHook>();
		List<IServerHook> hooks = loader.loadExtensionsWithPriority("net.bluemind.server.hook", "serverhook", "hook",
				"impl");
		return hooks;
	}

	public IServer getService(BmContext context, String containerId) throws ServerFault {
		logger.debug("getService");
		if (containerId.equals("default")) {
			containerId = InstallationId.getIdentifier();
		}
		ContainerStore containerStore = new ContainerStore(context, context.getDataSource(),
				context.getSecurityContext());
		Container container = null;
		try {
			container = containerStore.get(containerId);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (container == null) {
			throw new ServerFault("container " + containerId + " not found", ErrorCode.NOT_FOUND);
		}

		if (!"installation".equals(container.type)) {
			throw new ServerFault("container " + containerId + " not found", ErrorCode.NOT_FOUND);
		}

		ServerService service = new ServerService(context, container, serverHooks);

		return service;
	}

	@Override
	public Class<IServer> factoryClass() {
		return IServer.class;
	}

	@Override
	public IServer instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}

		return getService(context, params[0]);
	}
}
