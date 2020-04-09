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

import com.google.common.base.Throwables;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.internal.IInternalCalendar;
import net.bluemind.calendar.auditlog.CalendarAuditProxy;
import net.bluemind.calendar.auditlog.CalendarAuditor;
import net.bluemind.calendar.service.internal.CalendarService;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlog.IAuditManager;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.lib.elasticsearch.ESearchActivator;

public class CalendarServiceBaseFactory {

	protected IInternalCalendar getService(BmContext context, String containerId) throws ServerFault {

		DataSource ds = DataSourceRouter.get(context, containerId);
		ContainerStore containerStore = new ContainerStore(context, ds, context.getSecurityContext());
		Container container = null;
		try {
			container = containerStore.get(containerId);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (container == null) {
			throw new ServerFault("container " + containerId + " not found", ErrorCode.NOT_FOUND);
		}

		if (!container.type.equals(ICalendarUids.TYPE)) {
			Throwables.propagate(new ServerFault(
					"Incompatible calendar container type: " + container.type + ", uid: " + container.uid));
		}

		CalendarAuditor auditor = CalendarAuditor.auditor(IAuditManager.instance(), context, container);
		CalendarService service = new CalendarService(ds, ESearchActivator.getClient(), container, context, auditor);

		return new CalendarAuditProxy(auditor, service);
	}

}
