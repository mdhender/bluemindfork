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

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.ldap.api.ConnectionStatus;
import net.bluemind.addressbook.ldap.api.LdapParameters;
import net.bluemind.addressbook.ldap.service.internal.utils.LdapHelper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;
import net.bluemind.role.api.BasicRoles;

public class LdapAddressbookValidator implements IValidator<AddressBookDescriptor> {

	public static class Factory implements IValidatorFactory<AddressBookDescriptor> {

		@Override
		public Class<AddressBookDescriptor> support() {
			return AddressBookDescriptor.class;
		}

		@Override
		public IValidator<AddressBookDescriptor> create(BmContext context) {
			return new LdapAddressbookValidator(context);
		}

	}

	private RBACManager rbacManager;

	public LdapAddressbookValidator(BmContext context) {
		rbacManager = new RBACManager(context);
	}

	@Override
	public void create(AddressBookDescriptor obj) throws ServerFault {
		validate(obj);
	}

	@Override
	public void update(AddressBookDescriptor oldValue, AddressBookDescriptor newValue) throws ServerFault {
		validate(newValue);
	}

	private void validate(AddressBookDescriptor descriptor) {

		LdapParameters.DirectoryType type = null;
		if (!descriptor.settings.isEmpty() && descriptor.settings.containsKey("type")) {
			try {
				type = LdapParameters.DirectoryType.valueOf(descriptor.settings.get("type"));
			} catch (IllegalArgumentException iae) {
			}
		}

		if (type != null) {
			// FEATEXTCON-13 check role
			rbacManager.forDomain(descriptor.domainUid).check(BasicRoles.ROLE_MANAGE_DOMAIN_LDAP_AB);

			// FEATEXTCON-16 check ldap connection
			String hostname = descriptor.settings.get("hostname");
			if (hostname == null || hostname.isEmpty()) {
				throw new ServerFault(String.format("Undefined %s server hostname", type.name()));
			}

			LdapParameters params = LdapParameters.create(type, hostname, descriptor.settings.get("protocol"),
					"true".equals(descriptor.settings.get("allCertificate")), descriptor.settings.get("baseDn"),
					descriptor.settings.get("loginDn"), descriptor.settings.get("loginPw"),
					descriptor.settings.get("filter"), descriptor.settings.get("entryUUID"));
			ConnectionStatus cs = LdapHelper.checkLDAPParameters(params);

			if (!cs.status) {
				throw new ServerFault(cs.errorMsg);
			}
		}

	}

}
