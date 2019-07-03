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
package net.bluemind.user.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.user.api.IUser;

public class UserServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IUser> {

	private InCoreUserServiceFactory serverFactory = new InCoreUserServiceFactory();

	@Override
	public Class<IUser> factoryClass() {
		return IUser.class;
	}

	@Override
	public IUser instance(BmContext context, String... params) throws ServerFault {
		return serverFactory.instance(context, params);
	}

}
