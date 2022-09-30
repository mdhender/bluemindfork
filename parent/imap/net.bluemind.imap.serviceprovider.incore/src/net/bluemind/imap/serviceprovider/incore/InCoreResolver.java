/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.serviceprovider.incore;

import net.bluemind.config.Token;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.imap.serviceprovider.IServiceProviderResolver;

public class InCoreResolver implements IServiceProviderResolver {

	private ServerSideServiceProvider ssp;

	public InCoreResolver() {
		this.ssp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	@Override
	public IServiceProvider resolve(String authKey) {
		if (authKey != null) {
			SecurityContext sec = (authKey.equals(Token.admin0())) //
					? SecurityContext.SYSTEM //
					: Sessions.get().getIfPresent(authKey);
			return ServerSideServiceProvider.getProvider(sec);
		}
		return ssp;
	}

}
