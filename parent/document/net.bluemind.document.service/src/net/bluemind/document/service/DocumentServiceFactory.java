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
package net.bluemind.document.service;

import java.sql.SQLException;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.document.api.IDocument;
import net.bluemind.document.service.internal.DocumentService;

public class DocumentServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IDocument> {

	@Override
	public Class<IDocument> factoryClass() {
		return IDocument.class;
	}

	@Override
	public IDocument instance(BmContext context, String... params) throws ServerFault {

		if (params == null || params.length < 2) {
			throw new ServerFault("wrong number of instance parameters");
		}

		SecurityContext sc = context.getSecurityContext();

		String containerUid = params[0];
		ContainerStore containerStore = new ContainerStore(context, context.getDataSource(), sc);

		Container container = null;
		try {
			container = containerStore.get(containerUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (container == null) {
			throw new ServerFault("container uid " + containerUid + " not found");
		}

		String itemUid = params[1];
		ItemStore itemStore = new ItemStore(context.getDataSource(), container, sc);
		Item item = null;
		try {
			item = itemStore.get(itemUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (item == null) {
			throw new ServerFault("item " + itemUid + " not found");
		}

		DocumentService service = new DocumentService(context, container, item);

		return service;
	}
}
