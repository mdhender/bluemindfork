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

public class DirectoryRoot extends LdapObjects {
	@Override
	public String getDn() {
		return getRDn();
	}

	@Override
	public String getRDn() {
		return "dc=local";
	}

	@Override
	public Entry getLdapEntry() throws ServerFault {
		try {
			Entry ldapEntry = new DefaultEntry(getDn(), "objectClass: organization", "objectClass: dcObject",
					"o: BlueMind", "dc: local", "description: BlueMind LDAP directory");

			return ldapEntry;
		} catch (LdapException e) {
			throw new ServerFault("Fail to manage LDAP dn: " + getDn(), e);
		}
	}

}
