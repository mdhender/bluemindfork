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
package net.bluemind.webmodule.authenticationfilter.internal;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.webmodule.server.SecurityConfig;

public class AuthenticationCookie {

	public static final String OPENID_SESSION = "OpenIdSession";
	public static final String ACCESS_TOKEN = "AccessToken";
	public static final String REFRESH_TOKEN = "RefreshToken";
	public static final String ID_TOKEN = "IdToken";
	public static final String BMSID = "BMSID";
	public static final String BMPRIVACY = "BMPRIVACY";
	public static final String BMREDIRECT = "BM_REDIRECT";

	private AuthenticationCookie() {

	}

	public static void add(MultiMap headers, String name, String value) {
		Cookie cookie = new DefaultCookie(name, value);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		if (SecurityConfig.secureCookies) {
			cookie.setSecure(true);
		}
		headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
	}

	public static void purge(HttpServerRequest request) {
		MultiMap headers = request.response().headers();

		delete(headers, AuthenticationCookie.BMSID);
		delete(headers, AuthenticationCookie.OPENID_SESSION);
		delete(headers, AuthenticationCookie.ACCESS_TOKEN);
		delete(headers, AuthenticationCookie.ID_TOKEN);
		delete(headers, AuthenticationCookie.BMPRIVACY);
		delete(headers, AuthenticationCookie.BMREDIRECT);
	}

	private static void delete(MultiMap headers, String cookieName) {
		Cookie cookie = new DefaultCookie(cookieName, "");
		cookie.setPath("/");
		cookie.setMaxAge(0);
		cookie.setHttpOnly(true);
		if (SecurityConfig.secureCookies) {
			cookie.setSecure(true);
		}
		headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
	}

}
