/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.authentication.service;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.api.ISecurityToken;
import net.bluemind.authentication.service.tokens.TokensStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;

public class SecurityTokenService implements ISecurityToken {

	private static final Logger logger = LoggerFactory.getLogger(SecurityTokenService.class);

	private final String sid;

	public SecurityTokenService(BmContext context, String sid) {
		this.sid = sid;
	}

	@Override
	public void upgrade() {
		SecurityContext sec = Sessions.get().getIfPresent(sid);
		if (sec == null) {
			throw ServerFault.notFound("sid '" + sid + "' is missing");
		}
		logger.info("{} (Upgrade token)", sid);
		TokensStore.get().add(new Token(sid, sec.getSubject(), sec.getContainerUid()));
	}

	@Override
	public void renew() {
		Token token = TokensStore.get().byKey(sid);
		if (token == null) {
			throw ServerFault.notFound("token '" + sid + "' is missing");
		}
		token.renew();
		TokensStore.get().add(token);
	}

	@Override
	public void destroy() {
		Token token = TokensStore.get().remove(sid);
		if (token == null) {
			logger.warn("Token " + sid + " was unknown");
		} else {
			Sessions.get().invalidate(sid);
		}
	}

	public static class Factory implements ServerSideServiceProvider.IServerSideServiceFactory<ISecurityToken> {

		@Override
		public Class<ISecurityToken> factoryClass() {
			return ISecurityToken.class;
		}

		@Override
		public ISecurityToken instance(BmContext context, String... params) throws ServerFault {
			if (params.length != 1) {
				throw new ServerFault("sid parameter expected, params are " + Arrays.toString(params));
			}
			String sid = params[0];
			if (!context.getSecurityContext().isAdmin() && !sid.equals(context.getSecurityContext().getSessionId())) {
				throw new ServerFault("Admin or active session required.");
			}
			return new SecurityTokenService(context, params[0]);
		}

	}

}
