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

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwk.GuavaCachedJwkProvider;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Strings;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Shared;

public class AccessTokenValidator {

	private static final Logger logger = LoggerFactory.getLogger(AccessTokenValidator.class);

	private static Optional<GuavaCachedJwkProvider> provider = Optional.empty();

	private AccessTokenValidator() {

	}

	public static void validate(String domainUid, DecodedJWT token) throws ServerFault {
		Map<String, String> domainSettings = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);

		String issuer = token.getIssuer();

		String accessTokenIssuer = domainSettings.get("openid_issuer");
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

	public static void validateSignature(String domainUid, DecodedJWT token) throws ServerFault {
		Map<String, String> domainSettings = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);

		try {

			if (provider.isEmpty()) {
				provider = Optional.of(
						new GuavaCachedJwkProvider(new UrlJwkProvider(new URL(domainSettings.get("openid_jwks_uri")))));
			}

			Jwk jwk = provider.get().get(token.getKeyId());
			Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
			algorithm.verify(token);
		} catch (Exception e) {
			throw new ServerFault(e.getMessage());
		}

	}

	public static Optional<JsonObject> refreshToken(String domainUid, String refreshToken) {
		Map<String, String> domainSettings = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);

		try {
			String endpoint = domainSettings.get("openid_token_endpoint");
			Builder requestBuilder = HttpRequest.newBuilder(new URI(endpoint));
			requestBuilder.header("Charset", StandardCharsets.UTF_8.name());
			requestBuilder.header("Content-Type", "application/x-www-form-urlencoded");
			String params = "grant_type=refresh_token";
			params += "&client_id=" + domainSettings.get("openid_client_id");
			params += "&client_secret=" + domainSettings.get("openid_client_secret");
			params += "&refresh_token=" + refreshToken;
			byte[] postData = params.getBytes(StandardCharsets.UTF_8);
			requestBuilder.method("POST", HttpRequest.BodyPublishers.ofByteArray(postData));
			HttpRequest req = requestBuilder.build();
			HttpClient cli = HttpClient.newHttpClient();
			HttpResponse<String> resp = cli.send(req, BodyHandlers.ofString());

			if (resp.statusCode() >= 400) {
				logger.error("Failed to refresh token {}", resp.body());
				return Optional.empty();
			}

			JsonObject token = new JsonObject(resp.body());
			return Optional.of(token);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return Optional.empty();
	}

	public static void invalidateCache() {
		provider = Optional.empty();
	}

}
