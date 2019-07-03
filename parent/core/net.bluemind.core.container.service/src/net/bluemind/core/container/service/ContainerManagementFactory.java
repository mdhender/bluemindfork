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

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.service.internal.ContainerManagement;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class ContainerManagementFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IContainerManagement> {

	/**
	 * @param containerUid
	 * @return {@link IContainerManagement}
	 * @throws ServerFault
	 *             if container not found or technical fault
	 */
	private IContainerManagement get(BmContext context, String containerUid) throws ServerFault {
		DataSource ds = DataSourceRouter.get(context, containerUid);
		ContainerStore containerStore = new ContainerStore(null, ds, context.getSecurityContext());

		Container container = null;
		try {
			container = containerStore.get(containerUid);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}

		if (container == null) {
			throw new ServerFault("container " + containerUid + " not found", ErrorCode.NOT_FOUND);
		}

		return new ContainerManagement(context, container);
	}

	@Override
	public Class<IContainerManagement> factoryClass() {
		return IContainerManagement.class;
	}

	@Override
	public IContainerManagement instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}
		return get(context, params[0]);
	}
}
