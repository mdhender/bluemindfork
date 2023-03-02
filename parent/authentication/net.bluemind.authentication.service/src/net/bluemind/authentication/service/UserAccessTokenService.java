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

import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.AccessTokenInfo;
import net.bluemind.authentication.api.AccessTokenInfo.TokenStatus;
import net.bluemind.authentication.api.IUserAccessToken;
import net.bluemind.authentication.api.RefreshToken;
import net.bluemind.authentication.persistence.UserRefreshTokenStore;
import net.bluemind.authentication.service.internal.IOpenIdAuthFlow;
import net.bluemind.authentication.service.internal.OpenIdAuthFlowFactory;
import net.bluemind.authentication.service.internal.OpenIdException;
import net.bluemind.authentication.service.internal.OpenIdFlow;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.UserAccessToken;
import net.bluemind.core.rest.BmContext;
import net.bluemind.system.api.ExternalSystem;
import net.bluemind.system.api.IExternalSystem;

public class UserAccessTokenService implements IUserAccessToken {

	private final BmContext context;
	private final UserRefreshTokenStore store;
	private static final Logger logger = LoggerFactory.getLogger(UserAccessTokenService.class);

	public UserAccessTokenService(BmContext context) {
		this.context = context;
		this.store = new UserRefreshTokenStore(context.getDataSource(), context.getSecurityContext().getSubject());
	}

	@Override
	public AccessTokenInfo getTokenInfo(String externalSystem) {
		RBACManager.forContext(context).checkNotAnoynmous();

		if (externalSystem == null) {
			return AccessTokenInfo.noTokenNeeded();
		}

		ExternalSystem extSystem = context.su().provider().instance(IExternalSystem.class)
				.getExternalSystem(externalSystem);
		if (extSystem == null || !extSystem.authKind.name().startsWith("OPEN_ID")) {
			return AccessTokenInfo.noTokenNeeded();
		}

		Optional<UserAccessToken> userAccessToken = context.getSecurityContext().getUserAccessToken(externalSystem);
		if (!userAccessToken.isEmpty()) {
			return AccessTokenInfo.tokenValid();
		}

		RefreshToken refreshToken = store.get(externalSystem);
		if (refreshToken != null) {
			try {
				AccessTokenInfo refreshOpenIdToken = new OpenIdFlow(context)
						.refreshOpenIdToken(context.getSecurityContext().getSubject(), refreshToken);
				if (refreshOpenIdToken.status == TokenStatus.TOKEN_OK) {
					return refreshOpenIdToken;
				}
			} catch (OpenIdException e) {
				logger.warn("Cannot refresh token", e.getMessage());
			}

		}
		return OpenIdAuthFlowFactory.getFlow(context, extSystem.authKind).initalizeOpenIdAuthentication(extSystem);

	}

	@Override
	public AccessTokenInfo authCodeReceived(String state, String code) {
		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("Operation is only permitted for admin0", ErrorCode.PERMISSION_DENIED);
		}
		logger.debug("Received authToken for openid connect state {}", state);
		OpenIdContext openIdContext = OpenIdContextCache.get(context).getIfPresent(state);
		if (openIdContext == null) {
			logger.warn("Cannot find OpenId context {}", state);
			AccessTokenInfo info = new AccessTokenInfo();
			info.status = TokenStatus.TOKEN_NOT_VALID;
			return info;
		}

		JsonObject jwtToken = null;
		IOpenIdAuthFlow flow = OpenIdAuthFlowFactory.getFlow(context, openIdContext.authKind);
		try {
			jwtToken = flow.getAccessTokenByCode(code, openIdContext);
		} catch (OpenIdException e) {
			logger.warn("Cannot retrieve access token for {}@{}. code: {}", openIdContext.userUid,
					openIdContext.systemIdentifier, e);
			AccessTokenInfo info = new AccessTokenInfo();
			info.status = TokenStatus.TOKEN_NOT_VALID;
			return info;
		}

		String refreshToken = jwtToken.containsKey("refresh_token") ? jwtToken.getString("refresh_token") : null;

		flow.storeAccessToken(openIdContext.userUid, openIdContext.systemIdentifier, jwtToken);
		flow.storeRefreshToken(openIdContext, refreshToken);

		return AccessTokenInfo.tokenValid();
	}

}