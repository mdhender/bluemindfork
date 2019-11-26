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
package net.bluemind.calendar.service;

import java.sql.SQLException;

import javax.sql.DataSource;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.IPublishCalendar;
import net.bluemind.calendar.service.internal.PublishCalendarService;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class PublishCalendarFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IPublishCalendar> {

	@Override
	public Class<IPublishCalendar> factoryClass() {
		return IPublishCalendar.class;
	}

	@Override
	public IPublishCalendar instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}

		BmContext admin = context.su();

		DataSource ds = DataSourceRouter.get(admin, params[0]);
		ContainerStore containerStore = new ContainerStore(admin, ds, admin.getSecurityContext());
		Container container = null;
		try {
			container = containerStore.get(params[0]);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (container == null) {
			throw new ServerFault("container " + params[0] + " not found", ErrorCode.NOT_FOUND);
		}

		if (!container.type.equals(ICalendarUids.TYPE)) {
			throw (new ServerFault(
					"Incompatible calendar container type: " + container.type + ", uid: " + container.uid));
		}

		return new PublishCalendarService(admin, ds, container);
	}

}
