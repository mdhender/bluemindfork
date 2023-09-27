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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.ContentElement.ContentElementBuilder;
import net.bluemind.core.utils.JsonUtils;

public class DefaultLogMapperProvider<T> implements ILogMapperProvider<T> {

	private static final Logger logger = LoggerFactory.getLogger(DefaultLogMapperProvider.class);

	public ContentElement createContentElement(T newValue) {

		ContentElementBuilder builder = new ContentElement.ContentElementBuilder();
		try {
			String source = JsonUtils.asString(newValue);
			builder.newValue(source);
			return builder.build();
		} catch (ServerFault e) {
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
