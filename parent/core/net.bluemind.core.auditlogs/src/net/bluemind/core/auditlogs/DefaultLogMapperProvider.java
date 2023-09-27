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

public class DefaultLogMapperProvider<T> implements ILogMapperProvider<T> {

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(DefaultLogMapperProvider.class);

	public ContentElement createContentElement(T newValue) {

		ContentElementBuilder builder = new ContentElement.ContentElementBuilder();
		try {
			String source = objectMapper.writeValueAsString(newValue);
			builder.newValue(source);
			return builder.build();
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return builder.build();
		}
	}

	@Override
	public AuditLogUpdateStatus createUpdateMessage(T oldValue, T newValue) {
		return new AuditLogUpdateStatus();
	}
}
