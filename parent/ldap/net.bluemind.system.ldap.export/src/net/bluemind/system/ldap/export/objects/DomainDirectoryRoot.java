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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;

import com.google.common.collect.ImmutableList;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

public class DomainDirectoryRoot extends LdapObjects {
	private static final String RDN_ATTRIBUTE = "dc";

	private final ItemValue<Domain> domain;

	public static final List<String> ldapAttrsStringsValues = ImmutableList.of(//
			"objectclass", "o", "description");

	public DomainDirectoryRoot(ItemValue<Domain> domain) {
		this.domain = domain;
	}

	@Override
	public Entry getLdapEntry() throws ServerFault {

		try {
			Entry ldapEntry = new DefaultEntry(getDn(), "objectClass: organization", "objectClass: dcObject",
					"objectClass: bmDomain", "o: " + domain.value.label, "dc: " + domain.value.name);
			ldapEntry.add("bmVersion", "0");

			if (domain.value.description != null && !domain.value.description.trim().isEmpty()) {
				ldapEntry.add("description", domain.value.description);
			}

			return ldapEntry;
		} catch (LdapException e) {
			throw new ServerFault("Fail to manage LDAP dn: " + getDn(), e);
		}
	}

	@Override
	public String getDn() {
		String parentDn = new DirectoryRoot().getDn();
		return getRDn() + "," + parentDn;
	}

	@Override
	public String getRDn() {
		return RDN_ATTRIBUTE + "=" + domain.value.name;
	}

	@Override
	public ModifyRequest getModifyRequest(Entry currentEntry) throws ServerFault {
		ModifyRequest modifyRequest = new ModifyRequestImpl();
		modifyRequest.setName(currentEntry.getDn());

		Entry entry = getLdapEntry();

		for (String attr : ldapAttrsStringsValues.stream().map(String::toLowerCase).collect(Collectors.toSet())) {
			modifyRequest = updateLdapAttribute(modifyRequest, currentEntry, entry, attr);
		}

		return modifyRequest;
	}
}
