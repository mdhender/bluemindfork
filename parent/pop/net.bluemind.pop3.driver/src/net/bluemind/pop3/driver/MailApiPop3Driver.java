/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.pop3.driver;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.api.IAuthenticationPromise;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.network.topology.TopologyException;
import net.bluemind.pop3.endpoint.MailboxConnection;
import net.bluemind.pop3.endpoint.Pop3Driver;

public class MailApiPop3Driver implements Pop3Driver {

	private static final Logger logger = LoggerFactory.getLogger(MailApiPop3Driver.class);
	private HttpClientProvider clientProvider;

	@Override
	public CompletableFuture<MailboxConnection> connect(String login, String password) {
		this.clientProvider = new HttpClientProvider(VertxPlatform.getVertx());

		ILocator cachingLocator = (String service, AsyncHandler<String[]> asyncHandler) -> {
			IServiceTopology topology = Topology.get();
			try {
				String cores = topology.core().value.address();
				String[] resp = new String[] { cores };
				asyncHandler.success(resp);
			} catch (TopologyException e) {
				asyncHandler.failure(e);
				logger.error(e.getMessage());
			}
		};

		IServiceProvider loginProvider = new VertxPromiseServiceProvider(clientProvider, cachingLocator, null);
		IAuthenticationPromise authapi = loginProvider.instance(IAuthenticationPromise.class);
		CompletableFuture<MailboxConnection> coreConnection = new CompletableFuture<>();
		try {
			authapi.login(login, password, "pop3-endpoint").thenAccept(loginResponse -> {
				if (loginResponse.authKey == null) {
					coreConnection.completeExceptionally(new Exception("authkey is empty for login " + login));
				} else {
					IServiceProvider prov = new VertxPromiseServiceProvider(clientProvider, cachingLocator,
							loginResponse.authKey);
					coreConnection.complete(new CoreConnection(prov, loginResponse.authUser));
				}
			}).exceptionally(ex -> {
				logger.error(ex.getMessage());
				coreConnection.completeExceptionally(ex);
				return null;
			});
			return coreConnection;
		} catch (Exception e) {
			logger.error(e.getMessage());
			coreConnection.completeExceptionally(e);
			return coreConnection;
		}
	}

}
