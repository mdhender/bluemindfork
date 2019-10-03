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

import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.api.incore.IInCoreAuthentication;
import net.bluemind.authentication.service.tokens.TokensStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.ISessionsProvider;

public class TokenSessionProvider implements ISessionsProvider {

	private static final Logger logger = LoggerFactory.getLogger(TokenSessionProvider.class);

	private IInCoreAuthentication coreAuth;

	public TokenSessionProvider() {
		ServerSideServiceProvider apis = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		this.coreAuth = apis.instance(IInCoreAuthentication.class);
	}

	@Override
	public Optional<SecurityContext> get(String token) {
		return Optional.ofNullable(TokensStore.get().byKey(token)).map(tok -> {
			if (tok.expiresTimestamp < System.currentTimeMillis()) {
				logger.warn("[{}@{}] Token {} is expired since {}", tok.subjectUid, tok.subjectDomain, tok.key,
						new Date(tok.expiresTimestamp));
				return null;
			}
			logger.info("[{}@{}] Rebuilding context for token {}", tok.subjectUid, tok.subjectDomain, tok.key);
			return coreAuth.buildContext(tok.subjectDomain, tok.subjectUid);
		});
	}

}
