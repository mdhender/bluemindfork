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
package net.bluemind.system.ldap.importation.internal.scanner;

import java.util.Optional;

import org.apache.directory.api.ldap.model.name.Dn;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.importation.commons.ICoreServices;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.search.PagedSearchResult;
import net.bluemind.system.ldap.importation.internal.tools.GroupManagerImpl;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.search.MemberUidLdapSearch;

public class MemberUidLdapScanner extends MemberLdapScanner {
	private final MemberUidLdapSearch ldapSearch;

	public MemberUidLdapScanner(ImportLogger importLogger, LdapParameters ldapParameters, ItemValue<Domain> domain) {
		super(importLogger, ldapParameters, domain);

		this.ldapSearch = new MemberUidLdapSearch(ldapParameters);
	}

	public MemberUidLdapScanner(ImportLogger importLogger, ICoreServices coreService, LdapParameters ldapParameters,
			ItemValue<Domain> domain) {
		super(importLogger, coreService, ldapParameters, domain);

		this.ldapSearch = new MemberUidLdapSearch(ldapParameters);
	}

	@Override
	protected String getGroupMembersAttributeName() {
		return GroupManagerImpl.LDAP_MEMBER_UID;
	}

	@Override
	protected Optional<Dn> getMemberDnFromLogin(String userLogin) {
		try (PagedSearchResult cursor = ldapSearch.getUserFromLogin(ldapCon, userLogin)) {
			if (cursor.next()) {
				return Optional.of(cursor.getEntry().getDn());
			}
		} catch (Exception e) {
			// If LDAP search fail, return empty optional
		}

		return Optional.empty();
	}
}
