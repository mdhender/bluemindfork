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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.AuditLogUpdateStatus;
import net.bluemind.core.auditlogs.ContentElement;
import net.bluemind.core.auditlogs.ContentElement.ContentElementBuilder;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class AccessControlEntryAuditLogMapper implements ILogMapperProvider<AccessControlEntry> {

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
			// 'All' was granted to sylvain on david's 'Calendrier'
			sb.append("'" + entry.verb.name() + "' was granted to ");
			sb.append("'" + aclUserEntry + "' on ");
			User containerUser = userService.get(container.owner);
			String containerUserOwner = (containerUser != null) ? containerUser.defaultEmailAddress() : container.owner;
			sb.append("'" + containerUserOwner + "''s ");
			sb.append("'" + container.type + "' container");
			sb.append("\r\n");
			builder.with(Arrays.asList(aclUserEntry, containerUserOwner));
			builder.author(Arrays.asList(containerUserOwner));
		}

		builder.description(sb.toString());
		try {
			String source = JsonUtils.asString(entry);
			builder.newValue(source);
		} catch (ServerFault e) {
			logger.error(e.getMessage());
		}
		return builder.build();
	}

	@Override
	public AuditLogUpdateStatus createUpdateMessage(AccessControlEntry oldValue, AccessControlEntry newValue) {
		return new AuditLogUpdateStatus();
	}

}