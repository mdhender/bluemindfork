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

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

public class DomainDirectoryUsers extends LdapObjects {
	private static final String RDN_ATTRIBUTE = "ou";
	private static final String RDN_VALUE = "users";

	private final ItemValue<Domain> domain;

	public DomainDirectoryUsers(ItemValue<Domain> domain) {
		this.domain = domain;
	}

	@Override
	public Entry getLdapEntry() throws ServerFault {
		Entry ldapEntry;

		try {
			ldapEntry = new DefaultEntry(getDn(), "objectclass: organizationalUnit", "ou: " + RDN_VALUE,
					"description: " + domain.value.name + " domain users");
		} catch (LdapException e) {
			throw new ServerFault("Fail to manage LDAP dn: " + getDn(), e);
		}

		return ldapEntry;
	}

	@Override
	public String getDn() {
		String parentDn = new DomainDirectoryRoot(domain).getDn();
		return getRDn() + "," + parentDn;
	}

	@Override
	public String getRDn() {
		return RDN_ATTRIBUTE + "=" + RDN_VALUE;
	}
}
