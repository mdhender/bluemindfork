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
package net.bluemind.system.ldap.importation;

import java.util.Optional;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.commons.scanner.Scanner;
import net.bluemind.system.importation.search.LdapSearchCursor;
import net.bluemind.system.importation.search.SearchCursorBuilder;
import net.bluemind.system.ldap.importation.internal.scanner.MemberLdapScanner;
import net.bluemind.system.ldap.importation.internal.scanner.MemberOfLdapScanner;
import net.bluemind.system.ldap.importation.internal.scanner.MemberUidLdapScanner;
import net.bluemind.system.ldap.importation.internal.tools.GroupManagerImpl;
import net.bluemind.system.ldap.importation.internal.tools.LdapHelper;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.internal.tools.UserManagerImpl;
import net.bluemind.system.ldap.importation.search.LdapGroupSearchFilter;
import net.bluemind.system.ldap.importation.search.LdapUserSearchFilter;

public class LdapScannerFactory {
	public static Scanner getLdapScanner(ImportLogger importLogger, LdapParameters ldapParameters,
			ItemValue<Domain> domain) throws ServerFault {
		if (isLdapContainMemberOf(ldapParameters)) {
			return new MemberOfLdapScanner(importLogger, ldapParameters, domain);
		}

		if (isLdapContainMember(ldapParameters)) {
			return new MemberLdapScanner(importLogger, ldapParameters, domain);
		}

		return new MemberUidLdapScanner(importLogger, ldapParameters, domain);
	}

	private static boolean isLdapContainMember(LdapParameters ldapParameters) throws ServerFault {
		try (LdapConProxy ldapCon = LdapHelper.connectLdap(ldapParameters)) {
			try (LdapSearchCursor cursor = SearchCursorBuilder.withConnection(ldapCon, ldapParameters)
					.withSearchFilter("(&"
							+ new LdapGroupSearchFilter().getSearchFilter(ldapParameters, Optional.empty(), null, null)
							+ "(" + GroupManagerImpl.LDAP_MEMBER + "=*))")
					.withAttributes(GroupManagerImpl.LDAP_MEMBER).withSizeLimit(5).execute()) {
				return cursor.next();
			}
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	private static boolean isLdapContainMemberOf(LdapParameters ldapParameters) throws ServerFault {
		try (LdapConProxy ldapCon = LdapHelper.connectLdap(ldapParameters)) {
			try (LdapSearchCursor cursor = SearchCursorBuilder.withConnection(ldapCon, ldapParameters)
					.withSearchFilter("(&"
							+ new LdapUserSearchFilter().getSearchFilter(ldapParameters, Optional.empty(), null, null)
							+ "(" + UserManagerImpl.LDAP_MEMBER_OF + "=*))")
					.withAttributes(UserManagerImpl.LDAP_MEMBER_OF).withSizeLimit(5).execute()) {
				return cursor.next();
			}
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}
}
