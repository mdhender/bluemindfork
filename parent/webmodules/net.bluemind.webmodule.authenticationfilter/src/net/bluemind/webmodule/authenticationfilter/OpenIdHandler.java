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
package net.bluemind.webmodule.authenticationfilter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Strings;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import net.bluemind.common.cache.persistence.CacheBackingStore;
import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.auth.AuthTypes;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.keycloak.utils.endpoints.KeycloakEndpoints;
import net.bluemind.webmodule.authenticationfilter.internal.AuthenticationCookie;
import net.bluemind.webmodule.authenticationfilter.internal.CodeVerifierCache;
import net.bluemind.webmodule.authenticationfilter.internal.ExternalCreds;
import net.bluemind.webmodule.authenticationfilter.internal.SessionData;
import net.bluemind.webmodule.authenticationfilter.internal.SessionsCache;

public class OpenIdHandler extends AbstractAuthHandler implements Handler<HttpServerRequest> {
	private static final Logger logger = LoggerFactory.getLogger(OpenIdHandler.class);

	private static final String JWT_SESSION_STATE = "session_state";

	private class SessionConsumer implements Consumer<SessionData> {
		private final Vertx vertx;
		private final HttpServerRequest request;
		private final String realm;
		private final JsonObject jwtToken;
		private final String openIdClientSecret;

		public SessionConsumer(Vertx vertx, HttpServerRequest request, String realm, String openIdClientSecret,
				JsonObject jwtToken) {
			this.vertx = vertx;
			this.request = request;
			this.realm = realm;
			this.openIdClientSecret = openIdClientSecret;
			this.jwtToken = jwtToken;
		}

		@Override
		public void accept(SessionData sessionData) {
			decorateResponse(sessionData);

			long renewTimerId = new OpenIdRefreshHandler(vertx, httpClient, sessionData.authKey).setRefreshTimer();

			CacheBackingStore<SessionData> cache = SessionsCache.get();
			synchronized (cache) {
				cache.put(sessionData.authKey,
						sessionData.setOpenId(jwtToken, realm, openIdClientSecret, renewTimerId));
			}

			if (logger.isInfoEnabled()) {
				logger.info("[{}] Session {} for user {} created, JWT SID: {}", request.path(), sessionData.authKey,
						sessionData.loginAtDomain, jwtToken.getValue(JWT_SESSION_STATE));
			}
		}

		public void decorateResponse(SessionData sessionData) {
			MultiMap headers = request.response().headers();

			JsonObject cookie = new JsonObject();
			cookie.put("sid", sessionData.authKey);
			cookie.put("domain_uid", realm);
			AuthenticationCookie.add(headers, AuthenticationCookie.OPENID_SESSION, cookie.encode());

			Claim pubpriv = JWT.decode(jwtToken.getString("access_token")).getClaim("bm_pubpriv");
			boolean privateComputer = "private".equals(pubpriv.asString());
			AuthenticationCookie.add(headers, AuthenticationCookie.BMPRIVACY, Boolean.toString(privateComputer));
		}
	}

	private static final Decoder b64UrlDecoder = Base64.getUrlDecoder();

