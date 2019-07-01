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
package net.bluemind.server.service.internal;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.server.persistance.ServerStore;

public class ServerDomainHook extends DomainHookAdapter {

	private Logger logger = LoggerFactory.getLogger(ServerDomainHook.class);

	@Override
	public void onDeleted(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		unassignServerAssignments(context, domain);
	}

	private void unassignServerAssignments(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		logger.info("Deleting all server assignments of domain {}", domain.uid);
		ContainerStore containerStore = new ContainerStore(context, context.getDataSource(),
				context.getSecurityContext());
		try {
			Container container = containerStore.get(InstallationId.getIdentifier());
			ServerStoreService storeService = new ServerStoreService(context, container);
			ServerStore serverStore = (ServerStore) storeService.getItemValueStore();
			serverStore.unassignFromDomain(domain.uid);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

}
