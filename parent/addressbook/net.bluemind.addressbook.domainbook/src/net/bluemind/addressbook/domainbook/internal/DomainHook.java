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
package net.bluemind.addressbook.domainbook.internal;

import java.util.Arrays;

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.addressbook.domainbook.DomainAddressBook;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.DomainHookAdapter;

public class DomainHook extends DomainHookAdapter {

	@Override
	public void onCreated(BmContext context, ItemValue<Domain> domain) throws ServerFault {

		BmContext systemContext = context.su();

		IAddressBooksMgmt mgmt = systemContext.provider().instance(IAddressBooksMgmt.class);

		mgmt.create(DomainAddressBook.getIdentifier(domain.uid),
				AddressBookDescriptor.createSystem("$$domain.addressbook$$", domain.uid, domain.uid), true);
		systemContext.provider().instance(IContainerManagement.class, DomainAddressBook.getIdentifier(domain.uid))
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(domain.uid, Verb.Read)));
	}

}
