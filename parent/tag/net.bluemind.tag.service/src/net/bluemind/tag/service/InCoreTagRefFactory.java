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
package net.bluemind.tag.service;

import java.sql.SQLException;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider.IServerSideServiceFactory;
import net.bluemind.tag.service.internal.TagRefService;

public class InCoreTagRefFactory implements IServerSideServiceFactory<IInCoreTagRef> {

	public IInCoreTagRef instance(BmContext context, String containerUid) throws ServerFault {

		DataSource ds = DataSourceRouter.get(context, containerUid);
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());
		Container container = null;
		try {
			container = containerStore.get(containerUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (container == null) {
			throw new ServerFault("container " + containerUid + " not found", ErrorCode.NOT_FOUND);
		}

		return new TagRefService(ds, container, context);
	}

	@Override
	public Class<IInCoreTagRef> factoryClass() {
		return IInCoreTagRef.class;
	}

	@Override
	public IInCoreTagRef instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("should have exactly one param");
		}
		return instance(context, params[0]);
	}
}
