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
package net.bluemind.system.ldap.importation.search;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.ldap.importation.internal.tools.GroupManagerImpl;

public class LdapGroupSearchFilter extends LdapCommonSearchFilter {
	@Override
	protected String nameCondition(String name) {
		if (name == null || name.isBlank()) {
			throw new ServerFault("Invalid group name " + name);
		}

		return "(" + GroupManagerImpl.LDAP_NAME + "=" + name + ")";
	}

	@Override
	protected <T extends Parameters> String getFilter(T ldapParameters) {
		return ldapParameters.ldapDirectory.groupFilter;
	}
}
