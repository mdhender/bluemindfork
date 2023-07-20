/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.webmodule.authenticationfilter;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.webmodule.authenticationfilter.internal.AuthenticationCookie;
import net.bluemind.webmodule.authenticationfilter.internal.ExternalCreds;
import net.bluemind.webmodule.server.SecurityConfig;

public class SudoHandler extends AbstractAuthHandler implements Handler<HttpServerRequest> {
	private static final Logger logger = LoggerFactory.getLogger(SudoHandler.class);

	private String priv;
	private String login;
	private String password;

	@Override
	public void handle(HttpServerRequest request) {
		List<String> forwadedFor = new ArrayList<>(request.headers().getAll("X-Forwarded-For"));
		forwadedFor.add(request.remoteAddress().host());

		if (HttpMethod.POST.equals(request.method())) {
			request.setExpectMultipart(true);
			request.endHandler(e -> {
				MultiMap attributes = request.formAttributes();
				priv = attributes.get("priv");
				login = attributes.get("login");
				password = attributes.get("password");
				createSession(request, forwadedFor);
			});
		}
	}

	private void createSession(HttpServerRequest request, List<String> forwadedFor) {
		logger.info("Creating session for {}/{}", login, password);

		AuthProvider prov = new AuthProvider(vertx);
		ExternalCreds creds = new ExternalCreds();
		creds.setLoginAtDomain(login);
		prov.sessionId(login, password, forwadedFor, new AsyncHandler<String>() {
			@Override
			public void success(String sid) {
				Cookie cookie = new DefaultCookie(AuthenticationCookie.BMSID, sid);
				cookie.setPath("/");
				cookie.setHttpOnly(true);
				if (SecurityConfig.secureCookies) {
					cookie.setSecure(true);
				}
				request.response().headers().add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));

				Cookie privacyCo = new DefaultCookie(AuthenticationCookie.BMPRIVACY,
						Boolean.toString("true".equals(priv)));
				privacyCo.setPath("/");
				if (SecurityConfig.secureCookies) {
					privacyCo.setSecure(true);
				}
				request.response().headers().add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(privacyCo));

				request.response().setStatusCode(200);
				request.response().end();
			}

			@Override
			public void failure(Throwable e) {
				error(request, e);
			}
		});
	}
}
