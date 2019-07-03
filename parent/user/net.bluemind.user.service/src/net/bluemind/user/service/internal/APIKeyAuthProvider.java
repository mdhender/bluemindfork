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
package net.bluemind.user.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.provider.IAuthProvider;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class APIKeyAuthProvider implements IAuthProvider {

	private static final Logger logger = LoggerFactory.getLogger(APIKeyAuthProvider.class);

	@Override
	public int priority() {
		return Integer.MAX_VALUE - 1;
	}

	@Override
	public AuthResult check(IAuthContext authContext) throws ServerFault {
		ItemValue<Domain> domain = authContext.getDomain();
		ItemValue<User> user = authContext.getUser();
		String login = authContext.getRealUserLogin();
		String password = authContext.getUserPassword();

		if (domain == null || user == null || login == null || password == null) {
			return AuthResult.UNKNOWN;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("check {} @ {} with password {}", login, domain.value.name, password);
		}

		UserService userService = (UserService) ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domain.uid);
		return userService.checkApiKey(user.uid, password) ? AuthResult.YES : AuthResult.UNKNOWN;
	}
}
