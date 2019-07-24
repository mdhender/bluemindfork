/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.ldap.export.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.system.ldap.export.Activator;
import net.bluemind.system.ldap.export.enhancer.IEntityEnhancer;

public class DomainDirectoryGroup extends LdapObjects {
	private static final String RDN_ATTRIBUTE = "cn";

	public static class MembersList {
		// memberUid LDAP attribute values
		public List<String> memberUid = new ArrayList<>();
		// member LDAP attribute values
		public List<String> member = new ArrayList<>();
	}

	private final ItemValue<Domain> domain;
	private final ItemValue<Group> group;
	private final MembersList members;

	public static final List<String> ldapAttrsStringsValues = ImmutableList.of(//
			"objectclass", "bmUid", "bmHidden", "description", "mail", "member", "memberUid");

	public DomainDirectoryGroup(ItemValue<Domain> domain, ItemValue<Group> group, MembersList members) {
		this.domain = domain;
		this.group = group;
		this.members = members;
	}

	public DomainDirectoryGroup(ItemValue<Domain> domain, ItemValue<Group> group) {
		this.domain = domain;
		this.group = group;
		this.members = new MembersList();
	}

	@Override
	public String getDn() {
		String parentDn = new DomainDirectoryGroups(domain).getDn();
		return getRDn() + "," + parentDn;
	}

	@Override
	public String getRDn() {
		return RDN_ATTRIBUTE + "=" + group.value.name;
	}

	@Override
	public Entry getLdapEntry() throws ServerFault {
		Entry ldapEntry;

		try {
			ldapEntry = new DefaultEntry(getDn(), "objectclass: posixGroup", "objectclass: bmGroup");
			ldapEntry.add("bmUid", group.uid);
			ldapEntry.add("gidNumber", "-1");

			if (!Strings.isNullOrEmpty(group.value.description)) {
				ldapEntry.add("description", group.value.description);
			}

			ldapEntry.add("bmHidden", Boolean.toString(group.value.hidden));

			Email email = group.value.defaultEmail();
			if (email != null) {
				ldapEntry.add("mail", email.address);
			}

			if (!members.member.isEmpty()) {
				ldapEntry.add("member", members.member.toArray(new String[members.member.size()]));
			}

			if (!members.memberUid.isEmpty()) {
				ldapEntry.add("memberUid", members.memberUid.toArray(new String[members.memberUid.size()]));
			}

			for (IEntityEnhancer entityEnhancer : Activator.getEntityEnhancerHooks()) {
				Entry enhancedEntry = entityEnhancer.enhanceGroup(domain, group, members, ldapEntry);
				if (enhancedEntry != null) {
					ldapEntry = enhancedEntry;
				}
			}

			if (ldapEntry.get("bmUid") != null) {
				ldapEntry.removeAttributes("bmUid");
			}
			ldapEntry.add("bmUid", group.uid);
		} catch (LdapException e) {
			throw new ServerFault("Fail to manage group: " + getDn(), e);
		}

		return ldapEntry;
	}

	@Override
	public ModifyRequest getModifyRequest(Entry currentEntry) throws ServerFault {
		ModifyRequest modifyRequest = new ModifyRequestImpl();
		modifyRequest.setName(currentEntry.getDn());

		Entry entry = getLdapEntry();

		for (String attr : Stream.concat(ldapAttrsStringsValues.stream(), getEnhancerAttributeList().stream())
				.map(String::toLowerCase).collect(Collectors.toSet())) {
			modifyRequest = updateLdapAttribute(modifyRequest, currentEntry, entry, attr);
		}

		return modifyRequest;
	}

	private Collection<String> getEnhancerAttributeList() {
		return Activator.getEntityEnhancerHooks().stream().map(IEntityEnhancer::groupEnhancerAttributes)
				.filter(Objects::nonNull).flatMap(List::stream).collect(Collectors.toSet());
	}

	public List<String> getRemovedMembersUid(Entry entry) {
		List<String> removedUids = new ArrayList<>();

		Attribute attr = entry.get("memberUid");
		if (attr == null) {
			return removedUids;
		}

		return StreamSupport.stream(attr.spliterator(), false).filter(m -> !members.memberUid.contains(m.getString()))
				.map(m -> m.getString()).collect(Collectors.toList());
	}

	public List<String> getAddedMembersUid(Entry entry) {
		Attribute attr = entry.get("memberUid");
		if (attr == null) {
			return members.memberUid;
		}

		return members.memberUid.stream().filter(m -> !attr.contains(m)).collect(Collectors.toList());
	}
}
