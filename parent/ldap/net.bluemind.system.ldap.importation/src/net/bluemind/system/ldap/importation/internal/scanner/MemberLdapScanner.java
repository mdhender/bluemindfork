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

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.directory.api.ldap.codec.decorators.SearchResultEntryDecorator;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.name.Dn;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.importation.commons.ICoreServices;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.managers.GroupManager;
import net.bluemind.system.importation.commons.managers.UserManager;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.i18n.Messages;
import net.bluemind.system.importation.search.PagedSearchResult;
import net.bluemind.system.importation.search.PagedSearchResult.LdapSearchException;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.search.LdapSearch;
import net.bluemind.system.ldap.importation.search.MemberLdapSearch;

public class MemberLdapScanner extends LdapScanner {
	private final MemberLdapSearch ldapSearch;

	public MemberLdapScanner(ImportLogger importLogger, LdapParameters ldapParameters, ItemValue<Domain> domain) {
		super(importLogger, ldapParameters, domain);
		this.ldapSearch = new MemberLdapSearch(ldapParameters);
	}

	public MemberLdapScanner(ImportLogger importLogger, ICoreServices coreService, LdapParameters ldapParameters,
			ItemValue<Domain> domain) {
		super(importLogger, coreService, ldapParameters, domain);
		this.ldapSearch = new MemberLdapSearch(ldapParameters);
	}

	@Override
	protected LdapSearch getLdapSearch() {
		return ldapSearch;
	}

	@Override
	protected Optional<Set<UuidMapper>> getSplitGroupMembers() {
		if (!ldapParameters.splitDomain.splitRelayEnabled || ldapParameters.splitDomain.relayMailboxGroup == null
				|| ldapParameters.splitDomain.relayMailboxGroup.isEmpty()) {
			return Optional.of(Collections.emptySet());
		}

		Set<UuidMapper> splitGroupMembers = new HashSet<>();
		Entry entry = null;
		try (PagedSearchResult cursor = ldapSearch.findByGroupName(ldapCon, getGroupMembersAttributeName())) {
			while (cursor.next()) {
				Response response = cursor.get();

				if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
					continue;
				}

				entry = ((SearchResultEntryDecorator) response).getEntry();
			}
		} catch (LdapException | CursorException | LdapSearchException e) {
			throw new ServerFault(e);
		}

		if (entry == null) {
			return Optional.of(Collections.emptySet());
		}

		Optional<? extends GroupManager> groupManager = getGroupManager(entry);
		if (!groupManager.isPresent()) {
			return Optional.of(Collections.emptySet());
		}

		Set<String> groupMembers = groupManager.get().getGroupMembers(getGroupMembersAttributeName());

		for (String groupMember : groupMembers) {
			Optional<Dn> memberDn = getMemberDn(groupMember);
			if (!memberDn.isPresent()) {
				importLogger.info(Messages.groupMemberNotFound(groupMember));
				continue;
			}

			Optional<UuidMapper> member = getUserUuidMapper(memberDn.get());
			if (member.isPresent()) {
				splitGroupMembers.add(member.get());
			} else {
				importLogger.info(Messages.groupMemberNotFound(groupMember));
			}
		}

		return Optional.of(Collections.unmodifiableSet(splitGroupMembers));
	}

	@Override
	protected void manageUserGroups(UserManager userManager) {
		// User group membership are not described in user LDAP object
	}
}
