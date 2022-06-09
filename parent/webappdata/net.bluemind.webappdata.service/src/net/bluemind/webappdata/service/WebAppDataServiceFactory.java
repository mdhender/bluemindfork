/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.webappdata.service;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.webappdata.api.IWebAppData;
import net.bluemind.webappdata.service.internal.NoOpWebAppDataService;
import net.bluemind.webappdata.service.internal.WebAppDataService;

public class WebAppDataServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IWebAppData> {

	private static final Logger logger = LoggerFactory.getLogger(WebAppDataServiceFactory.class);

	public WebAppDataServiceFactory() {

	}

	private IWebAppData getService(BmContext context, String containerUid) {
		DataSource ds = DataSourceRouter.get(context, containerUid);
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());
		Container container;

		try {
			container = containerStore.get(containerUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (container == null) {
			logger.warn("container {} not found: using noop service", containerUid);
			return new NoOpWebAppDataService();
		}

		return new WebAppDataService(ds, container, context);
	}

	@Override
	public Class<IWebAppData> factoryClass() {
		return IWebAppData.class;
	}

	@Override
	public IWebAppData instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}
		return getService(context, params[0]);
	}
}
