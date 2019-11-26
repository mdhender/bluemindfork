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
package net.bluemind.todolist.service;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.elasticsearch.client.Client;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.service.internal.TodoListService;

public class TodoListServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<ITodoList> {

	public TodoListServiceFactory() {

	}

	private ITodoList getService(BmContext context, String containerId) throws ServerFault {

		DataSource ds = DataSourceRouter.get(context, containerId);
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());
		Container container = null;
		try {
			container = containerStore.get(containerId);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (container == null) {
			throw new ServerFault("container " + containerId + " not found", ErrorCode.NOT_FOUND);
		}
		// FIXME Not sure why but todo must have it own system tag...

		Client esClient = ESearchActivator.getClient();

		if (esClient == null) {
			throw new ServerFault("elasticsearch was not found for todo indexing");
		}
		TodoListService service = new TodoListService(ds, esClient, container, context);
		return service;
	}

	@Override
	public Class<ITodoList> factoryClass() {
		return ITodoList.class;
	}

	@Override
	public ITodoList instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}
		return getService(context, params[0]);
	}
}
