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
package net.bluemind.proxy.http.auth.api;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.ISessionStore;

public final class CookieHelper {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(CookieHelper.class);

	public enum CookieState {
		Ok, None
	}

	public static class CookieStatus {
		public final CookieState state;
		public final String cookieValue;
		public final String sessionId;

		CookieStatus(String value, String sessionId, CookieState state) {
			this.cookieValue = value;
			this.sessionId = sessionId;
			this.state = state;
		}

		public static CookieStatus ok(String value, String sessionId) {
			return new CookieStatus(value, sessionId, CookieState.Ok);
		}

		public static CookieStatus none() {
			return new CookieStatus(null, null, CookieState.None);
		}

	}

	public static CookieStatus check(ISessionStore ss, HttpServerRequest event) {
		// get BMHPS from url or cookie
		String bmhps = event.params().get("BMHPS");
		if (bmhps != null) {
			Cookie co = new DefaultCookie("BMHPS", bmhps);
			co.setPath("/");
			co.setHttpOnly(true);
			if (SecurityConfig.secureCookies) {
				co.setSecure(true);
			}
			event.response().headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(co));
		} else {
			String cookString = event.headers().get("Cookie");
			if (cookString != null) {
				Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookString);
				for (Cookie c : cookies) {
					if ("BMHPS".equals(c.name())) {
						bmhps = c.value();
						break;
					}
				}
			}
		}

		if (bmhps != null && ss.getSessionId(bmhps) != null) {
			return CookieStatus.ok(bmhps, ss.getSessionId(bmhps));
		}

		return CookieStatus.none();
	}

	public static void purgeSessionCookie(MultiMap headers) {
		Cookie co = new DefaultCookie("BMHPS", "delete");
		co.setPath("/");
		co.setMaxAge(0);
		co.setHttpOnly(true);
		if (SecurityConfig.secureCookies) {
			co.setSecure(true);
		}
		headers.add("Set-Cookie", ServerCookieEncoder.LAX.encode(co));
	}

}
