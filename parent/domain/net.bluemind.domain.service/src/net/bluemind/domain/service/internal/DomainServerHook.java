/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.domain.service.internal;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.GroupSearchQuery;
import net.bluemind.group.api.IGroup;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;

public class DomainServerHook extends DefaultServerHook {

	private static Logger logger = LoggerFactory.getLogger(DomainServerHook.class);

	@Override
	public void onServerAssigned(BmContext context, ItemValue<Server> server, ItemValue<Domain> assignedDomain,
			String tag) throws ServerFault {
		this.addMissingDatalocationForUserAndAdminGroups(context, server, assignedDomain, tag);
	}

	private void addMissingDatalocationForUserAndAdminGroups(final BmContext context, final ItemValue<Server> server,
			final ItemValue<Domain> assignedDomain, final String tag) throws ServerFault {

		if (!tag.equals("mail/imap")) {
			logger.debug("Tag {} for server {} is not an imap assignment", tag, server.uid);
			return;
		}

		// search for "admin" and "user" groups ("is_profile": "true")
		final GroupSearchQuery groupSearchQuery = GroupSearchQuery.matchProperty("is_profile", "true");
		final IGroup groupService = context.provider().instance(IGroup.class, assignedDomain.uid);
		final List<ItemValue<Group>> groups = groupService.search(groupSearchQuery);

		// now add missing datalocations
		groups.forEach(group -> {
			final String datalocation = group.value.dataLocation;
			if (datalocation == null || datalocation.isEmpty()) {
				group.value.dataLocation = server.uid;
				groupService.update(group.uid, group.value);
			}
		});
	}

}
