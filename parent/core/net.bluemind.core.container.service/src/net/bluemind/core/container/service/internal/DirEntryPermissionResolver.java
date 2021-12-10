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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.role.api.BasicRoles;

public class DirEntryPermissionResolver {
	private static final Map<String, Set<Permission>> selfRoles = new HashMap<>();
	private static final Map<String, Set<Permission>> permsForSameDomain = new HashMap<>();

	private final BmContext context;
	private final String dirEntryUid;
	private final String domainUid;
	private final String orgUnitUid;

	public DirEntryPermissionResolver(BmContext context, String domainUid, String dirEntryUid, String orgUnitUid) {
		this.context = context;
		this.domainUid = domainUid;
		this.dirEntryUid = dirEntryUid;
		this.orgUnitUid = orgUnitUid;
	}

	public Set<Permission> resolve() {
		Set<Permission> perms = new HashSet<>();

		if (domainUid.equals(context.getSecurityContext().getContainerUid()) && dirEntryUid != null
				&& dirEntryUid.equals(context.getSecurityContext().getSubject())) {
			// self
			perms.add(new DirEntryPermission(DirEntry.Kind.USER, BasicRoles.ROLE_SELF));
			perms.add(ContainerPermission.asPerm(Verb.All));

			for (String role : context.getSecurityContext().getRoles()) {
				perms.addAll(selfRoles.getOrDefault(role, Collections.emptySet()));
			}

		}

		if (domainUid.equals(context.getSecurityContext().getContainerUid())) {
			if (dirEntryUid != null) {
				DirEntry entry = context.su().provider().instance(IDirectory.class, domainUid)
						.findByEntryUid(dirEntryUid);

				if (entry != null && entry.orgUnitPath != null) {
					Set<String> ouRoles = context.getSecurityContext().getRolesForOrgUnit(entry.orgUnitPath.path());
					for (String role : ouRoles) {
						perms.addAll(permsForSameDomain.getOrDefault(role, Collections.emptySet()));
					}
				}
			}

			if (orgUnitUid != null) {
				OrgUnitPath path = context.su().provider().instance(IOrgUnits.class, domainUid).getPath(orgUnitUid);
				Set<String> ouRoles = context.getSecurityContext().getRolesForOrgUnit(path.path());
				for (String role : ouRoles) {
					perms.addAll(permsForSameDomain.getOrDefault(role, Collections.emptySet()));
				}
			}

			for (String role : context.getSecurityContext().getRoles()) {
				perms.addAll(permsForSameDomain.getOrDefault(role, Collections.emptySet()));
			}
		}
		return perms;
	}

	public static void registerSameDomainRole(String role, Permission perm) {
		registerSameDomainRole(role, ImmutableSet.<Permission>builder().add(perm).build());
	}

	public static void registerSameDomainRole(String role, Set<Permission> perms) {
		permsForSameDomain.merge(role, perms, (l, r) -> ImmutableSet.<Permission>builder().addAll(l).addAll(r).build());
	}

	public static void registerSelfRole(String role, Permission perm) {
		registerSelfRole(role, ImmutableSet.<Permission>builder().add(perm).build());
	}

	public static void registerSelfRole(String role, Set<Permission> perms) {
		selfRoles.merge(role, perms, (l, r) -> ImmutableSet.<Permission>builder().addAll(l).addAll(r).build());
	}

}
