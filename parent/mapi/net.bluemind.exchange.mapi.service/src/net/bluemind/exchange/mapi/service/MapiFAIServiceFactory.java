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
import net.bluemind.exchange.mapi.api.IMapiFolderAssociatedInformation;
import net.bluemind.exchange.mapi.api.MapiFAIContainer;
import net.bluemind.exchange.mapi.service.internal.MapiFAIService;

public class MapiFAIServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IMapiFolderAssociatedInformation> {

	@Override
	public Class<IMapiFolderAssociatedInformation> factoryClass() {
		return IMapiFolderAssociatedInformation.class;
	}

	private IMapiFolderAssociatedInformation getService(BmContext context, String localReplicaGuid) throws ServerFault {
		String mapiFaiContainerUid = MapiFAIContainer.getIdentifier(localReplicaGuid);
		DataSource ds = DataSourceRouter.get(context, mapiFaiContainerUid);
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());

		Container faiContainer = null;
		try {
			faiContainer = containerStore.get(mapiFaiContainerUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (faiContainer == null) {
			throw new ServerFault("container " + mapiFaiContainerUid + " not found");
		}
		MapiFAIService service = new MapiFAIService(context, localReplicaGuid, faiContainer);
		return service;
	}

	@Override
	public IMapiFolderAssociatedInformation instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}
		return getService(context, params[0]);
	}

}
