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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;

public class ContainerPermissionResolver {

	private BmContext context;
	private Container container;

	public ContainerPermissionResolver(BmContext context, Container container) {
		this.context = context;
		this.container = container;
	}

	public Set<Permission> resolve() {

		Set<Permission> perms = new HashSet<>();
		if (container.domainUid != null && container.domainUid.equals(context.getSecurityContext().getContainerUid())
				&& container.owner.equals(context.getSecurityContext().getSubject())) {
			perms.addAll(
					Arrays.stream(Verb.values()).map(v -> ContainerPermission.asPerm(v)).collect(Collectors.toList()));
			return perms;
		}

		if (container.type.equals("dir")
				&& container.domainUid.equals(context.getSecurityContext().getContainerUid())) {
			perms.add(ContainerPermission.asPerm(Verb.Read));
		}
		List<AccessControlEntry> acl = null;
		try {
			acl = new AclStore(context, DataSourceRouter.get(context, container.uid)).get(container);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		for (AccessControlEntry entry : acl) {

			if (entry.subject.equals("anonymous") && context.getSecurityContext().isAnonymous()) {
				perms.add(ContainerPermission.asPerm(entry.verb));
				continue;
			}

			if ((container.domainUid != null
					&& container.domainUid.equals(context.getSecurityContext().getContainerUid()))
					&& (context.getSecurityContext().getSubject().equals(entry.subject)
							|| context.getSecurityContext().getMemberOf().contains(entry.subject)
							|| context.getSecurityContext().getContainerUid().contains(entry.subject))) {

				perms.add(ContainerPermission.asPerm(entry.verb));
				continue;
			}

			if (SecurityContext.TOKEN_FAKE_DOMAIN.equals(context.getSecurityContext().getContainerUid())
					&& context.getSecurityContext().getSubject().equals(entry.subject)) {
				perms.add(ContainerPermission.asPerm(entry.verb));
				continue;
			}
		}

		return perms;
	}

}
