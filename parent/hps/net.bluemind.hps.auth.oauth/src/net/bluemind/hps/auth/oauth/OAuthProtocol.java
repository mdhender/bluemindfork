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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ServerFault;
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
	private static final Decoder b64UrlDecoder = Base64.getUrlDecoder();
	private static final Cache<String, String> codeVerifierCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES).build();
	private OAuthConf oAuthConf;

	public OAuthProtocol(OAuthConf oAuthConf) {
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

		JsonObject jsonState = new JsonObject(new String(b64UrlDecoder.decode(state.getBytes())));
		String codeVerifier = codeVerifierCache.getIfPresent(jsonState.getString("codeVerifierKey"));
		if (Strings.isNullOrEmpty(codeVerifier)) {
			error(request, new Throwable("Failed to fetch codeVerifier"));
			return;
		}

		try {
			String endpoint = oAuthConf.openIdConfiguration().getString("token_endpoint");
			Builder requestBuilder = HttpRequest.newBuilder(new URI(endpoint));
			requestBuilder.header("Charset", StandardCharsets.UTF_8.name());
			requestBuilder.header("Content-Type", "application/x-www-form-urlencoded");
			String params = "grant_type=authorization_code";
			params += "&client_id=" + oAuthConf.clientId();
			params += "&client_secret=" + oAuthConf.clientSecret();
			params += "&code=" + code;
			params += "&code_verifier=" + codeVerifier;
			params += "&redirect_uri=" + String.format("%s://%s/login", request.scheme(), request.host());
			params += "&scope=openid";
			byte[] postData = params.getBytes(StandardCharsets.UTF_8);
			requestBuilder.method("POST", HttpRequest.BodyPublishers.ofByteArray(postData));
			HttpRequest req = requestBuilder.build();
			HttpClient cli = HttpClient.newHttpClient();
			HttpResponse<String> resp = cli.send(req, BodyHandlers.ofString());

			if (resp.statusCode() >= 400) {
				error(request, new Throwable(resp.body()));
				return;
			}

			JsonObject token = new JsonObject(resp.body());
			String redirectTo = jsonState.getString("path");
			validateToken(request, protocol, provider, ss, forwadedFor, token, redirectTo);
		} catch (Exception e) {
			error(request, e);
		}
	}

	private void error(HttpServerRequest req, Throwable e) {
		logger.error("Error during auth: {}", e.getMessage(), e);
		req.response().setStatusCode(500);
		req.response().end();
	}

	private void validateToken(HttpServerRequest request, IAuthProtocol protocol, IAuthProvider prov, ISessionStore ss,
			List<String> forwadedFor, JsonObject token, String redirectTo) {

		DecodedJWT accessToken = JWT.decode(token.getString("access_token"));

		Claim email = accessToken.getClaim("email");
		if (email.isMissing() || email.isNull()) {
			error(request, new ServerFault("Failed to validate id_token: no email"));
			return;
		}

		ExternalCreds creds = new ExternalCreds();
		creds.setLoginAtDomain(email.asString());
		createSession(request, protocol, prov, ss, forwadedFor, creds, redirectTo, token);
	}

	private void createSession(HttpServerRequest request, IAuthProtocol protocol, IAuthProvider prov, ISessionStore ss,
			List<String> forwadedFor, ExternalCreds creds, String redirectTo, JsonObject token) {
		logger.info("Create session for {}", creds.getLoginAtDomain());
		prov.sessionId(creds, forwadedFor, new AsyncHandler<JsonObject>() {
			@Override
			public void success(JsonObject json) {
				String sid = json.getString("sid");
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
				request.response().headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(co));

				request.response().headers().add(HttpHeaders.LOCATION, redirectTo);
				request.response().setStatusCode(302);

				JsonObject cookie = new JsonObject();
				cookie.put("access_token", token.getString("access_token"));
				cookie.put("refresh_token", token.getString("refresh_token"));
				cookie.put("sid", sid);
				cookie.put("domain_uid", json.getString("domain_uid"));

				Cookie openIdCookie = new DefaultCookie("OpenIdToken", cookie.encode());
				openIdCookie.setPath("/");
				openIdCookie.setHttpOnly(true);
				if (SecurityConfig.secureCookies) {
					openIdCookie.setSecure(true);
				}
				request.response().headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(openIdCookie));

				request.response().end();
			}

			@Override
			public void failure(Throwable e) {
				error(request, e);
			}

		});
	}

	public void logout(HttpServerRequest req) {
		String location = oAuthConf.openIdConfiguration().getString("end_session_endpoint");
		req.response().headers().add(HttpHeaders.LOCATION, location);
		req.response().setStatusCode(302);
		req.response().end();
	}

	@Override
	public String getKind() {
		return "OAUTH";
	}

	private void redirect(HttpServerRequest req) {
		String location = oAuthConf.openIdConfiguration().getString("authorization_endpoint");
		String key = UUID.randomUUID().toString();
		String path = Optional.ofNullable(req.path()).orElse("/");

		JsonObject jsonState = new JsonObject();
		jsonState.put("codeVerifierKey", key);
		jsonState.put("path", path);

		String state = b64UrlEncoder.encodeToString(jsonState.encode().getBytes());

		String codeVerifier = createCodeVerifier();
		codeVerifierCache.put(key, codeVerifier);

		String codeChallenge = b64UrlEncoder
				.encodeToString(sha256.hashString(codeVerifier, StandardCharsets.UTF_8).asBytes());

		String redirectUri = String.format("%s://%s/login", req.scheme(), req.host());

		location += "?client_id=" + oAuthConf.clientId();
		location += "&redirect_uri=" + redirectUri;
		location += "&code_challenge=" + codeChallenge;
		location += "&state=" + state;
		location += "&code_challenge_method=S256";
		location += "&response_type=code";
		location += "&scope=openid";

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
