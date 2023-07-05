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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
import net.bluemind.webmodule.server.WebserverConfiguration;
import net.bluemind.webmodule.server.handlers.TemporaryRedirectHandler;

public class TryMailAppFilter implements IWebFilter, NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(TryMailAppFilter.class);
	public static final String ROLE_MAIL_WEBAPP = "hasMailWebapp";
	public static final String ROLE_WEBMAIL = "hasWebmail";

	private HttpClientProvider clientProvider;

	private static final ILocator locator = (String service, AsyncHandler<String[]> asyncHandler) -> {
		String core = Topology.get().core().value.address();
		String[] resp = new String[] { core };
		asyncHandler.success(resp);
	};

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request, WebserverConfiguration conf) {
		CompletableFuture<HttpServerRequest> completableFuture = new CompletableFuture<>();

		if (matchFilter(request)) {
			Set<String> roles = request.headers().get("BMRoles") != null ? //
					new HashSet<>(Arrays.asList(request.headers().get("BMRoles").split(","))) //
					: Collections.<String>emptySet();

			if (!roles.contains(ROLE_WEBMAIL)) {
				completableFuture.complete(request);
			} else if (!roles.contains(ROLE_MAIL_WEBAPP)) {
				redirectToWebmail(request);
				completableFuture.complete(null);
			} else {
				String userUid = request.headers().get("BMUserId");
				String domainUid = request.headers().get("BMUserDomainId");
				String apiKey = request.headers().get("BMSessionId");
				VertxServiceProvider provider = new VertxServiceProvider(clientProvider, locator, apiKey).from(request);
				provider.instance("bm/core", IUserSettingsAsync.class, domainUid).getOne(userUid, "mail-application",
						new AsyncHandler<String>() {

							@Override
							public void success(String mailApplication) {
								if (!mailApplication.equals("mail-webapp")) {
									redirectToWebmail(request);
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
			}
		} else {
			completableFuture.complete(request);
		}

		return completableFuture;
	}

	private boolean matchFilter(HttpServerRequest request) {
		return request.path().equals("/webapp/index.html") || request.path().startsWith("/webapp/mail");
	}

	@Override
	public void setVertx(Vertx vertx) {
		this.clientProvider = new HttpClientProvider(vertx);
	}

	private void redirectToWebmail(HttpServerRequest request) {
		String userUid = request.headers().get("BMUserId");
		String domainUid = request.headers().get("BMUserDomainId");
		logger.info("Redirecting /webapp/index.html to /webmail/ for user {} on domain {}", userUid, domainUid);
		new TemporaryRedirectHandler("/webmail/").handle(request);
	}

}