	@Override
	public void handle(HttpServerRequest event) {
		if (Strings.isNullOrEmpty(event.params().get("code"))) {
			event.response().end();
			return;
		}

		String code = event.params().get("code");
		String state = event.params().get("state");

		JsonObject jsonState = new JsonObject(new String(b64UrlDecoder.decode(state.getBytes())));

		if (sessionExists(event)) {
			event.response().headers().add(HttpHeaders.LOCATION, getRedirectTo(event, jsonState));
			event.response().setStatusCode(302);
			event.response().end();
			return;
		}

		List<String> forwadedFor = new ArrayList<>(event.headers().getAll("X-Forwarded-For"));
		forwadedFor.add(event.remoteAddress().host());

		String key = jsonState.getString("codeVerifierKey");
		String codeVerifier = CodeVerifierCache.verify(key);
		if (Strings.isNullOrEmpty(codeVerifier)) {
			error(event, new Throwable("OpenId codeVerifier key '" + key + "' not found in cache, ignore request from ["
					+ String.join(",", forwadedFor) + "]. Webserver restart ?"));
			return;
		}

		CodeVerifierCache.invalidate(key);

		String realm = jsonState.getString("domain_uid");
		Map<String, String> domainSettings = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(realm);

		try {
			httpClient.request(new RequestOptions().setMethod(HttpMethod.POST)
					.setAbsoluteURI(tokenEndpoint(realm, domainSettings)), reqHandler -> {
						boolean internalAuth = AuthTypes.INTERNAL.name()
								.equals(domainSettings.get(AuthDomainProperties.AUTH_TYPE.name()));
						String openIdClientSecret = domainSettings
								.get(AuthDomainProperties.OPENID_CLIENT_SECRET.name());

						if (reqHandler.succeeded()) {
							HttpClientRequest r = reqHandler.result();
							r.response(respHandler -> {
								if (respHandler.succeeded()) {
									HttpClientResponse resp = respHandler.result();
									resp.body(body -> {
										JsonObject jwtToken = new JsonObject(new String(body.result().getBytes()));

										Optional<SessionData> existingSession = Optional.empty();
										CacheBackingStore<SessionData> cache = SessionsCache.get();
										synchronized (cache) {
											existingSession = cache.asMap().values().stream()
													.filter(sessionData -> sessionData.jwtToken != null)
													.filter(sessionData -> jwtToken.getValue(JWT_SESSION_STATE)
															.equals(sessionData.jwtToken.getValue(JWT_SESSION_STATE)))
													.findAny();
										}

										existingSession.ifPresentOrElse(sessionData -> {
											logger.info(
													"BlueMind session {} already exists for JWT session_state {}, don't create a new one, redirect to {}",
													sessionData.authKey, jwtToken.getValue(JWT_SESSION_STATE),
													getRedirectTo(event, jsonState));
											new SessionConsumer(vertx, event, realm, openIdClientSecret, jwtToken)
													.decorateResponse(sessionData);
											event.response().headers().add(HttpHeaders.LOCATION,
													getRedirectTo(event, jsonState));
											event.response().setStatusCode(302);
											event.response().end();
										}, () -> validateToken(event, forwadedFor, jsonState, jwtToken,
												new SessionConsumer(vertx, event, realm, openIdClientSecret,
														jwtToken)));
									});
								} else {
									error(event, respHandler.cause());
								}
							});

							MultiMap headers = r.headers();
							headers.add(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
							headers.add(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
							String params = "grant_type=authorization_code";

							if (internalAuth) {
								params += "&client_id=" + IKeycloakUids.clientId(realm);
							} else {
								params += "&client_id="
										+ encode(domainSettings.get(AuthDomainProperties.OPENID_CLIENT_ID.name()));
							}
							params += "&client_secret=" + encode(openIdClientSecret);
							params += "&code=" + encode(code);
							params += "&code_verifier=" + encode(codeVerifier);
							params += "&redirect_uri=" + encode("https://" + event.authority().host() + "/auth/openid");
							params += "&scope=openid";

							byte[] postData = params.getBytes(StandardCharsets.UTF_8);
							headers.add(HttpHeaders.CONTENT_LENGTH, Integer.toString(postData.length));
							r.write(Buffer.buffer(postData));
							r.end();
						} else {
							error(event, reqHandler.cause());
						}
					});

			return;
		} catch (Exception e) {
			error(event, e);
		}

		event.response().end();
	}

	private String getRedirectTo(HttpServerRequest request, JsonObject jsonState) {
		String redirectTo = jsonState.getString("path");

		if (redirectTo == null) {
			redirectTo = "/";
		}

		if (logger.isDebugEnabled()) {
			logger.debug("[{}] Redirect to {}", request.path(), redirectTo);
		}
		return redirectTo;
	}

	private boolean sessionExists(HttpServerRequest request) {
		String sessionId = request.getHeader("BMSessionId");
		if (sessionId == null) {
			return false;
		}

		CacheBackingStore<SessionData> cache = SessionsCache.get();
		synchronized (cache) {
			return cache.getIfPresent(sessionId) != null;
		}
	}

	private String tokenEndpoint(String domainUid, Map<String, String> domainSettings) {
		String endpoint;
		if (AuthTypes.INTERNAL.name().equals(domainSettings.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			endpoint = KeycloakEndpoints.tokenEndpoint(domainUid);
		} else {
			endpoint = domainSettings.get(AuthDomainProperties.OPENID_TOKEN_ENDPOINT.name());
		}
		return endpoint;
	}

	private String encode(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	private void validateToken(HttpServerRequest request, List<String> forwadedFor, JsonObject jsonState,
			JsonObject jwtToken, Consumer<SessionData> handlerSessionConsumer) {
		DecodedJWT accessToken = null;
		try {
			accessToken = JWT.decode(jwtToken.getString("access_token"));
		} catch (JWTDecodeException t) {
			logger.error("Unexpected token endpoint response : {}", jwtToken);
			throw t;
		}

		Claim email = accessToken.getClaim("email");
		if (email.isMissing() || email.isNull()) {
			error(request, new Throwable("Failed to validate id_token: no email"));
			return;
		}

		AuthProvider prov = new AuthProvider(vertx);

		ExternalCreds creds = new ExternalCreds();
		creds.setLoginAtDomain(email.asString());
		createSession(request, prov, forwadedFor, creds, getRedirectTo(request, jsonState), handlerSessionConsumer);
	}
}
