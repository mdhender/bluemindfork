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

import java.util.HashSet;
import java.util.Set;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.role.provider.IRolesVerifier;

public class TestRolesVerifier implements IRolesVerifier {

	@Override
	public Set<String> getDeactivatedRoles() throws ServerFault {
		Set<String> deactivatedRoles = new HashSet<>();
		deactivatedRoles.add("role1");
		deactivatedRoles.add("role3");
		return deactivatedRoles;
	}

}
