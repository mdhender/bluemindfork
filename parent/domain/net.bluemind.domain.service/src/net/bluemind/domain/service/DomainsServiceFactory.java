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
package net.bluemind.domain.service;

import java.sql.SQLException;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.service.internal.DomainsService;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;

public class DomainsServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IDomains> {

	public DomainsServiceFactory() {
		MQ.init(new MQ.IMQConnectHandler() {

			@Override
			public void connected() {
				MQ.registerProducer(Topic.SYSTEM_NOTIFICATIONS);
			}
		});
	}

	@Override
	public Class<IDomains> factoryClass() {
		return IDomains.class;
	}

	@Override
	public IDomains instance(BmContext context, String... params) throws ServerFault {

		ContainerStore containerStore = new ContainerStore(context, context.getDataSource(),
				context.getSecurityContext());
		Container container = null;
		try {
			container = containerStore.get(DomainsContainerIdentifier.getIdentifier());
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (container == null) {
			throw new ServerFault("container " + DomainsContainerIdentifier.getIdentifier() + " not found");
		}
		return new DomainsService(context, container);
	}
}
