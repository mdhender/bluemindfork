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
package net.bluemind.addressbook.ldap.service.internal.utils;

import net.bluemind.addressbook.ldap.api.fault.LdapAddressBookErrorCode;

@SuppressWarnings("serial")
public class LdapAddressBookFault extends RuntimeException {

	public LdapAddressBookErrorCode errorCode;

	public LdapAddressBookFault(String s, LdapAddressBookErrorCode ec) {
		super(s);
		this.errorCode = ec;
	}

}
