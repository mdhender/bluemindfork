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
package net.bluemind.exchange.mapi.service;

import java.sql.SQLException;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.exchange.mapi.api.IMapiFolder;
import net.bluemind.exchange.mapi.service.internal.MapiFolderService;

public class MapiFolderServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IMapiFolder> {

	@Override
	public Class<IMapiFolder> factoryClass() {
		return IMapiFolder.class;
	}

	private IMapiFolder getService(BmContext context, String containerUid) throws ServerFault {
		DataSource ds = DataSourceRouter.get(context, containerUid);
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());

		Container rawMessagesContainer = null;
		try {
			rawMessagesContainer = containerStore.get(containerUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (rawMessagesContainer == null) {
			throw new ServerFault("container " + rawMessagesContainer + " not found");
		}
		MapiFolderService service = new MapiFolderService(context, ds, rawMessagesContainer);
		return service;
	}

	@Override
	public IMapiFolder instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters, expected containerUid");
		}
		return getService(context, params[0]);
	}

}
