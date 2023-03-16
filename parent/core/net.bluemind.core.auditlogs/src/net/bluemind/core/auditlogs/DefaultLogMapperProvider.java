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

package net.bluemind.core.auditlogs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.core.auditlogs.ContentElement.ContentElementBuilder;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.Item;

public class DefaultLogMapperProvider<T> implements ILogMapperProvider<T> {

	private static Logger logger = LoggerFactory.getLogger(DefaultLogMapperProvider.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public AuditLogEntry enhanceAuditLogEntry(Item item, T oldValue, T newValue, Type action,
			AuditLogEntry auditLogEntry) {

		ContentElement content;
		if (auditLogEntry.content == null) {
			String source;
			try {
				source = objectMapper.writeValueAsString(newValue);
				ContentElementBuilder builder = new ContentElement.ContentElementBuilder();
				content = builder.newValue(source).build();
				auditLogEntry.content = content;
			} catch (JsonProcessingException e) {
				logger.error(e.getMessage());
			}
		}
		return auditLogEntry;
	}
}
