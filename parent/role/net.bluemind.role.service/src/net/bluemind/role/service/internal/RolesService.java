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
package net.bluemind.role.service.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.role.api.RoleDescriptor;
import net.bluemind.role.api.RolesCategory;
import net.bluemind.role.provider.IRolesProvider;
import net.bluemind.role.provider.IRolesVerifier;
import net.bluemind.role.service.IInternalRoles;
import net.bluemind.role.service.RolesResolver;

public class RolesService implements IInternalRoles {

	private BmContext context;
	private List<IRolesProvider> providers;
	private RolesResolver resolver;
	private List<IRolesVerifier> validators;

	public RolesService(BmContext context, List<IRolesProvider> providers, RolesResolver resolver,
			List<IRolesVerifier> validators) {
		this.context = context;
		this.providers = providers;
		this.resolver = resolver;
		this.validators = validators;
	}

	@Override
	public Set<RoleDescriptor> getRoles() throws ServerFault {
		Set<RoleDescriptor> ret = new HashSet<>();

		Locale locale = Locale.forLanguageTag(context.getSecurityContext().getLang());
		for (IRolesProvider provider : providers) {
			ret.addAll(provider.getDescriptors(locale).stream().filter(d -> {
				return d.visible;
			}).collect(Collectors.toList()));
		}

		return ret;
	}

	@Override
	public Set<RolesCategory> getRolesCategories() throws ServerFault {
		Set<RolesCategory> ret = new HashSet<>();

		Locale locale = Locale.forLanguageTag(context.getSecurityContext().getLang());
		for (IRolesProvider provider : providers) {
			ret.addAll(provider.getCategories(locale));
		}

		return ret;
	}

	@Override
	public Set<String> filter(Set<String> roles) throws ServerFault {
		Set<String> newSet = new HashSet<>(roles);
		for (IRolesVerifier activator : validators) {
			newSet.removeAll(activator.getDeactivatedRoles());
		}
		return newSet;
	}

	@Override
	public Set<String> resolve(Set<String> roles) {
		return resolver.resolve(roles);
	}

	@Override
	public Set<String> resolveSelf(List<String> roles) {
		return resolver.resolveSelf(roles);
	}

	@Override
	public Set<String> resolveDirEntry(List<String> roles, DirEntry entry) {
		return resolver.resolveDirEntry(roles, entry);
	}
}
