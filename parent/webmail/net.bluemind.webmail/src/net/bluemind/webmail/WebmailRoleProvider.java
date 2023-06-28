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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.webmail;

import static net.bluemind.role.api.BasicRoles.CATEGORY_MAIL;

import java.util.Collections;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.bluemind.role.api.BasicRoles;
import net.bluemind.role.api.RoleDescriptor;
import net.bluemind.role.api.RolesCategory;
import net.bluemind.role.provider.IRolesProvider;

public class WebmailRoleProvider implements IRolesProvider {

	@Override
	public Set<String> getRoles() {
		return ImmutableSet.<String>builder().add(BasicRoles.ROLE_WEBMAIL).build();
	}

	@Override
	public Set<RoleDescriptor> getDescriptors(Locale locale) {
		ResourceBundle rb = ResourceBundle.getBundle("OSGI-INF/l10n/bundle", locale);

		return ImmutableSet.<RoleDescriptor>builder().add(RoleDescriptor
				.create(BasicRoles.ROLE_WEBMAIL, CATEGORY_MAIL, rb.getString("role.accessRoundcubeWebmail.label"),
						rb.getString("role.accessRoundcubeWebmail.description"))
				.giveRoles(BasicRoles.ROLE_MAIL).delegable()).build();
	}

	@Override
	public Set<RolesCategory> getCategories(Locale locale) {
		return Collections.emptySet();
	}
}
