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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;

public class ItemValueAuditLogService<T> extends AuditLogService<ItemValue<T>, T> {
	private static final Logger logger = LoggerFactory.getLogger(ItemValueAuditLogService.class);

	public ItemValueAuditLogService(SecurityContext sc, BaseContainerDescriptor cont, ILogMapperProvider<T> dm) {
		super(sc, cont, dm);
	}

	public ItemValueAuditLogService(SecurityContext sc, BaseContainerDescriptor cont) {
		super(sc, cont);
	}

	@Override
	protected AuditLogEntry createAuditLogEntry(ItemValue<T> itemValue) {
		AuditLogEntry auditLogEntry = new AuditLogEntry();
		auditLogEntry.item = createItemElement(itemValue.item());
		auditLogEntry.logtype = type();
		auditLogEntry.content = mapper.createContentElement(itemValue.value);
		auditLogEntry.securityContext = createSecurityContextElement(securityContext);
		auditLogEntry.container = createContainerElement();
		return auditLogEntry;
	}

	@Override
	protected String createUpdateMessage(ItemValue<T> newValue, T oldValue) {
		return mapper.createUpdateMessage(oldValue, newValue.value);
	}

}
