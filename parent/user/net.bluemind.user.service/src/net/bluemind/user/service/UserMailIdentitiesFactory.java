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
package net.bluemind.user.service;

import java.sql.SQLException;
import java.util.List;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.user.api.IUserMailIdentities;
import net.bluemind.user.hook.identity.IUserMailIdentityHook;
import net.bluemind.user.service.internal.UserMailIdentities;

public class UserMailIdentitiesFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IUserMailIdentities> {
	private static final List<IUserMailIdentityHook> identityHooks = getHooks();

	@Override
	public Class<IUserMailIdentities> factoryClass() {
		return IUserMailIdentities.class;
	}

	@Override
	public IUserMailIdentities instance(BmContext context, String... params) {
		if (params == null || params.length < 2) {
			throw new ServerFault("wrong number of instance parameters");
		}
		return getService(context, params[0], params[1]);
	}

	private IUserMailIdentities getService(BmContext context, String domainUid, String userUid) {

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

		ItemValue<Domain> domainValue = context.su().provider().instance(IDomains.class).get(domainUid);

		if (domainValue == null) {
			throw new ServerFault("domain " + domainUid + " not found", ErrorCode.NOT_FOUND);
		}

		return new UserMailIdentities(context, domainValue, container, userUid, identityHooks);
	}

	private static List<IUserMailIdentityHook> getHooks() {
		RunnableExtensionLoader<IUserMailIdentityHook> loader = new RunnableExtensionLoader<>();
		return loader.loadExtensionsWithPriority("net.bluemind.user.hook", "usermailidentity", "hook", "impl");
	}

}
