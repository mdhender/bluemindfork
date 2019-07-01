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
package net.bluemind.addressbook.service;

import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.addressbook.service.internal.AddressBooksMgmt;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class AddressBooksMgmtFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IAddressBooksMgmt> {

	public AddressBooksMgmtFactory() {

	}

	private IAddressBooksMgmt getService(BmContext bmContext) throws ServerFault {

		return new AddressBooksMgmt(bmContext);
	}

	@Override
	public Class<IAddressBooksMgmt> factoryClass() {
		return IAddressBooksMgmt.class;
	}

	@Override
	public IAddressBooksMgmt instance(BmContext context, String... params) throws ServerFault {
		return getService(context);
	}
}
