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
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.core.api.BMVersion;
import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.auth.AuthTypes;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.network.topology.Topology;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.webmodule.authenticationfilter.internal.SessionData;
import net.bluemind.webmodule.authenticationfilter.internal.SessionsCache;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.SecurityConfig;
import net.bluemind.webmodule.server.WebserverConfiguration;
import net.bluemind.webmodule.server.forward.ForwardedLocation;
import net.bluemind.webmodule.server.forward.ForwardedLocation.ResolvedLoc;

public class AuthenticationFilter implements IWebFilter {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
	private static final HashFunction sha256 = Hashing.sha256();
	private static final Encoder b64UrlEncoder = Base64.getUrlEncoder().withoutPadding();
	private static final Cache<String, String> codeVerifierCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES).build();
	private static final ServerCookieDecoder cookieDecoder = ServerCookieDecoder.LAX;
	private static final String OPENID_COOKIE = "OpenIdSession";
	private static final String ACCESS_COOKIE = "AccessToken";
	private static final String REFRESH_COOKIE = "RefreshToken";
	private static final String ID_COOKIE = "IdToken";
	private static final String BMSID_COOKIE = "BMSID";

	public AuthenticationFilter() {
		logger.info("AuthenticationFilter filter created.");
	}

	public static String verify(String key) {
		return codeVerifierCache.getIfPresent(key);
	}

	public static void put(String key, String value) {
		codeVerifierCache.put(key, value);
	}

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request, WebserverConfiguration conf) {
		Optional<ForwardedLocation> forwardedLocation = conf.getForwardedLocations().stream()
				.filter(fl -> request.path().startsWith(fl.getPathPrefix())).findFirst();

		if (!needAuthentication(request, forwardedLocation)) {
			if (logger.isDebugEnabled()) {
				logger.debug("[{}] No auth needed", request.path());
			}
			return CompletableFuture.completedFuture(request);
		}

		if (forwardedLocation.isPresent()) {
			Optional<ResolvedLoc> resolved = forwardedLocation.get().resolve();
			if (resolved.isPresent()) {
				Optional<SessionData> sessionData = sessionId(request)
						.map(sessionId -> SessionsCache.get().getIfPresent(sessionId));
				if (sessionData.isPresent()) {
					decorate(request, sessionData.get());
				} else {
					request.response().setStatusCode(302);
					request.response().headers().add(HttpHeaders.LOCATION, "/bluemind_sso_logout");
					request.response().end();
					return CompletableFuture.completedFuture(null);
				}

				return CompletableFuture.completedFuture(request);
			}
		}

		if (request.path().endsWith("/bluemind_sso_logout")) {
			return logout(request);
		}

		Optional<SessionData> sessionData = sessionId(request)
				.map(sessionId -> SessionsCache.get().getIfPresent(sessionId));
		if (sessionData.isPresent()) {
			decorate(request, sessionData.get());
			return CompletableFuture.completedFuture(request);
		}

		Optional<String> domainUid = getDomainUid(request);
		if (domainUid.isEmpty()) {
			redirectToGlobalExternalUrl(request);
		}

		if (isCasEnabled(domainUid.get())) {
			redirectToCasServer(request, domainUid.get());
		} else {
			redirectToOpenIdServer(request, domainUid.get());
		}

		return CompletableFuture.completedFuture(null);

	}

	private void redirectToOpenIdServer(HttpServerRequest request, String domainUid) {
		Map<String, String> domainProperties = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);

		String key = UUID.randomUUID().toString();
		String path = Optional.ofNullable(request.path()).orElse("/");
		path += request.query() != null ? "?" + request.query() : "";

		JsonObject jsonState = new JsonObject();
		jsonState.put("codeVerifierKey", key);
		jsonState.put("path", path);
		jsonState.put("domain_uid", domainUid);

		String state = b64UrlEncoder.encodeToString(jsonState.encode().getBytes());

		String codeVerifier = createCodeVerifier();
		AuthenticationFilter.put(key, codeVerifier);

		String codeChallenge = b64UrlEncoder
				.encodeToString(sha256.hashString(codeVerifier, StandardCharsets.UTF_8).asBytes());

		try {
			String location = domainProperties.get(AuthDomainProperties.OPENID_AUTHORISATION_ENDPOINT.name());
			location += "?client_id=" + encode(domainProperties.get(AuthDomainProperties.OPENID_CLIENT_ID.name()));
			location += "&redirect_uri=" + encode("https://" + request.host() + "/auth/openid");
			location += "&code_challenge=" + encode(codeChallenge);
			location += "&state=" + encode(state);
			location += "&code_challenge_method=S256";
			location += "&response_type=code";
			location += "&scope=openid";

			request.response().headers().add(HttpHeaders.LOCATION, location);
			request.response().setStatusCode(302);
		} catch (NullPointerException n) {
			logger.error("Unable to get OPENID location", n);
			request.response().setStatusCode(500);
		}

		request.response().end();
	}

	private void redirectToCasServer(HttpServerRequest request, String domainUid) {
		Map<String, String> domainProperties = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);
		String casURL = domainProperties.get(AuthDomainProperties.CAS_URL.name());
		String location = casURL + "login?service=";
		location += encode("https://" + request.host() + "/auth/cas");
		request.response().headers().add(HttpHeaders.LOCATION, location);
		request.response().setStatusCode(302);
		request.response().end();
	}

	private void redirectToGlobalExternalUrl(HttpServerRequest request) {
		SharedMap<String, String> sysconf = MQ.sharedMap(Shared.MAP_SYSCONF);
		String location = "https://" + sysconf.get(SysConfKeys.external_url.name()) + request.path();
		location += request.query() != null ? "?" + request.query() : "";
		request.response().headers().add(HttpHeaders.LOCATION, location);
		request.response().setStatusCode(302);
		request.response().end();
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
		Optional<Cookie> oidc = cookies.stream().filter(c -> OPENID_COOKIE.equals(c.name())).findFirst();
		if (oidc.isPresent()) {
			JsonObject token = new JsonObject(oidc.get().value());
			return Optional.of(token.getString("sid"));
		}

		Optional<Cookie> bmSid = cookies.stream().filter(c -> BMSID_COOKIE.equals(c.name())).findFirst();
		if (bmSid.isPresent()) {
			return Optional.of(bmSid.get().value());
		}

		return Optional.empty();
	}

	private CompletableFuture<HttpServerRequest> logout(HttpServerRequest request) {
		Optional<String> sessionId = sessionId(request);
		if (sessionId.isPresent()) {
			SessionsCache.get().invalidate(sessionId.get());
		}

		purgeSessionCookie(request.response().headers());

		String domainUid = getDomainUid(request).orElse("global.virt");
		Map<String, String> domainSettings = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid);

		String logoutUrl = null;
		String redirUrl = null;
		try {
			URL reqUrl = new URL(request.absoluteURI());
			redirUrl = "https://" + reqUrl.getHost() + "/";
		} catch (MalformedURLException e) {
		}
		try {
			redirUrl = URLEncoder.encode(redirUrl, java.nio.charset.StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e) {
		}

		if (!AuthTypes.CAS.name().equals(domainSettings.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			logoutUrl = domainSettings.get(AuthDomainProperties.OPENID_END_SESSION_ENDPOINT.name());
			String idToken = null;
			if (request.cookies("IdToken").stream().findFirst().isPresent()) {
				idToken = request.cookies("IdToken").stream().findFirst().get().getValue();
			}
			if (idToken != null) {
				try {
					idToken = URLEncoder.encode(idToken, java.nio.charset.StandardCharsets.UTF_8.toString());
				} catch (UnsupportedEncodingException e) {
				}
				logoutUrl += "?post_logout_redirect_uri=" + redirUrl;
				logoutUrl += "&id_token_hint=" + idToken;
			}
		} else {
			logoutUrl = domainSettings.get(AuthDomainProperties.CAS_URL.name()) + "logout";
			logoutUrl += "?url=" + redirUrl;
		}

		request.response().headers().add(HttpHeaders.LOCATION, logoutUrl);
		request.response().setStatusCode(302);
		request.response().end();

		return CompletableFuture.completedFuture(null);

	}

	public static void purgeSessionCookie(MultiMap headers) {

		Cookie bmSid = new DefaultCookie(BMSID_COOKIE, "delete");
		bmSid.setPath("/");
		bmSid.setMaxAge(0);
		bmSid.setHttpOnly(true);
		if (SecurityConfig.secureCookies) {
			bmSid.setSecure(true);
		}
		headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(bmSid));

		Cookie openId = new DefaultCookie(OPENID_COOKIE, "delete");
		openId.setPath("/");
		openId.setMaxAge(0);
		openId.setHttpOnly(true);
		if (SecurityConfig.secureCookies) {
			openId.setSecure(true);
		}
		headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(openId));

		Cookie access = new DefaultCookie(ACCESS_COOKIE, "delete");
		access.setPath("/");
		access.setMaxAge(0);
		access.setHttpOnly(true);
		if (SecurityConfig.secureCookies) {
			access.setSecure(true);
		}
		headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(access));

		Cookie refresh = new DefaultCookie(REFRESH_COOKIE, "delete");
		refresh.setPath("/");
		refresh.setMaxAge(0);
		refresh.setHttpOnly(true);
		if (SecurityConfig.secureCookies) {
			refresh.setSecure(true);
		}
		headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(refresh));

		Cookie idc = new DefaultCookie(ID_COOKIE, "delete");
		idc.setPath("/");
		idc.setMaxAge(0);
		idc.setHttpOnly(true);
		if (SecurityConfig.secureCookies) {
			idc.setSecure(true);
		}
		headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(idc));
	}

	private Optional<String> getDomainUid(HttpServerRequest request) {
		SharedMap<String, Map<String, String>> all = MQ
				.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS);

		// Look for host in domains external_url
		Iterator<String> it = all.keys().iterator();
		while (it.hasNext()) {
			String domainUid = it.next();
			Map<String, String> values = all.get(domainUid);
			String extUrl = values.get(DomainSettingsKeys.external_url.name());
			if (request.host().equalsIgnoreCase(extUrl)) {
				return Optional.of(domainUid);
			}
		}

		// Look for host in domains other_urls
		it = all.keys().iterator();
		while (it.hasNext()) {
			String domainUid = it.next();
			Map<String, String> values = all.get(domainUid);
			String otherUrls = values.get(DomainSettingsKeys.other_urls.name());
			if (otherUrls != null) {
				StringTokenizer tokenizer = new StringTokenizer(otherUrls.trim(), " ");
				while (tokenizer.hasMoreElements()) {
					if (request.host().equalsIgnoreCase(tokenizer.nextToken())) {
						return Optional.of(domainUid);
					}
				}
			}
		}

		// Look for a CAS domain without external_url (no matter the request host)
		it = all.keys().iterator();
		while (it.hasNext()) {
			String domainUid = it.next();
			Map<String, String> values = all.get(domainUid);
			String authType = values.get(AuthDomainProperties.AUTH_TYPE.name());
			if (AuthTypes.CAS.name().equals(authType)) {
				String extUrl = values.get(DomainSettingsKeys.external_url.name());
				if (extUrl == null || extUrl.trim().isEmpty()) {
					return Optional.of(domainUid);
				}
			}
		}

		// Look for host in global external_url
		SharedMap<String, String> sysconf = MQ.sharedMap(Shared.MAP_SYSCONF);
		if (request.host().equalsIgnoreCase(sysconf.get(SysConfKeys.external_url.name()))) {
			return Optional.of("global.virt");
		}

		// Look for host in global other_urls
		String otherUrls = sysconf.get(SysConfKeys.other_urls.name());
		if (otherUrls != null) {
			StringTokenizer tokenizer = new StringTokenizer(otherUrls.trim(), " ");
			while (tokenizer.hasMoreElements()) {
				if (request.host().equalsIgnoreCase(tokenizer.nextToken())) {
					return Optional.of("global.virt");
				}
			}
		}

		return Optional.empty();
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
		headers.add("BMUserId", sd.getUserUid());
		headers.add("BMUserLogin", sd.login);
		headers.add("BMAccountType", sd.accountType);
		headers.add("BMUserLATD", sd.loginAtDomain);
		if (sd.defaultEmail != null) {
			headers.add("BMUserDefaultEmail", sd.defaultEmail);
		}

		// Used to order the client to reset it's local data if guid specified
		// in sessionsInfos is different from the one in the local application data
		headers.add("BMMailboxCopyGuid", sd.getMailboxCopyGuid());

		headers.add("BMUserDomainId", sd.domainUid);

		addIfPresent(headers, sd.givenNames, "BMUserFirstName");
		addIfPresent(headers, sd.familyNames, "BMUserLastName");
		addIfPresent(headers, sd.formatedName, sd.login, "BMUserFormatedName");

		headers.add("BMRoles", sd.rolesAsString);

		headers.add("BMUserMailPerms", "true");

		// needed by rouncube ?

		headers.add("bmMailPerms", "true");
		Map<String, String> settings = sd.getSettings();
		String lang = settings.get("lang");
		headers.add("BMLang", lang == null ? "en" : lang);
		String defaultApp = settings.get("default_app");
		if (sd.loginAtDomain.equals("admin0@global.virt")) {
			defaultApp = "/adminconsole/";
		}
		headers.add("BMDefaultApp", defaultApp != null ? defaultApp : "/webmail/");
		headers.add("BMPrivateComputer", "" + sd.isPrivateComputer());

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
