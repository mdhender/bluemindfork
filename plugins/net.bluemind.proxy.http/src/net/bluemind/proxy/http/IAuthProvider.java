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
package net.bluemind.proxy.http;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.bluemind.core.api.AsyncHandler;

public interface IAuthProvider {

	void sessionId(String loginAtDomain, String password, boolean privateComputer, List<String> remoteIps,
			AsyncHandler<String> handler);

	void decorate(String sessionId, IDecorableRequest proxyReq);

	void ping(String sessionId, AsyncHandler<Boolean> handler);

	void reload(String sessionId);

	void sessionId(ExternalCreds krbCreds, List<String> remoteIps, AsyncHandler<String> handler);

	boolean inRole(String sessionId, String role);

	CompletableFuture<Void> logout(String sessionId);
}
