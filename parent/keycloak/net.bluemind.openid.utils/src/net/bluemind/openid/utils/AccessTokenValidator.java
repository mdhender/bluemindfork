/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.openid.utils;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Optional;

import com.auth0.jwk.GuavaCachedJwkProvider;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Strings;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;

public class AccessTokenValidator {

	private static Optional<GuavaCachedJwkProvider> provider = Optional.empty();

	private AccessTokenValidator() {

	}

	public static void validate(JsonObject openIdConfiguration, DecodedJWT token) throws ServerFault {
		String issuer = token.getIssuer();
		String accessTokenIssuer = Strings.isNullOrEmpty(openIdConfiguration.getString("access_token_issuer"))
				? openIdConfiguration.getString("issuer")
				: openIdConfiguration.getString("access_token_issuer");
		if (Strings.isNullOrEmpty(issuer) || !issuer.equals(accessTokenIssuer)) {
			throw new ServerFault("Failed to validate token: iss");
		}

		long now = new Date().getTime();
		Long iat = token.getIssuedAt().getTime();
		if (now < iat) {
			throw new ServerFault("Failed to validate token: iat");
		}

		Long exp = token.getExpiresAt().getTime();
		if (now > exp) {
			throw new ServerFault("Failed to validate token: exp");
		}

	}

	public static void validateSignature(JsonObject openIdConfiguration, DecodedJWT token) throws ServerFault {
		try {

			if (provider.isEmpty()) {
				provider = Optional.of(new GuavaCachedJwkProvider(
						new UrlJwkProvider(new URL(openIdConfiguration.getString("jwks_uri")))));
			}

			Jwk jwk = provider.get().get(token.getKeyId());
			Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
			algorithm.verify(token);
		} catch (Exception e) {
			throw new ServerFault(e.getMessage());
		}

	}

	public static void invalidateCache() {
		provider = Optional.empty();
	}

}
