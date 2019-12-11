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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.system.importation.commons.Parameters;

/**
 * Builder class simplifying the LDAP search cursor creation
 * 
 */

public class SearchCursorBuilder {
	private static final Logger logger = LoggerFactory.getLogger(SearchCursorBuilder.class);
	private final LdapConnection ldapCon;
	private Dn baseDn;
	private String filter;
	private SearchScope scope;
	private String[] attributes;
	private AliasDerefMode aliasDerefMode;
	private long sizeLimit;

	private SearchCursorBuilder(LdapConnection ldapCon, Parameters ldapParameters) {
		this.ldapCon = ldapCon;
		setDefaultValues(ldapParameters);
	}

	private void setDefaultValues(Parameters ldapParameters) {
		this.sizeLimit = 0;
		this.scope = SearchScope.SUBTREE;
		this.aliasDerefMode = AliasDerefMode.NEVER_DEREF_ALIASES;
		try {
			this.baseDn = ldapParameters.ldapDirectory.baseDn;
		} catch (Exception e) {
		}
		List<String> attributesAsList = new ArrayList<>();
		attributesAsList.addAll(Arrays.asList(new String[] { "*", "+", "modifyTimestamp" }));
		if (!Strings.isNullOrEmpty(ldapParameters.ldapDirectory.extIdAttribute)) {
			attributesAsList.add(ldapParameters.ldapDirectory.extIdAttribute);
		}
		this.attributes = attributesAsList.toArray(new String[0]);
	}

	public static SearchCursorBuilder withConnection(LdapConnection ldapCon, Parameters ldapParameters) {
		return new SearchCursorBuilder(ldapCon, ldapParameters);
	}

	public SearchCursorBuilder withBaseDn(Dn baseDn) {
		this.baseDn = baseDn;
		return this;
	}

	public SearchCursorBuilder withSearchFilter(String filter) {
		this.filter = filter;
		return this;
	}

	public SearchCursorBuilder withScope(SearchScope scope) {
		this.scope = scope;
		return this;
	}

	public SearchCursorBuilder withAttributes(String... attributes) {
		this.attributes = attributes;
		return this;
	}

	public SearchCursorBuilder withDerefAlias(AliasDerefMode aliasDerefMode) {
		this.aliasDerefMode = aliasDerefMode;
		return this;
	}

	public SearchCursorBuilder withSizeLimit(long sizeLimit) {
		this.sizeLimit = sizeLimit;
		return this;
	}

	public LdapSearchCursor execute() throws LdapException {
		SearchRequest searchRequest = new SearchRequestImpl();
		searchRequest.setBase(this.baseDn);
		if (null != filter) {
			try {
				searchRequest.setFilter(filter);
			} catch (LdapException e) {
				logger.warn("Cannot apply LDAP search filter {} : {}", filter, e.getMessage());
			}
		}
		searchRequest.setScope(this.scope);
		if (null != attributes && attributes.length > 0) {
			searchRequest.addAttributes(this.attributes);
		}
		searchRequest.setDerefAliases(this.aliasDerefMode);
		if (this.sizeLimit > 0) {
			searchRequest.setSizeLimit(0);
		}

		if (logger.isDebugEnabled()) {
			logger.debug(
					String.format("Executing LDAP search. BaseDn: %s, Filter: %s, Scope: %s, Attributes: %s, limit: %d",
							baseDn, filter, scope.name(), attributes.toString(), sizeLimit));
		}

		return new LdapSearchCursor(ldapCon.search(searchRequest));
	}

}
