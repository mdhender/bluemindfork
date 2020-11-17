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

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.internal.tools.LdapUuidMapper;
import net.bluemind.system.ldap.importation.internal.tools.UserManagerImpl;

public class MemberOfLdapSearch extends LdapSearch {
	Logger logger = LoggerFactory.getLogger(MemberOfLdapSearch.class);

	public MemberOfLdapSearch(LdapParameters ldapParameters) {
		super(ldapParameters);
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
			entry = ldapCon.lookup(new Dn(memberOfValue), ldapParameters.ldapDirectory.extIdAttribute);
		} catch (LdapInvalidDnException e) {
			logger.warn("Invalid group DN {}", memberOfValue, e);
			return Optional.empty();
		} catch (LdapException e) {
			logger.warn("Cannot find group {}", memberOfValue, e);
			return Optional.empty();
		}

		if (entry == null) {
			return Optional.empty();
		}

		Attribute extIdAttribut = entry.get(ldapParameters.ldapDirectory.extIdAttribute);
		if (extIdAttribut == null) {
			logger.warn("Unable to find external attribute for group: " + entry.getDn().getName());
			return Optional.empty();
		}

		return Optional.of(LdapUuidMapper.fromEntry(ldapParameters.ldapDirectory.extIdAttribute, entry));
	}

}
