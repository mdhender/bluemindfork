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

	private AuthenticationCookie() {

	}

	public static void purge(HttpServerRequest request) {

		MultiMap headers = request.response().headers();

		Cookie bmSid = new DefaultCookie(AuthenticationCookie.BMSID, "");
		bmSid.setPath("/");
		bmSid.setMaxAge(0);
		bmSid.setHttpOnly(true);
		if (SecurityConfig.secureCookies) {
			bmSid.setSecure(true);
		}
		headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(bmSid));

		Cookie openId = new DefaultCookie(AuthenticationCookie.OPENID_SESSION, "");
		openId.setPath("/");
		openId.setMaxAge(0);
		openId.setHttpOnly(true);
		if (SecurityConfig.secureCookies) {
			openId.setSecure(true);
		}
		headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(openId));

		Cookie access = new DefaultCookie(AuthenticationCookie.ACCESS_TOKEN, "");
		access.setPath("/");
		access.setMaxAge(0);
		access.setHttpOnly(true);
		if (SecurityConfig.secureCookies) {
			access.setSecure(true);
		}
		headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(access));

		Cookie refresh = new DefaultCookie(AuthenticationCookie.REFRESH_TOKEN, "");
		refresh.setPath("/");
		refresh.setMaxAge(0);
		refresh.setHttpOnly(true);
		if (SecurityConfig.secureCookies) {
			refresh.setSecure(true);
		}
		headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(refresh));

		Cookie idc = new DefaultCookie(AuthenticationCookie.ID_TOKEN, "");
		idc.setPath("/");
		idc.setMaxAge(0);
		idc.setHttpOnly(true);
		if (SecurityConfig.secureCookies) {
			idc.setSecure(true);
		}
		headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(idc));

		Cookie bmPrivacy = new DefaultCookie(AuthenticationCookie.BMPRIVACY, "");
		bmPrivacy.setPath("/");
		bmPrivacy.setMaxAge(0);
		bmPrivacy.setHttpOnly(true);
		if (SecurityConfig.secureCookies) {
			bmPrivacy.setSecure(true);
		}
		headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(bmPrivacy));

	}

}
