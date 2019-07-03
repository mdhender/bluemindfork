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

import org.vertx.java.core.http.HttpServerRequest;

import net.bluemind.proxy.http.IAuthProvider;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.ISessionStore;

public final class UserReq {

	public final HttpServerRequest fromClient;
	public final IAuthProvider provider;
	public final ISessionStore store;
	public final String sessionId;

	public UserReq(String sessionId, HttpServerRequest event, IAuthProvider provider, ISessionStore store) {
		this.fromClient = event;
		this.provider = provider;
		this.sessionId = sessionId;
		this.store = store;
	}

	@Override
	public String toString() {
		return "UserReq [fromClient=" + fromClient + ", provider=" + provider + ", sessionId=" + sessionId + "]";
	}

}
