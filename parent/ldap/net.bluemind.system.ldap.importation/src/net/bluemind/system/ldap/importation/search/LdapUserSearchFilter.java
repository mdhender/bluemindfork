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
import net.bluemind.system.importation.search.UserSearchFilter;
import net.bluemind.system.ldap.importation.api.LdapConstants;
import net.bluemind.system.ldap.importation.internal.tools.UserManagerImpl;

public class LdapUserSearchFilter implements UserSearchFilter {

	private static final Logger logger = LoggerFactory.getLogger(LdapUserSearchFilter.class);

	@Override
	public <T extends Parameters> String getSearchFilter(T ldapParameters, Optional<String> lastUpdate, String login,
			String uuid) {
		String filter = ldapParameters.ldapDirectory.userFilter;
		String conditions = "";

		if (lastUpdate.isPresent() && !lastUpdate.get().trim().isEmpty()) {
			conditions += "(" + LdapConstants.MODIFYTIMESTAMP_ATTR + ">=" + lastUpdate.get() + ")";
		}

		if (login != null && !"".equals(login.trim())) {
			conditions += "(" + UserManagerImpl.LDAP_LOGIN + "=" + login + ")";
		}

		if (uuid != null && !"".equals(uuid.trim())) {
			conditions += "(" + ldapParameters.ldapDirectory.extIdAttribute + "=" + uuid + ")";
		}

		if (!"".equals(conditions)) {
			filter = "(&" + filter + conditions + ")";
		}

		if (logger.isDebugEnabled()) {
			logger.debug("User search LDAP filter: " + filter);
		}

		return filter;
	}

}
