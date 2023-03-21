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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.webmodule.authenticationfilter.internal.ExternalCreds;
import net.bluemind.webmodule.server.CSRFTokenManager;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.server.SecurityConfig;

public class FormHandler implements Handler<HttpServerRequest>, NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(FormHandler.class);

	private Vertx vertx;

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;

	}

	@Override
	public void handle(HttpServerRequest event) {
		if (event.method() == HttpMethod.POST) {
			event.setExpectMultipart(true);
			event.endHandler(e -> loginFormSubmitted(event));
			return;
		}
		event.response().end();
	}

	private void loginFormSubmitted(HttpServerRequest request) {
		MultiMap attributes = request.formAttributes();
		String login = attributes.get("login");
		String pass = attributes.get("password");

		if (Strings.isNullOrEmpty(login)) {
			error(request, new ServerFault("invalid login", ErrorCode.INVALID_PARAMETER));
			return;
		}

		if (!"admin0@global.virt".equals(login)) {
			error(request, new ServerFault("invalid login", ErrorCode.INVALID_PARAMETER));
			return;
		}

		if (!CSRFTokenManager.INSTANCE.checkToken(request, attributes.get("csrfToken"))) {
			error(request, new ServerFault("invalid token", ErrorCode.INVALID_PARAMETER));
			return;
		}

		AuthProvider prov = new AuthProvider(vertx);
		ExternalCreds creds = new ExternalCreds();
		creds.setLoginAtDomain(login);
		createSession(request, login, pass, prov);
	}

	private void createSession(HttpServerRequest request, String login, String pass, AuthProvider prov) {

		MultiMap attributes = request.formAttributes();
		final boolean privateComputer = "priv".equals(attributes.get("priv"));

		List<String> forwadedFor = new ArrayList<>(request.headers().getAll("X-Forwarded-For"));
		forwadedFor.add(request.remoteAddress().host());

		prov.sessionId(login, pass, forwadedFor, new AsyncHandler<String>() {

			@Override
			public void success(String sid) {
				MultiMap headers = request.response().headers();

				if (sid == null) {
					logger.error("Error during auth, {} login not valid (not found/archived or not user)", login);
					headers.add(HttpHeaders.LOCATION, "/errors-pages/deniedAccess.html?login=" + login);
					request.response().setStatusCode(302);
					request.response().end();
					return;
				}

				Cookie co = new DefaultCookie("BMSID", sid);
				co.setPath("/");
				co.setHttpOnly(true);
				if (SecurityConfig.secureCookies) {
					co.setSecure(true);
				}

				Cookie privacyCo = new DefaultCookie("BMPRIVACY", Boolean.toString(privateComputer));
				privacyCo.setPath("/");
				if (SecurityConfig.secureCookies) {
					privacyCo.setSecure(true);
				}
				headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(privacyCo));
				headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(co));

				headers.add(HttpHeaders.LOCATION, "/");
				request.response().setStatusCode(302);

				request.response().end();
			}

			@Override
			public void failure(Throwable e) {
				error(request, e);
			}
		});
	}

	private void error(HttpServerRequest req, Throwable e) {
		MultiMap attributes = req.formAttributes();
		final String askedUri = checkAskedUri(attributes);

		int code = 10;
		if (e instanceof ServerFault sf) {
			if (sf.getCode() == ErrorCode.INVALID_PARAMETER) {
				code = 1;
			} else if (sf.getCode() == ErrorCode.INVALID_PASSWORD) {
				code = 2;
			}
		}

		if (code == 2) {
			logger.warn("Invalid password ({}), display login page.", e.getMessage());
		} else {
			logger.warn("Auth failure ({}), display login page.", e.getMessage(), e);
		}

		String q = "?authErrorCode=" + code;
		if (askedUri != null) {
			try {
				new URI(askedUri);
				q += "&askedUri=" + URLEncoder.encode(askedUri, StandardCharsets.UTF_8.toString());
			} catch (URISyntaxException | UnsupportedEncodingException e1) {
				logger.warn("asked uri is not a valid uri : {} ", askedUri, e1);
			}
		}

		HttpServerResponse resp = req.response();

		try {
			String login = URLEncoder.encode(attributes.get("login"), StandardCharsets.UTF_8.toString());
			q += "&userLogin=" + login;
		} catch (UnsupportedEncodingException e1) {
			logger.error("unsupported encoding", e1);
			resp.setStatusCode(500);
			resp.end();
			return;
		}

		final boolean privateComputer = "priv".equals(attributes.get("priv"));

		Cookie privacyCo = new DefaultCookie("BMPRIVACY", Boolean.toString(privateComputer));
		privacyCo.setPath("/");
		if (SecurityConfig.secureCookies) {
			privacyCo.setSecure(true);
		}

		resp.headers().add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(privacyCo));
		resp.headers().add(HttpHeaders.LOCATION, req.scheme() + "://" + req.host() + "/login/native" + q);
		resp.setStatusCode(302);
		resp.end();
	}

	private String checkAskedUri(MultiMap attributes) {
		String askedUri = attributes.get("askedUri") != null ? attributes.get("askedUri") : "/";

		try {
			new URI(askedUri);
		} catch (URISyntaxException e1) {
			logger.warn("asked uri is not un uri : {} ", askedUri, e1);
			askedUri = "/";
		}

		return askedUri;
	}
}
