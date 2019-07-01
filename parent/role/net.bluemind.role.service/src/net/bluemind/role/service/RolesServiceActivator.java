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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.bluemind.core.container.service.internal.DirEntryPermission;
import net.bluemind.core.container.service.internal.DirEntryPermissionResolver;
import net.bluemind.core.container.service.internal.DirectPermissionResolver;
import net.bluemind.core.container.service.internal.Permission;
import net.bluemind.core.container.service.internal.SimplePermission;
import net.bluemind.role.api.RoleDescriptor;
import net.bluemind.role.provider.IRolesProvider;
import net.bluemind.role.provider.IRolesVerifier;

public class RolesServiceActivator implements BundleActivator {
	private static final Logger logger = LoggerFactory.getLogger(RolesServiceActivator.class);
	static RolesResolver resolver;
	static List<IRolesVerifier> validators;
	static List<IRolesProvider> providers;
	static private Map<String, RoleDescriptor> roles;

	@Override
	public void start(BundleContext ctx) throws Exception {
		List<IRolesProvider> ps = RolesFactory.loadFactories();
		Set<RoleDescriptor> all = new HashSet<>();
		roles = new HashMap<>();
		Multimap<String, RoleDescriptor> m = HashMultimap.create();
		for (IRolesProvider p : ps) {

			Set<RoleDescriptor> descriptors = p.getDescriptors(Locale.ENGLISH);
			all.addAll(descriptors);

			roles.putAll(descriptors.stream().collect(Collectors.toMap(d -> d.id, d -> d)));
		}

		all.stream().forEach(rd -> {
			RoleDescriptor current = rd;
			m.put(current.id, rd);
			if (rd.childsRole != null) {
				for (String c : rd.childsRole) {
					RoleDescriptor dc = roles.get(c);
					if (dc != null) {
						m.put(current.id, dc);
					}
				}
			}

			while (current != null && current.parentRoleId != null) {
				m.put(current.parentRoleId, rd);
				if (rd.childsRole != null) {
					for (String c : rd.childsRole) {
						RoleDescriptor dc = roles.get(c);
						if (dc != null) {
							m.put(current.parentRoleId, dc);
						}
					}
				}
				String parentRoleId = current.parentRoleId;
				current = roles.get(parentRoleId);
				if (current == null) {
					logger.warn("no roleDescriptor found for {}", parentRoleId);
				}
			}
		});

		for (RoleDescriptor role : all) {
			if (role.selfPromote) {
				forEach(m, role, current -> {
					DirEntryPermissionResolver.registerSelfRole(role.id, perm(current));
					DirEntryPermissionResolver.registerSelfRole(role.id, selfPerm(current));
					return null;
				});
			} else if (role.dirEntryKind != null) {
				forEach(m, role, current -> {
					DirEntryPermissionResolver.registerSameDomainRole(role.id, perm(current));
					return null;
				});

			} else {
				forEach(m, role, current -> {
					DirectPermissionResolver.registerRole(role.id, perm(current));
					return null;
				});
			}
		}

		// DirEntryPermissionResolver.finish();
		resolver = new RolesResolver(all);
		validators = RolesFactory.loadValidators();
		providers = RolesFactory.loadFactories();
	}

	private static Set<Permission> perm(RoleDescriptor current) {
		Set<Permission> ret = new HashSet<>();
		ret.add(new SimplePermission(current.id));
		if (current.dirEntryKind == null || current.selfPromote) {
			for (String r : current.containerRoles) {
				ret.add(new SimplePermission(r));
			}
		} else {
			ret.add(DirEntryPermission.create(current.dirEntryKind, current.id));
			for (String r : current.containerRoles) {
				ret.add(DirEntryPermission.create(current.dirEntryKind, r));
			}
		}
		return ret;
	}
	
	private static Set<Permission> selfPerm(RoleDescriptor current) {
		Set<Permission> ret = new HashSet<>();
		if (current.selfPromote) {
			ret.add(DirEntryPermission.create(current.dirEntryKind, current.parentRoleId));
			ret.add(new SimplePermission(current.parentRoleId));
		}
		return ret;
	}

	public static void forEach(Multimap<String, RoleDescriptor> roles, RoleDescriptor current,
			Function<RoleDescriptor, Void> f) {

		f.apply(current);
		for (RoleDescriptor rd : roles.get(current.id)) {
			f.apply(rd);
			if (!rd.id.equals(current.id)) {
				forEach(roles, rd, f);
			}
		}

	}

	@Override
	public void stop(BundleContext ctx) throws Exception {
		resolver = null;
	}

}
