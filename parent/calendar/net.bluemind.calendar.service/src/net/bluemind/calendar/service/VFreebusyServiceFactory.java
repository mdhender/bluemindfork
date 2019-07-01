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
package net.bluemind.calendar.service;

import java.sql.SQLException;

import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.calendar.api.IVFreebusy;
import net.bluemind.calendar.service.internal.VFreebusyService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class VFreebusyServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IVFreebusy> {

	@Override
	public Class<IVFreebusy> factoryClass() {
		return IVFreebusy.class;
	}

	@Override
	public IVFreebusy instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}

		String containerId = params[0];
		ContainerStore containerStore = new ContainerStore(context, context.getDataSource(),
				context.getSecurityContext());
		Container container = null;
		try {
			container = containerStore.get(containerId);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (container == null) {
			throw new ServerFault("container " + containerId + " not found");
		}

		if (!IFreebusyUids.TYPE.equals(container.type)) {
			throw new ServerFault("container " + containerId + " is not a freebusy container");
		}

		return new VFreebusyService(context, container);
	}

}
