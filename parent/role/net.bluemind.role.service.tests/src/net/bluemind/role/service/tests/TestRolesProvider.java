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
package net.bluemind.role.service.tests;

import java.util.Locale;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.bluemind.role.api.RoleDescriptor;
import net.bluemind.role.api.RolesCategory;
import net.bluemind.role.provider.IRolesProvider;

public class TestRolesProvider implements IRolesProvider {

	public static final String ROLE_TEST = "test";
	public static final String CATEGORY_TEST = "test-cat";

	public TestRolesProvider() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Set<String> getRoles() {
		return ImmutableSet.<String> builder().add(ROLE_TEST).build();
	}

	@Override
	public Set<RoleDescriptor> getDescriptors(Locale locale) {

		RoleDescriptor desc = RoleDescriptor.create(ROLE_TEST, CATEGORY_TEST, "label-" + locale.getLanguage(),
				"desc-" + locale.getLanguage());
		return ImmutableSet.<RoleDescriptor> builder().add(desc).build();
	}

	@Override
	public Set<RolesCategory> getCategories(Locale locale) {
		return ImmutableSet.<RolesCategory> builder()
				.add(RolesCategory.create(CATEGORY_TEST, "cat-" + locale.getLanguage())).build();
	}

}
