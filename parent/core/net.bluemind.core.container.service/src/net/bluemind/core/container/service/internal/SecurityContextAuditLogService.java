/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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

package net.bluemind.core.container.service.internal;

import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.context.SecurityContext;

public class SecurityContextAuditLogService extends AuditLogService<SecurityContext, Void> {

	public SecurityContextAuditLogService(String type) {
		super(type);
	}

	@Override
	protected AuditLogEntry createAuditLogEntry(SecurityContext sc) {
		AuditLogEntry auditLogEntry = new AuditLogEntry();
		auditLogEntry.logtype = type();
		auditLogEntry.securityContext = createSecurityContextElement(sc);
		return auditLogEntry;
	}

	@Override
	protected String createUpdateMessage(SecurityContext newValue, Void oldValue) {
		return null;
	}

}
