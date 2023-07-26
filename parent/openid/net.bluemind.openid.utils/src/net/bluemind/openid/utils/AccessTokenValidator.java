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
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwk.GuavaCachedJwkProvider;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Strings;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Shared;

public class AccessTokenValidator {

	private static final Logger logger = LoggerFactory.getLogger(AccessTokenValidator.class);

	private static Map<String, GuavaCachedJwkProvider> provider = new HashMap<>();

	private static HttpClient cli = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
			.connectTimeout(Duration.ofSeconds(5)).build();

	private AccessTokenValidator() {

	}

	public static void validate(String domainUid, DecodedJWT token) throws ServerFault {
		Map<String, String> domainProperties = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);

		String issuer = token.getIssuer();
		Claim email = token.getClaim("email");

		if (email.isMissing() || email.isNull()) {
			throw new ServerFault("Failed to validate token: invalid email");
		}

		String accessTokenIssuer = domainProperties.get(AuthDomainProperties.OPENID_ISSUER.name());
		if (Strings.isNullOrEmpty(issuer) || !issuer.equals(accessTokenIssuer)) {
			throw new ServerFault("[" + email.asString() + "] Failed to validate token: invalid Issuer");
		}

		long now = new Date().getTime();
		Long iat = token.getIssuedAt().getTime();
		if (now < iat) {
			throw new ServerFault("[" + email.asString() + "] Failed to validate token: invalid Issued At");
		}

		Long exp = token.getExpiresAt().getTime();
		if (now > exp) {
			throw new ServerFault("[" + email.asString() + "] Failed to validate token: Expired");
		}

	}

	public static void validateSignature(String domainUid, DecodedJWT token) throws ServerFault {
		Map<String, String> domainProperties = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);

		try {

			if (!provider.containsKey(domainUid)) {
				provider.put(domainUid, new GuavaCachedJwkProvider(new UrlJwkProvider(
						new URL(domainProperties.get(AuthDomainProperties.OPENID_JWKS_URI.name())))));
			}

			Jwk jwk = provider.get(domainUid).get(token.getKeyId());
			Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
			algorithm.verify(token);
		} catch (Exception e) {
			throw new ServerFault(e.getMessage());
		}

	}

	public static CompletableFuture<Optional<JsonObject>> refreshToken(String domainUid, String refreshToken) {
		Map<String, String> domainProperties = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);

		String endpoint = domainProperties.get(AuthDomainProperties.OPENID_TOKEN_ENDPOINT.name());

		Builder requestBuilder = HttpRequest.newBuilder(URI.create(endpoint));
		requestBuilder.header("Charset", StandardCharsets.UTF_8.name());
		requestBuilder.header("Content-Type", "application/x-www-form-urlencoded");
		String params = "grant_type=refresh_token";
		params += "&client_id=" + domainProperties.get(AuthDomainProperties.OPENID_CLIENT_ID.name());
		params += "&client_secret=" + domainProperties.get(AuthDomainProperties.OPENID_CLIENT_SECRET.name());
		params += "&refresh_token=" + refreshToken;
		byte[] postData = params.getBytes(StandardCharsets.UTF_8);
		requestBuilder.method("POST", HttpRequest.BodyPublishers.ofByteArray(postData));
		HttpRequest req = requestBuilder.build();

		return cli.sendAsync(req, BodyHandlers.ofString()).thenApply(resp -> {

			if (resp.statusCode() >= 400) {
				logger.error("Failed to refresh token {}", resp.body());
				return Optional.empty();
			}

			JsonObject token = new JsonObject(resp.body());
			return Optional.of(token);
		});
	}

	public static void invalidateCache() {
		provider.clear();
	}

}
