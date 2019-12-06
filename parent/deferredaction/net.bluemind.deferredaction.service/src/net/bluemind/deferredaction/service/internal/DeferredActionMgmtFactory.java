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
package net.bluemind.deferredaction.service.internal;

import java.sql.SQLException;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.deferredaction.service.IDeferredActionMgmt;

public class DeferredActionMgmtFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IDeferredActionMgmt> {

	@Override
	public Class<IDeferredActionMgmt> factoryClass() {
		return IDeferredActionMgmt.class;
	}

	@Override
	public IDeferredActionMgmt instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("Wrong number of instance parameters");
		}

		DataSource dataSource = DataSourceRouter.get(context, params[0]);
		ContainerStore containerStore = new ContainerStore(context, dataSource, context.getSecurityContext());

		Container deferredAction = null;
		try {
			deferredAction = containerStore.get(params[0]);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (deferredAction == null) {
			throw new ServerFault("container " + params[0] + " not found");
		}

		return new DeferredActionMgmt(context, dataSource, deferredAction);
	}

}