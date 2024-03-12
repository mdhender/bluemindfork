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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Strings;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.common.cache.persistence.CacheBackingStore;
import net.bluemind.core.api.BMVersion;
import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.auth.AuthTypes;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.keycloak.utils.endpoints.KeycloakEndpoints;
import net.bluemind.network.topology.Topology;
import net.bluemind.webmodule.authenticationfilter.internal.AuthenticationCookie;
import net.bluemind.webmodule.authenticationfilter.internal.CodeVerifierCache;
import net.bluemind.webmodule.authenticationfilter.internal.DomainsHelper;
import net.bluemind.webmodule.authenticationfilter.internal.SessionData;
import net.bluemind.webmodule.authenticationfilter.internal.SessionsCache;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.server.SecurityConfig;
import net.bluemind.webmodule.server.WebserverConfiguration;
import net.bluemind.webmodule.server.forward.ForwardedLocation;

public class AuthenticationFilter implements IWebFilter, NeedVertx {
	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

	private static final HashFunction sha256 = Hashing.sha256();
	private static final Encoder b64UrlEncoder = Base64.getUrlEncoder().withoutPadding();
	private static final ServerCookieDecoder cookieDecoder = ServerCookieDecoder.LAX;

	private static final String REDIRECT_PROTO = "https://";

	@SuppressWarnings("serial")
	private static class InvalidIdToken extends Exception {
		public final String sessionId;

		public InvalidIdToken() {
			this.sessionId = null;
		}

		public InvalidIdToken(String sessionId) {
			this.sessionId = sessionId;
		}
	}

	@SuppressWarnings("serial")
	private static class JWTInvalidSid extends RuntimeException {
	}

	private Vertx vertx;
	private HttpClient httpClient;

	public AuthenticationFilter() {
		logger.info("AuthenticationFilter filter created.");
	}

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
		this.httpClient = vertx.createHttpClient();

		// Every day, remove sessions files from disk that are not in cache
		vertx.setPeriodic(TimeUnit.DAYS.toMillis(1), i -> SessionsCache.get().cleanUp());

