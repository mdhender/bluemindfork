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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.proxy.http.ExternalCreds;
import net.bluemind.proxy.http.IAuthProvider;
import net.bluemind.proxy.http.IAuthProviderFactory;
import net.bluemind.proxy.http.IDecorableRequest;
import net.bluemind.proxy.http.ILogoutListener;

public class NoneAuthProviderFactory implements IAuthProviderFactory, IAuthProvider {

	private static final Logger logger = LoggerFactory.getLogger(NoneAuthProviderFactory.class);
	private ILogoutListener ll;

	@Override
	public IAuthProvider get(Vertx vertx) {
		return this;
	}

	@Override
	public void setLogoutListener(ILogoutListener ll) {
		this.ll = ll;
		logger.debug(this.ll.toString());
	}

	@Override
	public String getKind() {
		return "NONE";
	}

	@Override
	public void sessionId(String loginAtDomain, String password, boolean privateComputer, List<String> remoteIps,
			AsyncHandler<String> handler) {
		handler.success("noneSid");
	}

	@Override
	public void decorate(String sessionId, IDecorableRequest proxyReq) {
	}

	@Override
	public void ping(String sessionId, AsyncHandler<Boolean> handler) {
		handler.success(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.proxy.http.IAuthProvider#reload(java.lang.String)
	 */
	@Override
	public void reload(String sessionId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionId(ExternalCreds krbCreds, List<String> remoteIps, AsyncHandler<String> handler) {
		handler.success("noneSid");
	}

	@Override
	public boolean inRole(String sessionId, String role) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public CompletableFuture<Void> logout(String sessionId) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public boolean isPasswordExpired(String sessionId) {
		return false;
	}

	@Override
	public CompletableFuture<Void> updatePassword(String sessionId, String currentPassword, String newPassword,
			List<String> forwadedFor) {
		return CompletableFuture.completedFuture(null);
	}
}
