/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.proxy.http.impl.vertx;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.proxy.http.IAuthProvider;
import net.bluemind.proxy.http.auth.api.AuthRequirements;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.IAuthProtocol;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.ISessionStore;
import net.bluemind.proxy.http.auth.api.SecurityConfig;

public class SudoProtocol implements IAuthProtocol {
	public static final String KIND = "SUDO";

	private static final Logger logger = LoggerFactory.getLogger(SudoProtocol.class);

	public SudoProtocol() {
	}

	@Override
	public void proceed(AuthRequirements authState, final ISessionStore ss, final IAuthProvider prov,
			final HttpServerRequest req) {
		final String uri = req.uri();

		logger.debug("handling uri containing bluemind_sso_security: {}", uri);
		req.setExpectMultipart(true);
		req.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				logger.debug("handling form submitted containing bluemind_sso_security: {}", uri);
				formSubmitted(authState.protocol, prov, ss, req);
			}

		});
	}

	private void formSubmitted(IAuthProtocol protocol, IAuthProvider prov, final ISessionStore ss,
			HttpServerRequest req) {
		MultiMap attributes = req.formAttributes();
		String login = attributes.get("login");
		String pass = attributes.get("password");
		final boolean privateComputer = "priv".equals(attributes.get("priv"));

		logger.info("[{}] l: '{}'", prov, login);

		final HttpServerResponse resp = req.response();
		List<String> forwadedFor = new ArrayList<>(req.headers().getAll("X-Forwarded-For"));
		forwadedFor.add(req.remoteAddress().host());

		prov.sessionId(login, pass, privateComputer, forwadedFor, new AsyncHandler<String>() {

			@Override
			public void success(String sid) {

				// get cookie...
				String proxySid = ss.newSession(sid, protocol);
				logger.info("Got sid: {}, proxySid: {}", sid, proxySid);

				resp.setStatusCode(200);
				Cookie co = new DefaultCookie("BMHPS", proxySid);
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

				resp.headers().add("BMSsoCookie", proxySid);
				resp.headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(co));
				resp.headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(privacyCo));
				resp.end();
			}

			@Override
			public void failure(Throwable e) {
				logger.warn("Auth failure: ", e);
				resp.setStatusCode(401);
				resp.end();

			}
		});

	}

	@Override
	public void logout(HttpServerRequest event) {
		HttpServerResponse resp = event.response();
		resp.headers().add("Location", "/");
		resp.setStatusCode(302);
		resp.end();
	}

	@Override
	public String getKind() {
		return KIND;
	}
}
