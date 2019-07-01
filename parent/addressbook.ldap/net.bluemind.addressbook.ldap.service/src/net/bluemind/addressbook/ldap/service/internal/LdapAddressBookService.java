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
package net.bluemind.addressbook.ldap.service.internal;

import net.bluemind.addressbook.ldap.api.ConnectionStatus;
import net.bluemind.addressbook.ldap.api.ILdapAddressBook;
import net.bluemind.addressbook.ldap.api.LdapParameters;
import net.bluemind.addressbook.ldap.service.internal.utils.LdapHelper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;

public class LdapAddressBookService implements ILdapAddressBook {

	private BmContext context;

	public LdapAddressBookService(BmContext context) {
		this.context = context;
	}

	@Override
	public ConnectionStatus testConnection(LdapParameters params) throws ServerFault {
		if (!context.getSecurityContext().isDomainGlobal()
				&& !context.getSecurityContext().getRoles().contains(SecurityContext.ROLE_ADMIN)) {
			throw new ServerFault("Only admin users can test LDAP parameters", ErrorCode.FORBIDDEN);
		}

		return LdapHelper.checkLDAPParameters(params);
	}

}
