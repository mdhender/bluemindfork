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

import java.io.IOException;
import java.util.Optional;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.ldap.GroupMemberAttribute;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.system.importation.commons.exceptions.DirectoryConnectionFailed;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.commons.scanner.Scanner;
import net.bluemind.system.importation.i18n.Messages;
import net.bluemind.system.importation.search.PagedSearchResult;
import net.bluemind.system.importation.search.PagedSearchResult.LdapSearchException;
import net.bluemind.system.importation.search.SearchCursorBuilder;
import net.bluemind.system.ldap.importation.internal.scanner.MemberLdapScanner;
import net.bluemind.system.ldap.importation.internal.scanner.MemberOfLdapScanner;
import net.bluemind.system.ldap.importation.internal.scanner.MemberUidLdapScanner;
import net.bluemind.system.ldap.importation.internal.tools.LdapHelper;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.internal.tools.UserManagerImpl;
import net.bluemind.system.ldap.importation.search.LdapGroupSearchFilter;
import net.bluemind.system.ldap.importation.search.LdapUserSearchFilter;

public class LdapScannerFactory {
	private static final Logger logger = LoggerFactory.getLogger(LdapScannerFactory.class);

	public static Optional<Scanner> getLdapScanner(ImportLogger importLogger, LdapParameters ldapParameters,
			ItemValue<Domain> domain) {
		try {
			if (isLdapContainMemberOf(ldapParameters)) {
				return Optional.of(new MemberOfLdapScanner(importLogger, ldapParameters, domain));
			}

			if (isLdapContainMember(ldapParameters)) {
				return Optional.of(new MemberLdapScanner(importLogger, ldapParameters, domain));
			}

			return Optional.of(new MemberUidLdapScanner(importLogger, ldapParameters, domain));
		} catch (DirectoryConnectionFailed | IOException | LdapException | CursorException
				| LdapSearchException failure) {
			logger.error(failure.getMessage());
			importLogger.error(Messages.directoriesConnectionFailed(Optional.of(failure)));
			return Optional.empty();
		} catch (Exception e) {
			importLogger.reportException(e);
			throw new ServerFault(e);
		}
	}

	private static boolean isLdapContainMember(LdapParameters ldapParameters)
			throws IOException, LdapException, CursorException, LdapSearchException {
		try (LdapConProxy ldapCon = LdapHelper.connectLdap(ldapParameters)) {
			try (PagedSearchResult cursor = SearchCursorBuilder.withConnection(ldapCon, ldapParameters)
					.withSearchFilter("(&" + new LdapGroupSearchFilter().getSearchFilter(ldapParameters) + "("
							+ GroupMemberAttribute.member.name() + "=*))")
					.withAttributes(GroupMemberAttribute.member.name()).withSizeLimit(5).execute()) {
				return cursor.next();
			}
		}
	}

	private static boolean isLdapContainMemberOf(LdapParameters ldapParameters)
			throws IOException, LdapException, CursorException, LdapSearchException {
		try (LdapConProxy ldapCon = LdapHelper.connectLdap(ldapParameters)) {
			try (PagedSearchResult cursor = SearchCursorBuilder.withConnection(ldapCon, ldapParameters)
					.withSearchFilter("(&" + new LdapUserSearchFilter().getSearchFilter(ldapParameters) + "("
							+ UserManagerImpl.LDAP_MEMBER_OF + "=*))")
					.withAttributes(UserManagerImpl.LDAP_MEMBER_OF).withSizeLimit(5).execute()) {
				return cursor.next();
			}
		}
	}
}
