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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.hps.auth.oauth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.proxy.http.ExternalCreds;
import net.bluemind.proxy.http.IAuthProvider;
import net.bluemind.proxy.http.auth.api.AuthRequirements;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.IAuthProtocol;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.ISessionStore;
import net.bluemind.proxy.http.auth.api.SecurityConfig;

public class OAuthProtocol implements IAuthProtocol {

	private static final Logger logger = LoggerFactory.getLogger(OAuthProtocol.class);
	private static final HashFunction sha256 = Hashing.sha256();
	private static final Encoder b64UrlEncoder = Base64.getUrlEncoder().withoutPadding();
	private static final Cache<String, String> codeVerifierCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES).build();
	private OAuthConf oAuthConf;
	private HttpClient httpClient;

	public OAuthProtocol(HttpClient client, OAuthConf oAuthConf) {
		this.httpClient = client;
		this.oAuthConf = oAuthConf;
	}

	@Override
	public void proceed(AuthRequirements authState, ISessionStore ss, IAuthProvider provider, HttpServerRequest req) {
		if (req.params().get("code") == null) {
			String logoutUri = String.format("%s://%s/logout", req.scheme(), req.host());
			if (req.path().endsWith("bluemind_sso_logout")
					&& !Strings.isNullOrEmpty(req.headers().get(HttpHeaders.REFERER))
					&& req.headers().get(HttpHeaders.REFERER).toLowerCase().equals(logoutUri)) {
				HttpServerResponse resp = req.response();
				resp.setStatusCode(200);
				resp.end();
				return;
			}
			redirect(req);
		} else {
			getToken(req, authState.protocol, provider, ss);
		}
	}

	private void getToken(HttpServerRequest request, IAuthProtocol protocol, IAuthProvider provider, ISessionStore ss) {
		List<String> forwadedFor = new ArrayList<>(request.headers().getAll("X-Forwarded-For"));
		forwadedFor.add(request.remoteAddress().host());

		String code = request.params().get("code");
		String state = request.params().get("state");

		String codeVerifier = codeVerifierCache.getIfPresent(state);
		if (Strings.isNullOrEmpty(codeVerifier)) {
			error(request, "Failed to fetch codeVerifier");
			return;
		}

		String uri = String.format("/realms/%s/protocol/openid-connect/token", oAuthConf.realm());

		try {
			String redirectUri = String.format("%s://%s/login", request.scheme(), request.host());
			httpClient.request(HttpMethod.POST, uri).onSuccess(req -> {
				String params = "grant_type=authorization_code";
				params += "&client_id=" + oAuthConf.clientId();
				params += "&client_secret=" + oAuthConf.clientSecret();
				params += "&code=" + code;
				params += "&code_verifier=" + codeVerifier;
				params += "&redirect_uri=" + redirectUri;

				byte[] postData = params.getBytes(StandardCharsets.UTF_8);

				req.putHeader("Content-Type", "application/x-www-form-urlencoded");
				req.putHeader("Charset", StandardCharsets.UTF_8.name());
				req.putHeader("Content-Length", Integer.toString(postData.length));

				req.send(Buffer.buffer(postData)).onSuccess(resp -> {
					if (resp.statusCode() >= 400) {
						error(request, resp.statusMessage());
						return;
					}
					resp.bodyHandler(buf -> {
						JsonObject response = new JsonObject(buf);
						String token = response.getString("access_token");
						resp.bodyHandler(body -> validateToken(request, protocol, provider, ss, forwadedFor, token));
					});
				}).onFailure(e -> error(request, e.getMessage()));
			}).onFailure(e -> error(request, e.getMessage()));
		} catch (Exception e) {
			error(request, e.getMessage());
		}

	}

	private void error(HttpServerRequest req, String message) {
		logger.error("Error during auth: {}", message);
		req.response().setStatusCode(500);
		req.response().end();
	}

	private void validateToken(HttpServerRequest request, IAuthProtocol protocol, IAuthProvider prov, ISessionStore ss,
			List<String> forwadedFor, String token) {

		String uri = String.format("/realms/%s/protocol/openid-connect/userinfo", oAuthConf.realm());
		httpClient.request(HttpMethod.GET, uri).onSuccess(req -> {
			req.putHeader("Authorization", "Bearer " + token);
			req.send().onSuccess(resp -> {
				if (resp.statusCode() >= 400) {
					error(request, resp.statusMessage());
					return;
				}
				resp.bodyHandler(buf -> {
					JsonObject response = new JsonObject(buf);
					ExternalCreds creds = new ExternalCreds();
					creds.setTicket(token);
					creds.setLoginAtDomain(response.getString("email"));
					createSession(request, protocol, prov, ss, forwadedFor, creds);
				});
			});
		}).onFailure(e -> {
			logger.error("Failed to fetch user profile", e);
			redirect(request);
		});

	}

	private void createSession(HttpServerRequest request, IAuthProtocol protocol, IAuthProvider prov, ISessionStore ss,
			List<String> forwadedFor, ExternalCreds creds) {
		logger.info("Create session for {}", creds.getLoginAtDomain());
		prov.sessionId(creds, forwadedFor, new AsyncHandler<String>() {
			@Override
			public void success(String sid) {
				if (sid == null) {
					logger.error("Error during auth, {} login not valid (not found/archived or not user)",
							creds.getLoginAtDomain());
					request.response().headers().add(HttpHeaders.LOCATION,
							String.format("/errors-pages/deniedAccess.html?login=%s", creds.getLoginAtDomain()));
					request.response().setStatusCode(302);
					request.response().end();
					return;
				}

				// get cookie...
				String proxySid = ss.newSession(sid, protocol);

				logger.info("Got sid: {}, proxySid: {}", sid, proxySid);

				Cookie co = new DefaultCookie("BMHPS", proxySid);
				co.setPath("/");
				co.setHttpOnly(true);
				if (SecurityConfig.secureCookies) {
					co.setSecure(true);
				}
				request.response().headers().add(HttpHeaders.LOCATION, "/");
				request.response().setStatusCode(302);

				request.response().headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(co));
				request.response().end();
			}

			@Override
			public void failure(Throwable e) {
				error(request, e.getMessage());
			}

		});
	}

	public void logout(HttpServerRequest req) {
		String location = String.format("%s://%s:%d/realms/%s/protocol/openid-connect/logout",
				oAuthConf.port() == 443 ? "https" : "http", oAuthConf.host(), oAuthConf.port(), oAuthConf.realm());
		req.response().headers().add(HttpHeaders.LOCATION, location);
		req.response().setStatusCode(302);
		req.response().end();
	}

	@Override
	public String getKind() {
		return "OAUTH";
	}

	private void redirect(HttpServerRequest req) {
		String location = String.format("%s://%s:%d/realms/%s/protocol/openid-connect/auth",
				oAuthConf.port() == 443 ? "https" : "http", oAuthConf.host(), oAuthConf.port(), oAuthConf.realm());

		String state = UUID.randomUUID().toString();
		String codeVerifier = createCodeVerifier();
		codeVerifierCache.put(state, codeVerifier);

		String codeChallenge = b64UrlEncoder
				.encodeToString(sha256.hashString(codeVerifier, StandardCharsets.UTF_8).asBytes());

		String redirectUri = String.format("%s://%s/login", req.scheme(), req.host());

		location += "?client_id=" + oAuthConf.clientId();
		location += "&redirect_uri=" + redirectUri;
		location += "&code_challenge=" + codeChallenge;
		location += "&state=" + state;
		location += "&code_challenge_method=S256";
		location += "&response_type=code";

		req.response().headers().add(HttpHeaders.LOCATION, location);
		req.response().setStatusCode(302);
		req.response().end();
	}

	private String createCodeVerifier() {
		SecureRandom sr = new SecureRandom();
		byte[] code = new byte[32];
		sr.nextBytes(code);
		return b64UrlEncoder.encodeToString(code);
	}

}
