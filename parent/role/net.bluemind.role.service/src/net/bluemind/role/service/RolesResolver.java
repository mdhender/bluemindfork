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
package net.bluemind.role.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.role.api.RoleDescriptor;

public class RolesResolver {

	private static final Logger logger = LoggerFactory.getLogger(RolesResolver.class);
	private Map<String, RoleDescriptor> descriptorById;
	private Map<String, Set<String>> roleChildren;

	public RolesResolver(Set<RoleDescriptor> rolesDescriptors) {
		descriptorById = new HashMap<>();
		roleChildren = new HashMap<>();

		for (Verb v : Verb.values()) {
			RoleDescriptor desc = RoleDescriptor.create(v.name(), null, null, null);
			descriptorById.put(v.name(), desc);
		}

		roleChildren.put(Verb.All.name(), new HashSet<String>(Arrays.<String> asList(Verb.Write.name(),
				Verb.Read.name(), Verb.Invitation.name(), Verb.Freebusy.name())));

		for (RoleDescriptor role : rolesDescriptors) {
			descriptorById.put(role.id, role);
		}

		for (RoleDescriptor role : rolesDescriptors) {
			insert(role, role.id);
		}
	}

	private void insert(RoleDescriptor role, String id) {
		if (role.parentRoleId != null && !role.selfPromote) {
			Set<String> child = roleChildren.get(role.parentRoleId);
			if (child == null) {
				child = new HashSet<>();
				roleChildren.put(role.parentRoleId, child);
			}
			child.add(id);

			insert(descriptorById.get(role.parentRoleId), id);

		}

		if (!role.childsRole.isEmpty() && !role.selfPromote) {
			Set<String> child = roleChildren.get(role.id);
			if (child == null) {
				child = new HashSet<>();
				roleChildren.put(role.id, child);
			}
			child.addAll(role.childsRole);
		}
	}

	public Set<String> resolve(String... roles) {
		return resolve(new HashSet<>(Arrays.asList(roles)));
	}

	public Set<String> resolve(Set<String> roles) {

		Set<String> ret = new HashSet<>();
		ret.addAll(roles);
		for (String role : roles) {
			Set<String> children = roleChildren.get(role);
			if (children != null) {
				ret.addAll(resolve(children));
			}
		}

		logger.debug("resolved roles {} for {}", ret, roles);
		return ret;
	}

	public Set<String> resolveSelf(Collection<String> roles) {

		Set<String> ret = new HashSet<>();
		ret.addAll(roles);
		for (String role : roles) {
			RoleDescriptor rd = descriptorById.get(role);
			if (rd != null && rd.selfPromote) {
				ret.add(rd.parentRoleId);
			}
		}

		return resolve(ret);
	}

	public Set<String> resolveDirEntry(List<String> roles, DirEntry entry) {

		Set<String> ret = new HashSet<>();
		ret.addAll(roles);
		for (String role : roles) {
			RoleDescriptor rd = descriptorById.get(role);
			if (rd != null && rd.dirEntryPromote && rd.dirEntryKind == entry.kind) {
				ret.addAll(resolve(rd.siblingRole));
			}
		}

		return resolve(ret);

	}
}
