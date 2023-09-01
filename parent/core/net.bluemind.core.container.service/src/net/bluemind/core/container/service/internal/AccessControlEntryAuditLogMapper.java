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

package net.bluemind.core.container.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.ContentElement;
import net.bluemind.core.auditlogs.ContentElement.ContentElementBuilder;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.acl.AccessControlEntry;

public class AccessControlEntryAuditLogMapper implements ILogMapperProvider<AccessControlEntry> {

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static Logger logger = LoggerFactory.getLogger(AccessControlEntryAuditLogMapper.class);

	@Override
	public AuditLogEntry enhanceAuditLogEntry(Item item, AccessControlEntry oldValue, AccessControlEntry newValue,
			Type action, AuditLogEntry auditLogEntry) {
		ContentElement content = buildContent(newValue);
		if (content != null) {
			auditLogEntry.content = content;
			return auditLogEntry;
		}
		return null;
	}

	private ContentElement buildContent(AccessControlEntry value) {
		ContentElementBuilder builder = new ContentElement.ContentElementBuilder();
		builder.key(value.subject);

		try {
			String source = objectMapper.writeValueAsString(value);
			builder.newValue(source);
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

		builder.description(value.verb.name());
		return builder.build();
	}
}
