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
package net.bluemind.system.importation.search;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;

import net.bluemind.system.importation.commons.Parameters;

public class DirectorySearch<T extends Parameters> {
	protected final T ldapParameters;
	protected final GroupSearchFilter groupFilter;
	protected final UserSearchFilter userFilter;

	public DirectorySearch(T ldapParameters, GroupSearchFilter groupFilter, UserSearchFilter userFilter) {
		this.ldapParameters = ldapParameters;
		this.groupFilter = groupFilter;
		this.userFilter = userFilter;
	}

	public PagedSearchResult findByFilter(LdapConnection ldapCon, String filter) throws LdapException {
		return SearchCursorBuilder.withConnection(ldapCon, ldapParameters).withSearchFilter(filter).execute();

	}

	public PagedSearchResult findByFilterAndAttributes(LdapConnection ldapCon, String filter, String... attributes)
			throws LdapException {
		return SearchCursorBuilder.withConnection(ldapCon, ldapParameters).withSearchFilter(filter)
				.withAttributes(attributes).execute();

	}

	public PagedSearchResult findByFilterAndBaseDnAndScopeAndAttributes(LdapConnection ldapCon, String filter,
			Dn baseDn, SearchScope scope, String... attributes) throws LdapException {
		return SearchCursorBuilder.withConnection(ldapCon, ldapParameters).withSearchFilter(filter)
				.withAttributes(attributes).withScope(scope).withBaseDn(baseDn).execute();

	}
}
