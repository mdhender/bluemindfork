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
package net.bluemind.webmodules.login.hps;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.proxy.http.NeedVertx;
import net.bluemind.proxy.http.auth.api.AuthRequirements;
import net.bluemind.proxy.http.auth.api.CookieHelper;
import net.bluemind.proxy.http.auth.api.CookieHelper.CookieState;
import net.bluemind.proxy.http.auth.api.CookieHelper.CookieStatus;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer;

public class AuthEnforcer implements IAuthEnforcer, NeedVertx {

	private WebModuleProtocol protocol;

	@Override
	public AuthRequirements enforce(ISessionStore sc, HttpServerRequest event) {
		CookieStatus cs = CookieHelper.check(sc, event);
		if (cs.state == CookieState.Ok) {
			throw new IllegalArgumentException("MUST NOT call this method with a valid session");
		}

		return AuthRequirements.needSession(protocol);
	}

	@Override
	public void setVertx(Vertx vertx) {
		protocol = new WebModuleProtocol(vertx);
	}

}
