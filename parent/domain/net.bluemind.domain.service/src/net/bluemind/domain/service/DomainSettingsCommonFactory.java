/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.domain.service;

import java.sql.SQLException;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.service.internal.DomainSettingsService;

public abstract class DomainSettingsCommonFactory {

	protected DomainSettingsService instanceImpl(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}

		String domainUid = params[0];

		ContainerStore containerStore = new ContainerStore(context, context.getDataSource(),
				context.getSecurityContext());

		Container domainsContainers = null;
		try {
			domainsContainers = containerStore.get(DomainsContainerIdentifier.getIdentifier());
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (domainsContainers == null) {
			throw new ServerFault("container " + DomainsContainerIdentifier.getIdentifier() + " not found");
		}

		return new DomainSettingsService(context, domainsContainers, domainUid);
	}

}
