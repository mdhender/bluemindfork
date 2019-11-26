/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.user.accounts.service;

import java.sql.SQLException;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public abstract class AbstractAccountServiceFactory<T>
		implements ServerSideServiceProvider.IServerSideServiceFactory<T> {

	@Override
	public T instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 2) {
			throw new ServerFault("wrong number of instance parameters");
		}
		String domainUid = params[0];
		String uid = params[1];

		ContainerStore containerStore = new ContainerStore(context.getDataSource(), context.getSecurityContext());
		Container container = null;

		try {
			container = containerStore.get(domainUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (container == null) {
			throw new ServerFault("container " + domainUid + " not found");
		}

		ItemStore itemStore = new ItemStore(context.getDataSource(), container, SecurityContext.SYSTEM);
		Item item = null;
		try {
			item = itemStore.get(uid);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}

		return instanceImpl(context, container, item);
	}

	protected abstract T instanceImpl(BmContext context, Container container, Item item) throws ServerFault;

}