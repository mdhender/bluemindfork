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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.core.auditlogs.ContentElement;
import net.bluemind.core.auditlogs.ContentElement.ContentElementBuilder;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class AccessControlEntryAuditLogMapper implements ILogMapperProvider<AccessControlEntry> {

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(AccessControlEntryAuditLogMapper.class);
	private final Container container;

	public AccessControlEntryAuditLogMapper(Container cont) {
		this.container = cont;
	}

	@Override
	public ContentElement createContentElement(AccessControlEntry entry) {
		ContentElementBuilder builder = new ContentElement.ContentElementBuilder();
		StringBuilder sb = new StringBuilder();
		builder.key(entry.subject);

		IUser userService = null;
		if (container.domainUid != null) {
			userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
					container.domainUid);
			User aclUser = userService.get(entry.subject);
			String aclUserEntry = (aclUser != null) ? aclUser.defaultEmailAddress() : entry.subject;

			sb.append("'" + aclUserEntry + "': ");

			sb.append("access '" + entry.verb.name() + "'");
			sb.append(" granted container '" + container.uid + "' ");

			User containerUser = userService.get(container.owner);
			String containerUserOwner = (containerUser != null) ? containerUser.defaultEmailAddress() : container.owner;
			sb.append("with owner '" + containerUserOwner + "'");
			builder.with(Arrays.asList(aclUserEntry, containerUserOwner));
			builder.author(Arrays.asList(containerUserOwner));
		}

		builder.description(sb.toString());
		try {
			String source = objectMapper.writeValueAsString(entry);
			builder.newValue(source);
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage());
		}
		return builder.build();
	}

	@Override
	public String createUpdateMessage(AccessControlEntry oldValue, AccessControlEntry newValue) {
		return null;
	}

}