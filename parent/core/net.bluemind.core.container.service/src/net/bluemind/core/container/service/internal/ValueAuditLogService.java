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
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.context.SecurityContext;

public class ValueAuditLogService<T> extends AuditLogService<T, T> {

	public ValueAuditLogService(SecurityContext sc, BaseContainerDescriptor cont, ILogMapperProvider<T> dm) {
		super(sc, cont, dm);
	}

	public ValueAuditLogService(SecurityContext sc, BaseContainerDescriptor cont) {
		super(sc, cont);
	}

	@Override
	protected AuditLogEntry createAuditLogEntry(T value) {
		AuditLogEntry auditLogEntry = new AuditLogEntry();
		auditLogEntry.container = createContainerElement();
		auditLogEntry.logtype = type();
		auditLogEntry.content = mapper.createContentElement(value);
		auditLogEntry.securityContext = createSecurityContextElement(securityContext);
		return auditLogEntry;
	}

	@Override
	protected AuditLogUpdateStatus createUpdateStatus(T newValue, T oldValue) {
		return mapper.createUpdateMessage(oldValue, newValue);
	}

}
