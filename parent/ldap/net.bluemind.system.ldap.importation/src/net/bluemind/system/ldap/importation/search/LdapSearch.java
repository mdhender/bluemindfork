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
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;

import net.bluemind.system.importation.search.DirectorySearch;
import net.bluemind.system.importation.search.PagedSearchResult;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;

public class LdapSearch extends DirectorySearch<LdapParameters> {

	public LdapSearch(LdapParameters ldapParameters) {
		super(ldapParameters, new LdapGroupSearchFilter(), new LdapUserSearchFilter());
	}

	public PagedSearchResult findAllUsers(LdapConnection ldapCon) throws LdapException {
		return findByFilter(ldapCon, userFilter.getSearchFilter(ldapParameters, Optional.empty(), null, null));
	}

	public PagedSearchResult findUsersDnByLastModification(LdapConnection ldapCon, Optional<String> lastUpdate)
			throws LdapException {
		return super.findByFilterAndAttributes(ldapCon,
				userFilter.getSearchFilter(ldapParameters, lastUpdate, null, null));
	}

	public PagedSearchResult findGroupsDnByLastModification(LdapConnection ldapCon, Optional<String> lastUpdate)
			throws LdapException {
		return findByFilterAndAttributes(ldapCon, groupFilter.getSearchFilter(ldapParameters, lastUpdate, null, null));
	}

	public PagedSearchResult findAllGroups(LdapConnection ldapCon) throws LdapException {
		return findByFilterAndAttributes(ldapCon,
				groupFilter.getSearchFilter(ldapParameters, Optional.empty(), null, null),
				ldapParameters.ldapDirectory.extIdAttribute);
	}

	public PagedSearchResult getUserUUID(LdapConnection ldapCon, Dn userDn) throws LdapException {
		return super.findByFilterAndBaseDnAndScopeAndAttributes(ldapCon,
				userFilter.getSearchFilter(ldapParameters, Optional.empty(), null, null), userDn, SearchScope.OBJECT,
				ldapParameters.ldapDirectory.extIdAttribute);
	}

	public PagedSearchResult getGroupUUID(LdapConnection ldapCon, Dn groupDn) throws LdapException {
		return super.findByFilterAndBaseDnAndScopeAndAttributes(ldapCon,
				groupFilter.getSearchFilter(ldapParameters, Optional.empty(), null, null), groupDn, SearchScope.OBJECT,
				ldapParameters.ldapDirectory.extIdAttribute);
	}

	public PagedSearchResult findByGroupByName(LdapConnection ldapCon) throws LdapException {
		return super.findByFilterAndAttributes(ldapCon, groupFilter.getSearchFilter(ldapParameters, Optional.empty(),
				null, ldapParameters.splitDomain.relayMailboxGroup), "*");
	}

	public PagedSearchResult findByUserLogin(LdapConnection ldapCon, String userLogin) throws LdapException {
		return super.findByFilterAndAttributes(ldapCon,
				userFilter.getSearchFilter(ldapParameters, Optional.empty(), userLogin, null),
				ldapParameters.ldapDirectory.extIdAttribute);
	}
}
