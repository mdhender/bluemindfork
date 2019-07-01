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
import java.util.Set;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.importation.commons.ICoreServices;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.managers.UserManager;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.search.LdapSearch;
import net.bluemind.system.ldap.importation.search.MemberOfLdapSearch;

public class MemberOfLdapScanner extends LdapScanner {
	private final MemberOfLdapSearch ldapSearch;

	public MemberOfLdapScanner(ImportLogger importLogger, LdapParameters ldapParameters, ItemValue<Domain> domain) {
		super(importLogger, ldapParameters, domain);
		this.ldapSearch = new MemberOfLdapSearch(importLogger, ldapParameters);
	}

	public MemberOfLdapScanner(ImportLogger importLogger, ICoreServices coreService, LdapParameters ldapParameters,
			ItemValue<Domain> domain) {
		super(importLogger, coreService, ldapParameters, domain);
		this.ldapSearch = new MemberOfLdapSearch(importLogger, ldapParameters);
	}

	@Override
	protected Optional<Set<UuidMapper>> getSplitGroupMembers() {
		return Optional.empty();
	}

	@Override
	protected LdapSearch getLdapSearch() {
		return ldapSearch;
	}

	@Override
	protected void manageUserGroups(UserManager userManager) {
		manageUserGroups(ldapCon, coreService, userManager, this::getUuidMapperFromExtId);
	}
}
