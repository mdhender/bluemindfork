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
package net.bluemind.authentication.service.internal;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.AccessTokenInfo;
import net.bluemind.authentication.service.OpenIdContext;
import net.bluemind.authentication.service.OpenIdContextCache;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.service.internal.IInCoreDomainSettings;
import net.bluemind.system.api.ExternalSystem;
import net.bluemind.system.api.ExternalSystem.AuthKind;

public class OpenIdPkceFlow extends OpenIdFlow implements IOpenIdAuthFlow {

	protected OpenIdPkceFlow(BmContext context) {
		super(context);
	}

	public AccessTokenInfo initalizeOpenIdAuthentication(ExternalSystem system) {
		String endpointKey = system.identifier + "_endpoint";
		String applicationIdKey = system.identifier + "_appid";
		String applicationSecretKey = system.identifier + "_secret";
		String tokenEndpointKey = system.identifier + "_tokenendpoint";

		IInCoreDomainSettings settingsService = context.su().provider().instance(IInCoreDomainSettings.class,
				context.getSecurityContext().getContainerUid());
		Map<String, String> settings = settingsService.get();

		String contextId = UUID.randomUUID().toString();
		String secret = generateCodeVerifier();

		String externalAuthEndPointUrl = settings.get(endpointKey);
		String applicationId = settings.get(applicationIdKey);
		String internalRedirectUrl = "https://" + getExternalUrl(settingsService) + "/bm-openid/auth/"
				+ system.identifier;
		String state = contextId;
		String codeChallenge = hash(secret);
		String codeChallengeMethod = "S256";
		String responseType = "code";
		String clientSecret = settings.get(applicationSecretKey);
		String tokenEndpoint = settings.get(tokenEndpointKey);
		String scope = system.properties.containsKey("scope") ? system.properties.get("scope") : "openid";

		OpenIdContext ctx = new OpenIdContext(state, codeChallenge, codeChallengeMethod,
				context.getSecurityContext().getContainerUid(), context.getSecurityContext().getSubject(),
				system.identifier, tokenEndpoint, internalRedirectUrl, applicationId, clientSecret, secret,
				AuthKind.OPEN_ID_PKCE);
		OpenIdContextCache.get(context).put(contextId, ctx);

		String url = String.format(
				"%s?response_type=code&scope=%s&client_id=%s&state=%s&redirect_uri=%s&code_challenge=%s&code_challenge_method=S256",
				externalAuthEndPointUrl, URLEncoder.encode(scope), applicationId, state,
				URLEncoder.encode(internalRedirectUrl), codeChallenge);

		return AccessTokenInfo.tokenNotValid(externalAuthEndPointUrl, internalRedirectUrl, applicationId, state,
				codeChallenge, codeChallengeMethod, responseType, url);
	}

	public JsonObject getAccessTokenByCode(String code, OpenIdContext openIdContext) throws OpenIdException {
		Map<String, String> params = new HashMap<>();
		params.put("grant_type", "authorization_code");
		params.put("redirect_uri", openIdContext.internalRedirectUrl);
		params.put("client_id", openIdContext.applicationId);
		params.put("client_secret", openIdContext.clientSecret);
		params.put("code_verifier", openIdContext.codeVerifier);
		params.put("code", code);

		return postCall(openIdContext.tokenEndpoint, params);
	}

	private String generateCodeVerifier() {
		SecureRandom sr = new SecureRandom();
		byte[] code = new byte[32];
		sr.nextBytes(code);
		String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(code);
		return secret;
	}

	private String hash(String secret) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
		}
		byte[] digest = md.digest(secret.getBytes());
		return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(digest);
	}

}
