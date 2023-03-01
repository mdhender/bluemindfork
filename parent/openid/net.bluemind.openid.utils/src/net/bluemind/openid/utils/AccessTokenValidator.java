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
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwk.GuavaCachedJwkProvider;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Strings;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.openid.api.OpenIdProperties;

public class AccessTokenValidator {

	private static final Logger logger = LoggerFactory.getLogger(AccessTokenValidator.class);

	private static Optional<GuavaCachedJwkProvider> provider = Optional.empty();

	private AccessTokenValidator() {

	}

	public static void validate(String domainUid, DecodedJWT token) throws ServerFault {
		Map<String, String> domainProperties = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);

		String issuer = token.getIssuer();

		String accessTokenIssuer = domainProperties.get(OpenIdProperties.OPENID_ISSUER.name());
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
		Map<String, String> domainProperties = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);

		try {

			if (provider.isEmpty()) {
				provider = Optional.of(new GuavaCachedJwkProvider(
						new UrlJwkProvider(new URL(domainProperties.get(OpenIdProperties.OPENID_JWKS_URI.name())))));
			}

			Jwk jwk = provider.get().get(token.getKeyId());
			Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
			algorithm.verify(token);
		} catch (Exception e) {
			throw new ServerFault(e.getMessage());
		}

	}

	public static CompletableFuture<Optional<JsonObject>> refreshToken(String domainUid, String refreshToken) {
		Map<String, String> domainProperties = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);

		CompletableFuture<Optional<JsonObject>> future = new CompletableFuture<>();

		try {
			String endpoint = domainProperties.get(OpenIdProperties.OPENID_TOKEN_ENDPOINT.name());

			URI uri = new URI(endpoint);
			HttpClient client = initHttpClient(uri);

			client.request(HttpMethod.POST, uri.getPath(), reqHandler -> {
				if (reqHandler.succeeded()) {
					HttpClientRequest r = reqHandler.result();
					r.response(respHandler -> {
						if (respHandler.succeeded()) {
							HttpClientResponse resp = respHandler.result();
							resp.body(body -> {
								JsonObject token = new JsonObject(new String(body.result().getBytes()));
								future.complete(Optional.of(token));
							});
						} else {
							future.complete(Optional.empty());
							logger.error(reqHandler.cause().getMessage(), reqHandler.cause());
						}
					});

					MultiMap headers = r.headers();
					headers.add(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
					headers.add(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
					String params = "grant_type=refresh_token";
					params += "&client_id=" + domainProperties.get(OpenIdProperties.OPENID_CLIENT_ID.name());
					params += "&client_secret=" + domainProperties.get(OpenIdProperties.OPENID_CLIENT_SECRET.name());
					params += "&refresh_token=" + refreshToken;
					byte[] postData = params.getBytes(StandardCharsets.UTF_8);
					headers.add(HttpHeaders.CONTENT_LENGTH, Integer.toString(postData.length));
					r.write(Buffer.buffer(postData));
					r.end();
				} else {
					future.complete(Optional.empty());
					logger.error(reqHandler.cause().getMessage(), reqHandler.cause());
				}
			});

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return future;
	}

	public static void invalidateCache() {
		provider = Optional.empty();
	}

	private static HttpClient initHttpClient(URI uri) {
		HttpClientOptions opts = new HttpClientOptions();
		opts.setDefaultHost(uri.getHost());
		opts.setSsl(uri.getScheme().equalsIgnoreCase("https"));
		opts.setDefaultPort(
				uri.getPort() != -1 ? uri.getPort() : (uri.getScheme().equalsIgnoreCase("https") ? 443 : 80));
		if (opts.isSsl()) {
			opts.setTrustAll(true);
			opts.setVerifyHost(false);
		}
		return VertxPlatform.getVertx().createHttpClient(opts);
	}

}
