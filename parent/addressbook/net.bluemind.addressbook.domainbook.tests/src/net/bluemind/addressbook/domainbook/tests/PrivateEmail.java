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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.addressbook.domainbook.tests;

import net.bluemind.addressbook.api.VCardChanges.ItemAdd;
import net.bluemind.addressbook.api.VCardChanges.ItemDelete;
import net.bluemind.addressbook.api.VCardChanges.ItemModify;
import net.bluemind.addressbook.domainbook.IDomainAddressBookHook;
import net.bluemind.core.api.fault.ServerFault;

public class PrivateEmail implements IDomainAddressBookHook {

	@Override
	public void beforeAdd(ItemAdd add) throws ServerFault {
		try {
			add.value.communications.emails.removeIf(e -> e.value.indexOf("-private") >= 0);
		} catch (Exception e) {
			// yeah yeah
		}
	}

	@Override
	public void beforeDelete(ItemDelete rm) throws ServerFault {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeUpdate(ItemModify mod) throws ServerFault {
		try {
			mod.value.communications.emails.removeIf(e -> e.value.indexOf("-private") >= 0);
		} catch (Exception e) {
			// yeah yeah
		}

	}

}
