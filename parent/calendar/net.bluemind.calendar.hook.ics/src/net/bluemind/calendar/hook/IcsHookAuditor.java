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
package net.bluemind.calendar.hook;

import net.bluemind.calendar.hook.internal.VEventMessage;
import net.bluemind.core.auditlog.ContainerAuditor;
import net.bluemind.core.auditlog.IAuditManager;

public class IcsHookAuditor extends ContainerAuditor<IcsHookAuditor> {

	public IcsHookAuditor(IAuditManager manager) {
		super(manager);
	}

	public IcsHookAuditor forMessage(VEventMessage message) {
		return forSecurityContext(message.securityContext).parentEventId(message.auditEventId)
				.forContainer(message.container);
	}

	public IcsHookAuditor actionSend(String itemUid, String to, String ics) {
		return action("send-mail") //
				.addActionMetadata("item-uid", itemUid) //
				.addActionMetadata("mailTo", to) //
				.addActionMetadata("ics", ics);
	}

}
