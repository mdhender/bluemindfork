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
package net.bluemind.user.accounts.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.user.accounts.service.internal.UserAccountsService;
import net.bluemind.user.api.IInternalUserExternalAccount;

public class InternalUserAccountsServiceFactory extends AbstractAccountServiceFactory<IInternalUserExternalAccount>
		implements ServerSideServiceProvider.IServerSideServiceFactory<IInternalUserExternalAccount> {

	@Override
	public Class<IInternalUserExternalAccount> factoryClass() {
		return IInternalUserExternalAccount.class;
	}

	@Override
	protected IInternalUserExternalAccount instanceImpl(BmContext context, Container container, Item item)
			throws ServerFault {
		return new UserAccountsService(context, container, item);
	}

}
