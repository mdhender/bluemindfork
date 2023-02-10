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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.core.api.BMVersion;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.network.topology.Topology;
import net.bluemind.webmodule.authenticationfilter.internal.SessionData;
import net.bluemind.webmodule.authenticationfilter.internal.SessionsCache;
import net.bluemind.webmodule.server.IWebFilter;

public class AuthenticationFilter implements IWebFilter {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
	private static final HashFunction sha256 = Hashing.sha256();
	private static final Encoder b64UrlEncoder = Base64.getUrlEncoder().withoutPadding();
	private static final Cache<String, String> codeVerifierCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES).build();
	private static final ServerCookieDecoder cookieDecoder = ServerCookieDecoder.LAX;

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
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request) {
		if (request.path().startsWith("/login/") && !request.path().equals("/login/index.html")) {
			return CompletableFuture.completedFuture(request);
		}

		if (request.path().endsWith("/bluemind_sso_logout")) {
			return logout(request);
		}

		if (request.path().equals("/auth/verify")) {
			return CompletableFuture.completedFuture(request);
		}

		String cookieStr = Optional.ofNullable(request.headers().get("cookie")).orElse("");
		Set<Cookie> cookies = cookieDecoder.decode(cookieStr);
		Optional<Cookie> oidc = cookies.stream().filter(c -> "OpenIdToken".equals(c.name())).findFirst();

		if (oidc.isPresent()) {
			JsonObject token = new JsonObject(oidc.get().value());
			String sessionId = token.getString("sid");
			decorate(request, sessionId);
			return CompletableFuture.completedFuture(request);
		}

		Optional<String> domainUid = getDomainUid(request);
		if (domainUid.isEmpty()) {
			request.response().headers().add(HttpHeaders.LOCATION, "/login/native");
			request.response().setStatusCode(301);
			request.response().end();
			return CompletableFuture.completedFuture(null);
		}

		Map<String, String> domainSettings = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid.get());

		String key = UUID.randomUUID().toString();
		String path = Optional.ofNullable(request.path()).orElse("/");

		JsonObject jsonState = new JsonObject();
		jsonState.put("codeVerifierKey", key);
		jsonState.put("path", path);
		jsonState.put("domain_uid", domainUid.get());

		String state = b64UrlEncoder.encodeToString(jsonState.encode().getBytes());

		String codeVerifier = createCodeVerifier();
		AuthenticationFilter.put(key, codeVerifier);

		String codeChallenge = b64UrlEncoder
				.encodeToString(sha256.hashString(codeVerifier, StandardCharsets.UTF_8).asBytes());

		String location = domainSettings.get(DomainSettingsKeys.openid_authorization_endpoint.name());
		location += "?client_id=" + domainSettings.get(DomainSettingsKeys.openid_client_id.name());
		location += "&redirect_uri=" + String.format("%s://%s/auth/verify", request.scheme(), request.host());
		location += "&code_challenge=" + codeChallenge;
		location += "&state=" + state;
		location += "&code_challenge_method=S256";
		location += "&response_type=code";
		location += "&scope=openid";

		request.response().headers().add(HttpHeaders.LOCATION, location);
		request.response().setStatusCode(301);
		request.response().end();
		return CompletableFuture.completedFuture(null);

	}

	private CompletableFuture<HttpServerRequest> logout(HttpServerRequest request) {
		Optional<String> domainUid = getDomainUid(request);
		if (domainUid.isEmpty()) {
			return CompletableFuture.completedFuture(request);
		}

		Map<String, String> domainSettings = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domainUid.get());

		request.response().headers().add(HttpHeaders.LOCATION,
				domainSettings.get(DomainSettingsKeys.openid_end_session_endpoint.name()));
		request.response().setStatusCode(302);
		request.response().end();

		return CompletableFuture.completedFuture(null);
	}

	private Optional<String> getDomainUid(HttpServerRequest request) {
		SharedMap<String, Map<String, String>> all = MQ
				.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS);

		return all.keys().stream().filter(domainUid -> {
			Map<String, String> values = all.get(domainUid);
			String extUrl = values.get(DomainSettingsKeys.external_url.name());
			return request.host().equals(extUrl);
		}).findFirst();
	}

	private String createCodeVerifier() {
		SecureRandom sr = new SecureRandom();
		byte[] code = new byte[32];
		sr.nextBytes(code);
		return b64UrlEncoder.encodeToString(code);
	}

	private void decorate(HttpServerRequest request, String sessionId) {

		MultiMap headers = request.headers();

		SessionData sd = SessionsCache.get().getIfPresent(sessionId);

		if (sd == null) {
			logger.error("session {} doesnt exists", sessionId);
			request.response().setStatusCode(500);
			request.response().end();
			return;
		}

		headers.add("BMSessionId", sd.authKey);
		headers.add("BMUserId", "" + sd.getUserUid());
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

		addIfPresent(request.response(), sd.givenNames, "BMUserFirstName");
		addIfPresent(request.response(), sd.familyNames, "BMUserLastName");
		addIfPresent(request.response(), sd.formatedName, sd.login, "BMUserFormatedName");

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

	private void addIfPresent(HttpServerResponse proxyReq, String value, String fallback, String headerKey) {
		if (!addIfPresent(proxyReq, value, headerKey)) {
			addIfPresent(proxyReq, fallback, headerKey);
		}
	}

	private boolean addIfPresent(HttpServerResponse proxyReq, String value, String headerKey) {
		if (value != null) {
			proxyReq.headers().add(headerKey, java.util.Base64.getEncoder().encodeToString(value.getBytes()));
			return true;
		} else {
			return false;
		}
	}

}
