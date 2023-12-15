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
import java.util.stream.StreamSupport;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;

import com.google.common.base.Strings;

import net.bluemind.system.importation.commons.exceptions.NullOrEmptySplitGroupName;
import net.bluemind.system.importation.search.DirectorySearch;
import net.bluemind.system.importation.search.PagedSearchResult;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;

public class LdapSearch extends DirectorySearch<LdapParameters> {
	private final LdapGroupSearchFilter groupFilter;
	private final LdapUserSearchFilter userFilter;

	public LdapSearch(LdapParameters ldapParameters) {
		super(ldapParameters);
		this.groupFilter = new LdapGroupSearchFilter();
		this.userFilter = new LdapUserSearchFilter();
	}

	public PagedSearchResult findAllUsers(LdapConnection ldapCon) throws LdapException {
		return findByFilter(ldapCon, userFilter.getSearchFilter(ldapParameters));
	}

	public PagedSearchResult findUsersDnByLastModification(LdapConnection ldapCon, Optional<String> lastUpdate)
			throws LdapException {
		return super.findByFilterAndAttributes(ldapCon,
				userFilter.getSearchFilterByLastModification(ldapParameters, lastUpdate));
	}

	public PagedSearchResult findGroupsDnByLastModification(LdapConnection ldapCon, Optional<String> lastUpdate)
			throws LdapException {
		return findByFilterAndAttributes(ldapCon,
				groupFilter.getSearchFilterByLastModification(ldapParameters, lastUpdate));
	}

	public PagedSearchResult findAllGroups(LdapConnection ldapCon) throws LdapException {
		return findByFilterAndAttributes(ldapCon, groupFilter.getSearchFilter(ldapParameters),
				ldapParameters.ldapDirectory.extIdAttribute);
	}

	/**
	 * Get user entry from DN considering user filter and ignoring incremental mode
	 * 
	 * @param ldapCon
	 * @param userDn
	 * @return
	 * @throws LdapException
	 */
	public Optional<Entry> findUserFromDn(LdapConnection ldapCon, Dn userDn) throws LdapException {
		try (PagedSearchResult cursor = super.findByFilterAndBaseDnAndScopeAndAttributes(ldapCon,
				userFilter.getSearchFilter(ldapParameters), userDn, SearchScope.OBJECT,
				ldapParameters.ldapDirectory.extIdAttribute)) {
			return StreamSupport.stream(cursor.spliterator(), false)
					.filter(r -> r.getType() == MessageTypeEnum.SEARCH_RESULT_ENTRY)
					.map(r -> ((SearchResultEntry) r).getEntry()).findFirst();
		}
	}

	/**
	 * Get group entry from DN considering group filter and ignoring incremental
	 * mode
	 * 
	 * @param ldapCon
	 * @param userDn
	 * @return
	 * @throws LdapException
	 */
	public Optional<Entry> getGroupFromDn(LdapConnection ldapCon, Dn groupDn) throws LdapException {
		try (PagedSearchResult cursor = super.findByFilterAndBaseDnAndScopeAndAttributes(ldapCon,
				groupFilter.getSearchFilter(ldapParameters), groupDn, SearchScope.OBJECT,
				ldapParameters.ldapDirectory.extIdAttribute)) {
			return StreamSupport.stream(cursor.spliterator(), false)
					.filter(r -> r.getType() == MessageTypeEnum.SEARCH_RESULT_ENTRY)
					.map(r -> ((SearchResultEntry) r).getEntry()).findFirst();
		}
	}

	/**
	 * Get split group from its name
	 * 
	 * @param ldapCon
	 * @return LDAP search result
	 * @throws LdapException
	 * @throws NullOrEmptySplitGroupName
	 */
	public PagedSearchResult findSplitGroup(LdapConnection ldapCon) throws LdapException {
		if (Strings.isNullOrEmpty(ldapParameters.splitDomain.relayMailboxGroup)) {
			throw new NullOrEmptySplitGroupName();
		}

		return super.findByFilterAndAttributes(ldapCon,
				groupFilter.getSearchFilterByName(ldapParameters, ldapParameters.splitDomain.relayMailboxGroup), "*");
	}

	public PagedSearchResult findByUserLogin(LdapConnection ldapCon, String userLogin) throws LdapException {
		return super.findByFilterAndAttributes(ldapCon, userFilter.getSearchFilterByName(ldapParameters, userLogin),
				ldapParameters.ldapDirectory.extIdAttribute);
	}
}
