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
package net.bluemind.domain.service;

import java.util.Set;

import com.google.common.collect.ImmutableMap;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.group.api.Group;
import net.bluemind.role.api.DefaultRoles;

public class DefaultGroups {

	@FunctionalInterface
	public interface GroupAndRoles {
		public void values(Group group, Set<String> roles) throws ServerFault;
	}

	public static void userGroup(GroupAndRoles func) throws ServerFault {
		Group userGroup = new Group();
		userGroup.name = "user";
		userGroup.hiddenMembers = true;
		userGroup.hidden = true;
		userGroup.properties = ImmutableMap.of("is_profile", "true");

		func.values(userGroup, DefaultRoles.USER_DEFAULT_ROLES);
	}

	public static void adminGroup(GroupAndRoles func) throws ServerFault {
		Group adminGroup = new Group();
		adminGroup.name = "admin";
		adminGroup.hiddenMembers = true;
		adminGroup.hidden = true;
		adminGroup.properties = ImmutableMap.of("is_profile", "true");
		func.values(adminGroup, DefaultRoles.ADMIN_DEFAULT_ROLES);

	}

}