		// TODO: Must logout KC too
		vertx.eventBus().consumer(Topic.CORE_SESSIONS, (Message<JsonObject> event) -> {
			JsonObject cm = event.body();
			String op = cm.getString("operation");
			if ("logout".equals(op)) {
				String sid = cm.getString("sid");

				SessionData sessionData = SessionsCache.get().getIfPresent(sid);
				if (sessionData != null && sessionData.refreshTimerId > 0) {
					logger.info("Remove refresh timer {} for {}", sessionData.refreshTimerId, sid);
					vertx.cancelTimer(sessionData.refreshTimerId);
				}

				SessionsCache.get().invalidate(sid);
			}
		});
	}

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request, WebserverConfiguration conf) {
		Optional<ForwardedLocation> forwardedLocation = conf.getForwardedLocations().stream()
				.filter(fl -> request.path().startsWith(fl.getPathPrefix())).findFirst();

		if (!needAuthentication(request, forwardedLocation)) {
			if (logger.isDebugEnabled()) {
				logger.debug("[{}] No auth needed", request.path());
			}
			return sessionExists(request).orElse(CompletableFuture.completedFuture(request));
		}

		if (request.path().endsWith("/bluemind_sso_logout")) {
			return logout(request);
		}

		if (request.path().endsWith("/bluemind_sso_logout/backchannel")) {
			return backChannelLogout(request);
		}

		return forwardedLocation.flatMap(fl -> fl.resolve()).map(resolved -> forwardedLocation(request))
				.orElseGet(() -> notForwardedLocation(request));
	}

	private CompletableFuture<HttpServerRequest> notForwardedLocation(HttpServerRequest request) {
		return sessionExists(request).orElseGet(() -> {
			String domainUid = DomainsHelper.getDomainUid(request);

			if (isCasEnabled(domainUid)) {
				try {
					CasHandler.CASRequest.build(request, domainUid).redirectToCasLogin();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					request.response().setStatusCode(500).end();
				}
			} else {
				redirectToOpenIdServer(request, domainUid);
			}
			return CompletableFuture.completedFuture(null);
		});
	}

	private CompletableFuture<HttpServerRequest> forwardedLocation(HttpServerRequest request) {
		return sessionExists(request).orElseGet(() -> {
			request.response().setStatusCode(302);
			request.response().headers().add(HttpHeaders.LOCATION, "/bluemind_sso_logout");
			request.response().end();
			return CompletableFuture.completedFuture((HttpServerRequest) null);
		});
	}

	private Optional<CompletableFuture<HttpServerRequest>> sessionExists(HttpServerRequest request) {
		CacheBackingStore<SessionData> cache = SessionsCache.get();

		synchronized (cache) {
			return sessionId(request).map(cache::getIfPresent).map(sessionData -> {
				if (logger.isDebugEnabled()) {
					logger.debug("[{}] Session data for session ID {} found in cache", request.path(),
							sessionData.authKey);
				}

				decorate(request, sessionData);
				if (sessionData.refreshTimerId < 0 && sessionData.jwtToken != null && sessionData.realm != null
						&& sessionData.openIdClientSecret != null) {
					long timerId = new OpenIdRefreshHandler(vertx, httpClient, sessionData.authKey).setRefreshTimer();
					cache.put(sessionData.authKey, sessionData.setOpenId(sessionData.jwtToken, sessionData.realm,
							sessionData.openIdClientSecret, timerId));

				}
				return CompletableFuture.completedFuture(request);
			});
		}
	}

	private void redirectToOpenIdServer(HttpServerRequest request, String domainUid) {
		Map<String, String> domainProperties = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);

		String key = UUID.randomUUID().toString();
		String path = getPath(request);

		JsonObject jsonState = new JsonObject();
		jsonState.put("codeVerifierKey", key);
		jsonState.put("path", path);
		jsonState.put("domain_uid", domainUid);

		String state = b64UrlEncoder.encodeToString(jsonState.encode().getBytes());

		String codeVerifier = createCodeVerifier();
		CodeVerifierCache.put(key, codeVerifier);

		String codeChallenge = b64UrlEncoder
				.encodeToString(sha256.hashString(codeVerifier, StandardCharsets.UTF_8).asBytes());

		try {
			String location = "";

			if (AuthTypes.INTERNAL.name().equals(domainProperties.get(AuthDomainProperties.AUTH_TYPE.name()))) {
				location = KeycloakEndpoints.authorizationEndpoint(domainUid);
				location += "?client_id=" + IKeycloakUids.clientId(domainUid);
			} else {
				location = domainProperties.get(AuthDomainProperties.OPENID_AUTHORISATION_ENDPOINT.name());
				location += "?client_id=" + encode(domainProperties.get(AuthDomainProperties.OPENID_CLIENT_ID.name()));
			}
			location += "&redirect_uri=" + encode(REDIRECT_PROTO + request.authority().host() + "/auth/openid");
			location += "&code_challenge=" + encode(codeChallenge);
			location += "&state=" + encode(state);
			location += "&code_challenge_method=S256";
			location += "&response_type=code";
			location += "&scope=openid";

			// BM-19877 hack
			// used for "Back to Application" redirect
			Cookie redirect = new DefaultCookie(AuthenticationCookie.BMREDIRECT, path);
			redirect.setPath("/");
			redirect.setHttpOnly(false);
			if (SecurityConfig.secureCookies) {
				redirect.setSecure(true);
			}
			request.response().headers().add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(redirect));

			request.response().headers().add(HttpHeaders.LOCATION, location);
			request.response().setStatusCode(302);
		} catch (NullPointerException n) {
			logger.error("Unable to get OPENID location", n);
			request.response().setStatusCode(500);
		}

		request.response().end();
	}

	private String getPath(HttpServerRequest request) {
		String path = Optional.ofNullable(request.path()).orElse("/");

		// visio hack otherwise it will redirect to /visio//
		if ("/visio/".equals(path)) {
			return request.headers().get(HttpHeaders.REFERER);
		}

		String askedUri = request.params().get("askedUri");
		if (!Strings.isNullOrEmpty(askedUri)) {
			path = askedUri;
		} else {
			path += request.query() != null ? "?" + request.query() : "";
		}
		return path;
	}

	private boolean isCasEnabled(String domainUid) {
		Map<String, String> domainProperties = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);
		String authType = domainProperties.get(AuthDomainProperties.AUTH_TYPE.name());
		return AuthTypes.CAS.name().equals(authType);
	}

	private boolean needAuthentication(HttpServerRequest request, Optional<ForwardedLocation> forwardedLocation) {
		if (forwardedLocation.isPresent()) {
			ForwardedLocation fl = forwardedLocation.get();
			if (fl.isWhitelisted(request.uri())) {
				return false;
			}
			if (!fl.needAuth()) {
				return false;
			}
		}
		return true;
	}

	private Optional<String> sessionId(HttpServerRequest request) {
		String cookieStr = Optional.ofNullable(request.headers().get("cookie")).orElse("");
		Set<Cookie> cookies = cookieDecoder.decode(cookieStr);
		Optional<Cookie> oidc = cookies.stream().filter(c -> AuthenticationCookie.OPENID_SESSION.equals(c.name()))
				.findFirst();
		if (oidc.isPresent()) {
			JsonObject token = new JsonObject(oidc.get().value());

			if (logger.isDebugEnabled()) {
				logger.debug("[{}] Session ID {} found in {} cookie", request.path(), token.getString("sid"),
						AuthenticationCookie.OPENID_SESSION);
			}

			return Optional.of(token.getString("sid"));
		}

		Optional<Cookie> bmSid = cookies.stream().filter(c -> AuthenticationCookie.BMSID.equals(c.name())).findFirst();
		if (bmSid.isPresent()) {
			if (logger.isDebugEnabled()) {
				logger.debug("[{}] Session ID {} found in {} cookie", request.path(), bmSid.get().value(),
						AuthenticationCookie.BMSID);
			}

			return Optional.of(bmSid.get().value());
		}

		if (logger.isDebugEnabled()) {
			logger.debug("[{}] No session ID found", request.path());
		}
		return Optional.empty();
	}

	private CompletableFuture<HttpServerRequest> logout(HttpServerRequest request) {
		AuthenticationCookie.purge(request);

		String domainUid = DomainsHelper.getDomainUid(request);
		Map<String, String> domainSettings = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);

		if (!AuthTypes.INTERNAL.name().equals(domainSettings.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			// Redirect to external authentication server logout URL before logout BM
			// session
			sessionId(request).ifPresent(sessionId -> vertx.setTimer(TimeUnit.SECONDS.toMillis(10),
					i -> new AuthProvider(vertx).logout(sessionId)));
		}

		String logoutUrl = null;
		if (!AuthTypes.CAS.name().equals(domainSettings.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			try {
				if (AuthTypes.INTERNAL.name().equals(domainSettings.get(AuthDomainProperties.AUTH_TYPE.name()))) {
					logoutUrl = KeycloakEndpoints.endSessionEndpoint(domainUid);
				} else {
					logoutUrl = domainSettings.get(AuthDomainProperties.OPENID_END_SESSION_ENDPOINT.name());
				}

				String sessionId = sessionId(request).orElseThrow(InvalidIdToken::new);

				logoutUrl += getRedirectUrl(request).map(url -> "?post_logout_redirect_uri=" + url).orElse("");

				CacheBackingStore<SessionData> cache = SessionsCache.get();
				synchronized (cache) {
					logoutUrl += Optional.ofNullable(cache.getIfPresent(sessionId))
							.map(sessionData -> sessionData.jwtToken).map(jwtToken -> jwtToken.getString("id_token"))
							.map(idToken -> "&id_token_hint=" + idToken)
							.orElseThrow(() -> new InvalidIdToken(sessionId));
				}
			} catch (InvalidIdToken iIT) {
				if (iIT.sessionId != null) {
					new AuthProvider(vertx).logout(iIT.sessionId);
				}

				logoutUrl = "/";
			}
		} else {
			logoutUrl = domainSettings.get(AuthDomainProperties.CAS_URL.name()) + "logout";
			logoutUrl += getRedirectUrl(request).map(url -> "?url=" + url).orElse("");
		}

		request.response().headers().add(HttpHeaders.LOCATION, logoutUrl);
		request.response().setStatusCode(302);
		request.response().end();

		return CompletableFuture.completedFuture(null);
	}

	/**
	 * https://openid.net/specs/openid-connect-backchannel-1_0.html
	 * 
	 * Errors: https://www.rfc-editor.org/rfc/inline-errata/rfc6749.html
	 * 
	 * @param request
	 * @return
	 */
	private CompletableFuture<HttpServerRequest> backChannelLogout(HttpServerRequest request) {
		if (!isValidRequest(request)) {
			logger.info("Backchannel logout: invalid session");
			return CompletableFuture.completedFuture(null);
		}

		request.setExpectMultipart(true).endHandler(v1 -> {
			DecodedJWT jwtLogoutToken = JWT.decode(request.getFormAttribute("logout_token"));

			Claim jwtSid = jwtLogoutToken.getClaim("sid");
			if (jwtSid.isMissing() || jwtSid.isNull()) {
				throw new JWTInvalidSid();
			}

			Optional<SessionData> existingSession = Optional.empty();
			CacheBackingStore<SessionData> cache = SessionsCache.get();
			synchronized (cache) {
				existingSession = cache.asMap().values().stream().filter(sessionData -> sessionData.jwtToken != null)
						.filter(sessionData -> jwtSid.asString().equals(sessionData.jwtToken.getValue("session_state")))
						.findAny();
			}

			existingSession.ifPresentOrElse(sessionData -> new AuthProvider(vertx).logout(sessionData),
					() -> logger.warn("Backchannel logout: session not found for JWTSid {}", jwtSid.asString()));

			backChannelLogoutSuccess(request);
		}).exceptionHandler(e -> {
			logger.error("JWT logout token process error from {}: {}", request.headers().getAll("X-Forwarded-For"),
					e.getMessage(), e);
			backChannelLogoutError(request, "JWT logout token process error: " + e.getMessage());
		});

		return CompletableFuture.completedFuture(null);
	}

	public void backChannelLogoutSuccess(HttpServerRequest request) {
		logger.debug("Bachchannel logout successfully");
		backChannelResponse(request, 200, new JsonObject().put("status", "ok").toBuffer());
	}

	public void backChannelLogoutError(HttpServerRequest request, String errorDescription) {
		backChannelResponse(request, 400,
				new JsonObject().put("error", "invalid_request").put("error_description", errorDescription).toBuffer());
	}

	public void backChannelResponse(HttpServerRequest request, int httpStatusCode, Buffer body) {
		request.response().putHeader(HttpHeaders.CACHE_CONTROL, "no-store").setStatusCode(httpStatusCode).end(body);
	}

	private boolean isValidRequest(HttpServerRequest request) {
		if (request.method() != HttpMethod.POST) {
			backChannelLogoutError(request, "Request must use POST method");
			return false;
		}

		if (!request.headers().get(HttpHeaders.CONTENT_TYPE)
				.contentEquals(HttpHeaders.APPLICATION_X_WWW_FORM_URLENCODED.toString())) {
			backChannelLogoutError(request, "Invalid request content");
			return false;
		}

		String clAsString = request.headers().get(HttpHeaders.CONTENT_LENGTH);
		Long contentLength = null;
		try {
			contentLength = Long.parseLong(clAsString);
		} catch (NumberFormatException e) {
			// Do nothing
		}

		if (contentLength == null || contentLength > 1000 * 100) {
			logger.info("TOO BIG: {}", contentLength);
			backChannelLogoutError(request, "Too big");
			return false;
		}

		return true;
	}

	private Optional<String> getRedirectUrl(HttpServerRequest request) {
		try {
			URL reqUrl = URI.create(request.absoluteURI()).toURL();
			return Optional.ofNullable(URLEncoder.encode(REDIRECT_PROTO + reqUrl.getHost() + "/",
					java.nio.charset.StandardCharsets.UTF_8.toString()));
		} catch (MalformedURLException | UnsupportedEncodingException e) {
			logger.warn("Unable to get logout redirect URL", e);
			return Optional.empty();
		}
	}

	private String createCodeVerifier() {
		SecureRandom sr = new SecureRandom();
		byte[] code = new byte[32];
		sr.nextBytes(code);
		return b64UrlEncoder.encodeToString(code);
	}

	private void decorate(HttpServerRequest request, SessionData sd) {
		MultiMap headers = request.headers();

		headers.add("BMSessionId", sd.authKey);
		headers.add("BMUserId", sd.userUid);
		headers.add("BMUserLogin", sd.login);
		headers.add("BMAccountType", sd.accountType);
		headers.add("BMUserLATD", sd.loginAtDomain);
		if (sd.defaultEmail != null) {
			headers.add("BMUserDefaultEmail", sd.defaultEmail);
		}

		// Used to order the client to reset it's local data if guid specified
		// in sessionsInfos is different from the one in the local application data
		headers.add("BMMailboxCopyGuid", sd.mailboxCopyGuid);

		headers.add("BMUserDomainId", sd.domainUid);

		addIfPresent(headers, sd.givenNames, "BMUserFirstName");
		addIfPresent(headers, sd.familyNames, "BMUserLastName");
		addIfPresent(headers, sd.formatedName, sd.login, "BMUserFormatedName");

		headers.add("BMRoles", sd.rolesAsString);

		headers.add("BMUserMailPerms", "true");

		// needed by rouncube ?

		headers.add("bmMailPerms", "true");
		Map<String, String> settings = sd.settings;
		String lang = settings.get("lang");
		headers.add("BMLang", lang == null ? "en" : lang);
		String defaultApp = settings.get("default_app");
		if (sd.loginAtDomain.equals("admin0@global.virt")) {
			defaultApp = "/adminconsole/";
		}
		headers.add("BMDefaultApp", defaultApp != null ? defaultApp : "/webapp/mail/");
		headers.add("BMPrivateComputer", "" + sd.privateComputer);

		headers.add("BMHasIM", "true");
		headers.add("BMVersion", BMVersion.getVersion());
		headers.add("BMBrandVersion", BMVersion.getVersionName());

		if (sd.dataLocation != null) {
			headers.add("BMDataLocation", sd.dataLocation);
			headers.add("BMPartition", CyrusPartition.forServerAndDomain(sd.dataLocation, sd.domainUid).name);
			Topology.getIfAvailable().ifPresent(topo -> {
				// prevent roundcube from trying locator calls
				headers.add("bmTopoCore", topo.any("bm/core").value.address());
				headers.add("bmTopoEs", topo.any("bm/es").value.address());
				headers.add("bmTopoImap", topo.datalocation(sd.dataLocation).value.address());
				topo.anyIfPresent("cti/frontend").ifPresent(cti -> headers.add("bmTopoCti", cti.value.address()));
			});
		}
	}

	private void addIfPresent(MultiMap headers, String value, String fallback, String headerKey) {
		if (!addIfPresent(headers, value, headerKey)) {
			addIfPresent(headers, fallback, headerKey);
		}
	}

	private boolean addIfPresent(MultiMap headers, String value, String headerKey) {
		if (value != null) {
			headers.add(headerKey, java.util.Base64.getEncoder().encodeToString(value.getBytes()));
			return true;
		} else {
			return false;
		}
	}

	private String encode(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

}
