/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.lib.ldap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NestedGroupHelper {
	private static final Logger logger = LoggerFactory.getLogger(NestedGroupHelper.class);

	private final LdapConProxy ldapCon;
	private final Dn baseDn;
	private final GroupMemberAttribute memberAttr;
	private final String groupFilter;
	private final String userLoginAttribute;
	private final String userUuidAttribute;

	public NestedGroupHelper(LdapConProxy ldapCon, Dn baseDn, GroupMemberAttribute memberAttr, String groupFilter,
			String userLoginAttribute, String userUuidAttribute) {
		this.ldapCon = ldapCon;
		this.baseDn = baseDn;
		this.memberAttr = memberAttr;
		this.groupFilter = groupFilter;
		this.userLoginAttribute = userLoginAttribute;
		this.userUuidAttribute = userUuidAttribute;
	}

	public Set<String> getUserMembers(Entry group) {
		Set<String> memberUuids = new HashSet<>();

		Set<Entry> members = getMembers(group);
		while (!members.isEmpty()) {
			members = members.stream().peek(entry -> getUserMemberUuid(entry).ifPresent(memberUuids::add))
					.map(this::getMembers).flatMap(set -> set.stream()).collect(Collectors.toSet());
		}

		return memberUuids;
	}

	private Optional<String> getUserMemberUuid(Entry entry) {
		if (entry.containsAttribute(memberAttr.name()) || !entry.containsAttribute(userUuidAttribute)
				|| isAGroup(entry)) {
			return Optional.empty();
		}

		return Optional.ofNullable(entry.get(userUuidAttribute).get().toString());
	}

	private boolean isAGroup(Entry entry) {
		try {
			return ldapCon.search(new SearchRequestImpl().setBase(entry.getDn()).setScope(SearchScope.OBJECT)
					.setFilter(groupFilter).setSizeLimit(1)).next();
		} catch (LdapException | CursorException e) {
			logger.warn("Unable to check if {} is a group. Assume user...", entry.getDn().getName(), e);
		}

		return false;
	}

	private Set<Entry> getMembers(Entry entry) {
		Attribute members = entry.get(memberAttr.name());
		if (members == null) {
			return Collections.emptySet();
		}

		return StreamSupport.stream(members.spliterator(), false).distinct().map(this::getMemberEntry)
				.filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
	}

	private Optional<Entry> getMemberEntry(Value<?> attribute) {
		String value = attribute.getString();

		if (memberAttr == GroupMemberAttribute.memberUid) {
			return getMemberDnFromUid(value);
		}

		try {
			return Optional.ofNullable(ldapCon.lookup(new Dn(value), memberAttr.name(), userUuidAttribute));
		} catch (LdapException e) {
			logger.warn("Unable to get entry DN {}, ignoring member...", value, e);
		}

		return Optional.empty();
	}

	private Optional<Entry> getMemberDnFromUid(String uid) {
		try {
			SearchCursor cursor = ldapCon.search(new SearchRequestImpl().setBase(baseDn).setScope(SearchScope.SUBTREE)
					.setDerefAliases(AliasDerefMode.NEVER_DEREF_ALIASES).setSizeLimit(1)
					.setFilter(String.format("(%s=%s)", userLoginAttribute, uid))
					.addAttributes(memberAttr.name(), userUuidAttribute));

			if (!cursor.next() || !cursor.isEntry()) {
				logger.warn("Unable to get DN from uid {}, ignoring member...", uid);
				return Optional.empty();
			}

			return Optional.of(cursor.getEntry());
		} catch (LdapException | CursorException e) {
			logger.warn("Unable to get DN from uid {}, ignoring member...", uid, e);
		}

		return Optional.empty();
	}
}
