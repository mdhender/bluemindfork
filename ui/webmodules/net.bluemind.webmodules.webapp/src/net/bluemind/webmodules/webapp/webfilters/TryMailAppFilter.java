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
package net.bluemind.webmodules.webapp.webfilters;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxServiceProvider;
import net.bluemind.network.topology.Topology;
import net.bluemind.user.api.IUserSettingsAsync;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.server.handlers.TemporaryRedirectHandler;

public class TryMailAppFilter implements IWebFilter, NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(TryMailAppFilter.class);

	private HttpClientProvider clientProvider;

	private static final ILocator locator = (String service, AsyncHandler<String[]> asyncHandler) -> {
		String core = Topology.get().core().value.address();
		String[] resp = new String[] { core };
		asyncHandler.success(resp);
	};

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request) {
		CompletableFuture<HttpServerRequest> completableFuture = new CompletableFuture<>();
		String userUid = request.headers().get("BMUserId");
		String domainUid = request.headers().get("BMUserDomainId");
		String roles = request.headers().get("BMRoles");
		String apiKey = request.headers().get("BMSessionId");
		boolean hasBothWebmailRoles = false, hasNewWebmailRole = false;
		if (roles != null) {
			hasNewWebmailRole = roles.contains("hasMailWebapp");
			hasBothWebmailRoles = roles.contains("hasWebmail") && hasNewWebmailRole;
		}

		if ((request.path().equals("/webapp/index.html") || request.path().startsWith("/webapp/mail"))
				&& !hasNewWebmailRole) {
			new TemporaryRedirectHandler("/webmail/").handle(request);
			completableFuture.complete(null);
		} else if (request.path().equals("/webapp/index.html") && hasBothWebmailRoles) {
			VertxServiceProvider provider = new VertxServiceProvider(clientProvider, locator, apiKey).from(request);

			provider.instance("bm/core", IUserSettingsAsync.class, domainUid).getOne(userUid, "mail-application",
					new AsyncHandler<String>() {

						@Override
						public void success(String value) {
							boolean tryNewWebmail = value.equals("mail-webapp");

							if (!tryNewWebmail) {
								logger.info("Redirecting /webapp/index.html to /webmail/ for user {} on domain {}",
										userUid, domainUid);
								new TemporaryRedirectHandler("/webmail/").handle(request);
								completableFuture.complete(null);
							} else {
								completableFuture.complete(request);
							}
						}

						@Override
						public void failure(Throwable e) {
							completableFuture.complete(request);
						}
					});
		} else {
			completableFuture.complete(request);
		}
		return completableFuture;
	}

	@Override
	public void setVertx(Vertx vertx) {
		this.clientProvider = new HttpClientProvider(vertx);
	}
}
