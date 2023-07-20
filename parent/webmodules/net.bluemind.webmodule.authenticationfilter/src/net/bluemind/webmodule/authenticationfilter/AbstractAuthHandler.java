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

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.webmodule.authenticationfilter.internal.AuthenticationCookie;
import net.bluemind.webmodule.authenticationfilter.internal.ExternalCreds;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.server.SecurityConfig;

public abstract class AbstractAuthHandler implements NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(AbstractAuthHandler.class);

	protected Vertx vertx;

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;

	}

	protected void createSession(HttpServerRequest request, AuthProvider prov, List<String> forwadedFor,
			ExternalCreds creds, String redirectTo, String domainUid) {
		createSession(request, prov, forwadedFor, creds, redirectTo, domainUid, null);
	}

	protected void createSession(HttpServerRequest request, AuthProvider prov, List<String> forwadedFor,
			ExternalCreds creds, String redirectTo) {
		createSession(request, prov, forwadedFor, creds, redirectTo, null, null);
	}

	protected void createSession(HttpServerRequest request, AuthProvider prov, List<String> forwadedFor,
			ExternalCreds creds, String redirectTo, String domainUid, JsonObject token) {
		logger.info("Create session for {}", creds.getLoginAtDomain());
		prov.sessionId(creds, forwadedFor, new AsyncHandler<String>() {
			@Override
			public void success(String sid) {

				MultiMap headers = request.response().headers();

				if (sid == null) {
					logger.error("Error during auth, {} login not valid (not found/archived or not user)",
							creds.getLoginAtDomain());
					headers.add(HttpHeaders.LOCATION,
							"/errors-pages/deniedAccess.html?login=" + creds.getLoginAtDomain());
					request.response().setStatusCode(302);
					request.response().end();
					return;
				}

				if (token != null) {
					JsonObject cookie = new JsonObject();
					cookie.put("sid", sid);
					cookie.put("domain_uid", domainUid);

					Cookie openIdCookie = new DefaultCookie(AuthenticationCookie.OPENID_SESSION, cookie.encode());
					openIdCookie.setPath("/");
					openIdCookie.setHttpOnly(true);
					if (SecurityConfig.secureCookies) {
						openIdCookie.setSecure(true);
					}
					request.response().headers().add(HttpHeaders.SET_COOKIE,
							ServerCookieEncoder.LAX.encode(openIdCookie));

					Cookie accessCookie = new DefaultCookie(AuthenticationCookie.ACCESS_TOKEN,
							token.getString("access_token"));
					accessCookie.setPath("/");
					accessCookie.setHttpOnly(true);
					if (SecurityConfig.secureCookies) {
						accessCookie.setSecure(true);
					}
					request.response().headers().add(HttpHeaders.SET_COOKIE,
							ServerCookieEncoder.LAX.encode(accessCookie));

					Cookie refreshCookie = new DefaultCookie(AuthenticationCookie.REFRESH_TOKEN,
							token.getString("refresh_token"));
					refreshCookie.setPath("/");
					refreshCookie.setHttpOnly(true);
					if (SecurityConfig.secureCookies) {
						refreshCookie.setSecure(true);
					}
					request.response().headers().add(HttpHeaders.SET_COOKIE,
							ServerCookieEncoder.LAX.encode(refreshCookie));

					Cookie idCookie = new DefaultCookie(AuthenticationCookie.ID_TOKEN, token.getString("id_token"));
					idCookie.setPath("/");
					idCookie.setHttpOnly(true);
					if (SecurityConfig.secureCookies) {
						idCookie.setSecure(true);
					}
					request.response().headers().add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(idCookie));

					DecodedJWT accessToken = JWT.decode(token.getString("access_token"));
					Claim pubpriv = accessToken.getClaim("bm_pubpriv");
					boolean privateComputer = "private".equals(pubpriv.asString());
					Cookie privacyCo = new DefaultCookie(AuthenticationCookie.BMPRIVACY,
							Boolean.toString(privateComputer));
					privacyCo.setPath("/");
					if (SecurityConfig.secureCookies) {
						privacyCo.setSecure(true);
					}
					request.response().headers().add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(privacyCo));

				} else {
					Cookie cookie = new DefaultCookie(AuthenticationCookie.BMSID, sid);
					cookie.setPath("/");
					cookie.setHttpOnly(true);
					if (SecurityConfig.secureCookies) {
						cookie.setSecure(true);
					}
					headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
				}

				headers.add(HttpHeaders.LOCATION, redirectTo);
				request.response().setStatusCode(302);

				request.response().end();
			}

			@Override
			public void failure(Throwable e) {
				error(request, e);
			}

		});
	}

	protected void error(HttpServerRequest req, Throwable e) {
		logger.error(e.getMessage(), e);
		req.response().setStatusCode(500);
		req.response().end();
	}

	protected HttpClient initHttpClient(URI uri) {
		HttpClientOptions opts = new HttpClientOptions();
		opts.setDefaultHost(uri.getHost());
		opts.setSsl(uri.getScheme().equalsIgnoreCase("https"));
		opts.setDefaultPort(
				uri.getPort() != -1 ? uri.getPort() : (uri.getScheme().equalsIgnoreCase("https") ? 443 : 80));
		if (opts.isSsl()) {
			opts.setTrustAll(true);
			opts.setVerifyHost(false);
		}
		return vertx.createHttpClient(opts);
	}

}