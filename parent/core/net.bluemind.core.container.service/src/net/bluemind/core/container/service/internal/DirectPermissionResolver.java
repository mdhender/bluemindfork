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

import net.bluemind.core.rest.BmContext;

public class DirectPermissionResolver {

	private BmContext context;

	public static final Map<String, Set<Permission>> permsForNotSameDomain = new HashMap<>();

	public DirectPermissionResolver(BmContext context) {
		this.context = context;
	}

	public Set<Permission> resolve() {
		Set<Permission> perms = new HashSet<>();

		for (String role : context.getSecurityContext().getRoles()) {
			perms.addAll(permsForNotSameDomain.getOrDefault(role, Collections.emptySet()));
		}
		return perms;
	}

	public static void registerRole(String role, Permission perm) {
		registerRole(role, ImmutableSet.<Permission>builder().add(perm).build());
	}

	public static void registerRole(String role, Set<Permission> perms) {
		permsForNotSameDomain.merge(role, perms,
				(l, r) -> ImmutableSet.<Permission>builder().addAll(l).addAll(r).build());
	}

}
