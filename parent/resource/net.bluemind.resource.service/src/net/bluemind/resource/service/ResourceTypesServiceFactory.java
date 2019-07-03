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
package net.bluemind.resource.service;

import java.sql.SQLException;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.IResourceTypeUids;
import net.bluemind.resource.service.internal.ResourceTypesService;

public class ResourceTypesServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IResourceTypes> {

	@Override
	public Class<IResourceTypes> factoryClass() {
		return IResourceTypes.class;
	}

	@Override
	public IResourceTypes instance(BmContext context, String... params) throws ServerFault {

		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}

		String domainUid = params[0];
		String containerId = IResourceTypeUids.getIdentifier(domainUid);
		ContainerStore containerStore = new ContainerStore(null, context.getDataSource(), context.getSecurityContext());
		Container container = null;
		try {
			container = containerStore.get(containerId);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (container == null) {
			throw new ServerFault("container " + containerId + " not found");
		}

		if (!container.type.equals("dir")) {
			throw new ServerFault("Incompatible resources container: " + container.type + ", uid: " + container.uid);
		}

		return new ResourceTypesService(context, domainUid, container);
	}
}
