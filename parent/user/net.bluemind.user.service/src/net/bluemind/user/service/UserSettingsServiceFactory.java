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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.service.internal.UserSettingsService;

public class UserSettingsServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IUserSettings> {

	public UserSettingsServiceFactory() {
	}

	private IUserSettings getService(BmContext context, String domainContainerUid) throws ServerFault {

		IDomainSettings domainSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainContainerUid);

		ContainerStore containerStore = new ContainerStore(context, context.getDataSource(),
				context.getSecurityContext());

		Container userSettings = null;
		try {
			userSettings = containerStore.get(domainContainerUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (userSettings == null) {
			throw new ServerFault("container " + domainContainerUid + " not found");
		}

		UserSettingsService service = new UserSettingsService(context, domainSettingsService, userSettings,
				domainContainerUid);
		return service;
	}

	@Override
	public Class<IUserSettings> factoryClass() {
		return IUserSettings.class;
	}

	@Override
	public IUserSettings instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}

		return getService(context, params[0]);
	}
}
