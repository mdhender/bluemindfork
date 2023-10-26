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

package net.bluemind.authentication.service;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.auditlogs.AuditLogUpdateStatus;
import net.bluemind.core.auditlogs.ContentElement;
import net.bluemind.core.auditlogs.ContentElement.ContentElementBuilder;
import net.bluemind.core.auditlogs.ILogMapperProvider;
import net.bluemind.core.context.SecurityContext;

public class LoginAuditLogMapper implements ILogMapperProvider<SecurityContext> {

	@Override
	public ContentElement createContentElement(SecurityContext sc) {
		return buildContent(sc);
	}

	@Override
	public AuditLogUpdateStatus createUpdateMessage(SecurityContext oldValue, SecurityContext newValue) {
		return new AuditLogUpdateStatus();
	}

	private ContentElement buildContent(SecurityContext sc) {
		ContentElementBuilder builder = new ContentElement.ContentElementBuilder();
		List<String> attendees = new ArrayList<>();
		if (sc.getSubjectDisplayName() != null) {
			attendees.add(sc.getSubjectDisplayName());
		}
		if (sc.getOwnerPrincipal() != null) {
			attendees.add(sc.getOwnerPrincipal());
		}
		if (!attendees.isEmpty()) {
			builder.with(attendees);
		}
		return builder.build();
	}
}
