/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.api.IAPIKeys;
import net.bluemind.authentication.api.incore.IInCoreAuthentication;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.ISessionsProvider;

public class ApiKeySessionProvider implements ISessionsProvider {

	private static final Logger logger = LoggerFactory.getLogger(TokenSessionProvider.class);

	private final IInCoreAuthentication coreAuth;

	public ApiKeySessionProvider() {
		this.coreAuth = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInCoreAuthentication.class);
	}

	@Override
	public Optional<SecurityContext> get(String token) {
		IAPIKeys keyService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IAPIKeys.class);
		return Optional.ofNullable(keyService.get(token)).map(apiKey -> {

			logger.info("[{}@{}] Building context for api key}", apiKey.subject, apiKey.domainUid);
			return coreAuth.buildContext(apiKey.sid, apiKey.domainUid, apiKey.subject);
		});
	}

}
