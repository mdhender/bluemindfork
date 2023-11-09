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
import net.bluemind.core.auditlogs.AuditLogUpdateStatus;
import net.bluemind.core.auditlogs.AuditLogUpdateStatus.MessageCriticity;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.context.SecurityContext;

public class SecurityContextAuditLogService extends AuditLogService<SecurityContext, SecurityContext> {

	public SecurityContextAuditLogService(String type, ILogMapperProvider<SecurityContext> dm) {
		super(type, dm);
	}

	@Override
	protected AuditLogEntry createAuditLogEntry(SecurityContext sc) {
		AuditLogEntry auditLogEntry = new AuditLogEntry();
		auditLogEntry.logtype = type();
		auditLogEntry.content = mapper.createContentElement(sc);
		auditLogEntry.securityContext = createSecurityContextElement(sc);
		return auditLogEntry;
	}

	@Override
	protected AuditLogUpdateStatus createUpdateStatus(SecurityContext newValue, SecurityContext oldValue) {
		return new AuditLogUpdateStatus();
	}

	public void logCreate(SecurityContext value, String domainUid) {
		AuditLogEntry auditLogEntry = createAuditLogEntry(value);
		auditLogEntry.action = Type.Created.name();
		auditLogEntry.criticity = MessageCriticity.MAJOR;
		auditLogEntry.domainUid = domainUid;
		store(auditLogEntry);
	}

}
