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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.i18n.Messages;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.internal.tools.LdapUuidMapper;
import net.bluemind.system.ldap.importation.internal.tools.UserManagerImpl;

public class MemberOfLdapSearch extends LdapSearch {
	Logger logger = LoggerFactory.getLogger(MemberOfLdapSearch.class);
	private ImportLogger importLogger;

	public MemberOfLdapSearch(ImportLogger importLogger, LdapParameters ldapParameters) {
		super(ldapParameters, new LdapGroupSearchFilter(), new LdapUserSearchFilter());
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

		List<UuidMapper> userGroups = new ArrayList<>();
		Iterator<Value<?>> iterator = memberOf.iterator();
		while (iterator.hasNext()) {
			String memberOfValue = iterator.next().getString();

			EntryCursor results = null;
			try {
				results = getUserGroups(ldapCon, ldapParameters, memberOfValue);
				if (results.next()) {
					Entry groupEntry = results.get();

					Attribute extIdAttribut = groupEntry.get(ldapParameters.ldapDirectory.extIdAttribute);
					if (extIdAttribut == null) {
						logger.warn("Unable to find external attribut for group: " + groupEntry.getDn().getName());
						importLogger.warning(Messages.failGetGroupExternalId(groupEntry.getDn().getName()));
						continue;
					}
					userGroups.add(LdapUuidMapper.fromEntry(ldapParameters.ldapDirectory.extIdAttribute, groupEntry));
				}

			} catch (Exception e) {
				logger.warn("Cannot lookup memberOfGroupGuid {}", e.getMessage());
			} finally {
				if (null != results && !results.isClosed()) {
					results.close();
				}
			}
		}

		return userGroups;
	}

	private EntryCursor getUserGroups(LdapConnection ldapCon, LdapParameters ldapParameters, String memberOfValue)
			throws LdapException, LdapInvalidDnException {

		try {
			Dn memberOfValueDn = new Dn(memberOfValue);
			return ldapCon.search(memberOfValueDn, ldapParameters.ldapDirectory.groupFilter, SearchScope.OBJECT,
					ldapParameters.ldapDirectory.extIdAttribute);
		} catch (LdapInvalidDnException lide) {
			// If LDAP_MEMBER_OF value is not a valid DN, try to
			// search
			// group by name
			return ldapCon.search(ldapParameters.ldapDirectory.baseDn,
					groupFilter.getSearchFilter(ldapParameters, Optional.empty(), null, memberOfValue),
					SearchScope.SUBTREE, ldapParameters.ldapDirectory.extIdAttribute);
		}
	}

}
