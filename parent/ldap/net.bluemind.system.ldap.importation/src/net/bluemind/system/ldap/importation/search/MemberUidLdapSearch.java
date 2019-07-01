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

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;

import net.bluemind.system.importation.search.LdapSearchCursor;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;

public class MemberUidLdapSearch extends LdapSearch {

	public MemberUidLdapSearch(LdapParameters ldapParameters) {
		super(ldapParameters, new LdapGroupSearchFilter(), new LdapUserSearchFilter());
	}

	public LdapSearchCursor getUserFromLogin(LdapConnection ldapCon, String userLogin) throws LdapException {
		return super.findByFilterAndAttributes(ldapCon,
				userFilter.getSearchFilter(ldapParameters, Optional.empty(), userLogin, null),
				ldapParameters.ldapDirectory.extIdAttribute);
	}
}
