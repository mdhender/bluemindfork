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
package net.bluemind.calendar.auditlog;

import net.bluemind.core.auditlog.ContainerAuditor;
import net.bluemind.core.auditlog.IAuditManager;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;

public class CalendarAuditor extends ContainerAuditor<CalendarAuditor> {

	public CalendarAuditor(IAuditManager manager) {
		super(manager);
	}

	public static CalendarAuditor auditor(IAuditManager manager, BmContext context, Container container) {
		CalendarAuditor ret = new CalendarAuditor(manager);
		return ret.forContext(context).forContainer(container);
	}

	public static CalendarAuditor auditor(IAuditManager manager, SecurityContext sc, Container container) {
		CalendarAuditor ret = new CalendarAuditor(manager);
		return ret.forSecurityContext(sc).forContainer(container);
	}

	public CalendarAuditor withSendNotification(Boolean sendNotif) {
		if (sendNotif != null) {
			return addActionMetadata("sendNotif", Boolean.toString(sendNotif));
		} else {
			return addActionMetadata("sendNotif", "false");
		}
	}

}
