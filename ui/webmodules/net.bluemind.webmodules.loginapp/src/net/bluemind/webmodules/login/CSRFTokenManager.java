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
package net.bluemind.webmodules.login;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.proxy.http.auth.api.SecurityConfig;

public class CSRFTokenManager {

	private static final Logger logger = LoggerFactory.getLogger(CSRFTokenManager.class);

	public final static CSRFTokenManager INSTANCE = new CSRFTokenManager();

	public String initRequest(HttpServerRequest request) {
		String sessionId = UUID.randomUUID().toString();
		Cookie co = new DefaultCookie("HPSSESSION", sessionId);
		co.setPath("/");
		if (SecurityConfig.secureCookies) {
			co.setSecure(true);
		}
		co.setHttpOnly(true);
		request.response().headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(co));
		return sessionId;
	}

	public boolean checkToken(HttpServerRequest req, String csrfToken) {
		String sessionId = currentSessionId(req);
		if (sessionId == null) {
			logger.debug("no session to check csrfToken");
			return false;
		}

		try {
			UUID.fromString(sessionId);
		} catch (IllegalArgumentException e) {
			logger.debug("invalid sessionId {}", sessionId, e);
			return false;
		}

		boolean ret = sessionId.equals(csrfToken);
		logger.debug("csrfToken {}: {} ", csrfToken, ret);
		return ret;
	}

	private String currentSessionId(HttpServerRequest request) {
		String sessionId = null;
		String cookString = request.headers().get("Cookie");
		if (cookString != null) {
			Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookString);
			for (Cookie c : cookies) {
				if ("HPSSESSION".equals(c.name())) {
					sessionId = c.value();
					break;
				}
			}
		}
		return sessionId;
	}
}
