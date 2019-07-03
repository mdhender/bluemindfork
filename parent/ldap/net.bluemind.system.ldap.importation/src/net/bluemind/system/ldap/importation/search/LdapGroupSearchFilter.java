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

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.search.GroupSearchFilter;
import net.bluemind.system.ldap.importation.api.LdapConstants;
import net.bluemind.system.ldap.importation.internal.tools.GroupManagerImpl;

public class LdapGroupSearchFilter implements GroupSearchFilter {

	private static final Logger logger = LoggerFactory.getLogger(LdapGroupSearchFilter.class);

	@Override
	public <T extends Parameters> String getSearchFilter(T ldapParameters, Optional<String> lastUpdate, String uuid,
			String name) {
		String filter = ldapParameters.ldapDirectory.groupFilter;
		String conditions = "";

		if (lastUpdate.isPresent() && !lastUpdate.get().trim().isEmpty()) {
			conditions += "(" + LdapConstants.MODIFYTIMESTAMP_ATTR + ">=" + lastUpdate.get() + ")";
		}

		if (uuid != null && !"".equals(uuid)) {
			conditions += "(" + ldapParameters.ldapDirectory.extIdAttribute + "=" + uuid + ")";
		}

		if (name != null && !"".equals(name)) {
			conditions += "(" + GroupManagerImpl.LDAP_NAME + "=" + name + ")";
		}

		if (!"".equals(conditions)) {
			filter = "(&" + filter + conditions + ")";
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Group search LDAP filter: " + filter);
		}

		return filter;
	}

}
