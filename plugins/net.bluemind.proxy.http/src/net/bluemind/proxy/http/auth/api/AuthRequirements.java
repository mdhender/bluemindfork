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

import io.vertx.core.http.HttpServerRequest;
import net.bluemind.proxy.http.auth.api.CookieHelper.CookieState;
import net.bluemind.proxy.http.auth.api.CookieHelper.CookieStatus;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.IAuthProtocol;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.ISessionStore;

public class AuthRequirements {
	public boolean authNeeded;
	public String externalAuthURL;
	public String sessionId;
	public String cookie;
	public IAuthProtocol protocol;

	private AuthRequirements(boolean authNeeded) {
		this.authNeeded = authNeeded;
		this.externalAuthURL = null;
	}

	public static AuthRequirements existingSession(ISessionStore ss, HttpServerRequest request) {
		CookieStatus cs = CookieHelper.check(ss, request);
		if (cs.state != CookieState.Ok) {
			throw new IllegalArgumentException("MUST NOT call this method without a session");
		}

		AuthRequirements ret = new AuthRequirements(false);
		ret.sessionId = cs.sessionId;
		ret.cookie = cs.cookieValue;
		ret.protocol = ss.getProtocol(ret.sessionId);
		return ret;
	}

	public static AuthRequirements needSession(IAuthProtocol protocol) {
		AuthRequirements ret = new AuthRequirements(true);
		ret.protocol = protocol;
		return ret;
	}

	public static AuthRequirements noNeedSession() {
		AuthRequirements ret = new AuthRequirements(false);
		return ret;
	}

	public static AuthRequirements notHandle() {
		AuthRequirements ret = new AuthRequirements(false);
		return ret;
	}

}
