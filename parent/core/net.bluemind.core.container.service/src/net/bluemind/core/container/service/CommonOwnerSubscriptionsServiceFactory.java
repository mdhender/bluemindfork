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
package net.bluemind.core.container.service;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IOwnerSubscriptionUids;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.service.internal.InternalOwnerSubscriptionsService;
import net.bluemind.core.container.service.internal.OwnerSubscriptionsEventProducer;
import net.bluemind.core.rest.BmContext;
import net.bluemind.lib.vertx.VertxPlatform;

public abstract class CommonOwnerSubscriptionsServiceFactory<T> {

	public abstract Class<T> factoryClass();

	private static final Logger logger = LoggerFactory.getLogger(CommonOwnerSubscriptionsServiceFactory.class);

	public T instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 2) {
			throw new ServerFault("2 parameters expected, domainUid & ownerUid (got " + params + ")");
		}

		String domainUid = params[0];
		String ownerUid = params[1];
		String containerUid = IOwnerSubscriptionUids.getIdentifier(ownerUid, domainUid);

		DataSource ds = DataSourceRouter.get(context, containerUid);
		if (ds == context.getDataSource()) {
			logger.warn("directory datasource selected for {}", containerUid);
		}
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());

		Container container = null;
		try {
			container = containerStore.get(containerUid);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}

		if (container == null) {
			throw ServerFault.notFound("owner subs '" + containerUid + "' is missing");
		}
		OwnerSubscriptionsEventProducer events = new OwnerSubscriptionsEventProducer(domainUid, ownerUid,
				VertxPlatform.eventBus());
		return factoryClass().cast(new InternalOwnerSubscriptionsService(context, ds, container, events));
	}

}
