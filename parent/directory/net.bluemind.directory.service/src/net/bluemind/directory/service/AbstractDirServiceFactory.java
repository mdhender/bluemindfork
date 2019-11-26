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
package net.bluemind.directory.service;

import java.sql.SQLException;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;

public abstract class AbstractDirServiceFactory<T> implements ServerSideServiceProvider.IServerSideServiceFactory<T> {

	@Override
	public T instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}
		String domainUid = params[0];

		ItemValue<Domain> domainValue = context.su().provider().instance(IDomains.class).findByNameOrAliases(domainUid);
		if (domainValue == null) {
			throw new ServerFault("domain " + domainUid + " not found", ErrorCode.NOT_FOUND);
		}
		domainUid = domainValue.uid;

		ContainerStore containerStore = new ContainerStore(context, context.getDataSource(),
				context.getSecurityContext());
		Container container = null;

		try {
			container = containerStore.get(domainUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (container == null) {
			throw new ServerFault("container " + domainUid + " not found");
		}

		return instanceImpl(context, domainValue, container);
	}

	protected abstract T instanceImpl(BmContext context, ItemValue<Domain> domainValue, Container container)
			throws ServerFault;

}
