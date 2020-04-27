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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.i18n.Messages;
import net.bluemind.system.importation.search.PagedSearchResult;
import net.bluemind.system.importation.search.PagedSearchResult.LdapSearchException;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.internal.tools.LdapUuidMapper;
import net.bluemind.system.ldap.importation.internal.tools.UserManagerImpl;

public class MemberOfLdapSearch extends LdapSearch {
	Logger logger = LoggerFactory.getLogger(MemberOfLdapSearch.class);
	private ImportLogger importLogger;

	public MemberOfLdapSearch(ImportLogger importLogger, LdapParameters ldapParameters) {
		super(ldapParameters);
		this.importLogger = importLogger;
	}

	public List<UuidMapper> getUserGroupsByMemberUuid(LdapConnection ldapCon, LdapParameters ldapParameters,
			Entry entry) {
		Attribute memberOf = entry.get(UserManagerImpl.LDAP_MEMBER_OF);
		if (null == memberOf) {
			return Collections.<UuidMapper>emptyList();
		}

		return getGroupsByMemberOfUuid(ldapCon, ldapParameters, memberOf);
	}

	private List<UuidMapper> getGroupsByMemberOfUuid(LdapConnection ldapCon, LdapParameters ldapParameters,
			Attribute memberOf) {

		return StreamSupport.stream(memberOf.spliterator(), false)
				.map(member -> getUserGroupUuidMapper(ldapCon, ldapParameters, member.getString()))
				.filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
	}

	private Optional<UuidMapper> getUserGroupUuidMapper(LdapConnection ldapCon, LdapParameters ldapParameters,
			String memberOfValue) {
		Entry entry = null;

		try {
			entry = ldapCon.lookup(memberOfValue, ldapParameters.ldapDirectory.extIdAttribute);
		} catch (LdapInvalidDnException lide) {
		} catch (LdapException e) {
			logger.warn("Cannot lookup memberOfGroupGuid {}", e.getMessage());
			return Optional.empty();
		}

		try {
			// If LDAP_MEMBER_OF value is not a valid DN, try to search group by name
			SearchRequest searchRequest = new SearchRequestImpl().setBase(ldapParameters.ldapDirectory.baseDn)
					.setFilter(groupFilter.getSearchFilter(ldapParameters, Optional.empty(), null, memberOfValue))
					.setScope(SearchScope.SUBTREE).addAttributes(ldapParameters.ldapDirectory.extIdAttribute)
					.setSizeLimit(1);
			try (PagedSearchResult result = new PagedSearchResult(ldapCon, searchRequest)) {
				if (result.next()) {
					entry = result.getEntry();
				}
			}
		} catch (CursorException | LdapSearchException | LdapException e) {
			logger.warn("Cannot lookup memberOfGroupGuid {}", e.getMessage());
			return Optional.empty();
		}

		if (entry == null) {
			return Optional.empty();
		}

		Attribute extIdAttribut = entry.get(ldapParameters.ldapDirectory.extIdAttribute);
		if (extIdAttribut == null) {
			logger.warn("Unable to find external attribute for group: " + entry.getDn().getName());
			importLogger.warning(Messages.failGetGroupExternalId(entry.getDn().getName()));
			return Optional.empty();
		}

		return Optional.of(LdapUuidMapper.fromEntry(ldapParameters.ldapDirectory.extIdAttribute, entry));
	}

}
