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

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.importation.commons.ICoreServices;
import net.bluemind.system.importation.commons.managers.UserManager;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.search.LdapSearch;

public class MemberLdapScanner extends LdapScanner {
	protected final LdapSearch ldapSearch;

	public MemberLdapScanner(ImportLogger importLogger, LdapParameters ldapParameters, ItemValue<Domain> domain) {
		super(importLogger, ldapParameters, domain);
		this.ldapSearch = new LdapSearch(ldapParameters);
	}

	public MemberLdapScanner(ImportLogger importLogger, ICoreServices coreService, LdapParameters ldapParameters,
			ItemValue<Domain> domain) {
		super(importLogger, coreService, ldapParameters, domain);
		this.ldapSearch = new LdapSearch(ldapParameters);
	}

	@Override
	protected LdapSearch getLdapSearch() {
		return ldapSearch;
	}

	@Override
	protected void manageUserGroups(UserManager userManager) {
		// User group membership are not described in user LDAP object
	}
}
