/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.webmodules.webapp.wrapper.handlers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.network.topology.Topology;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.api.IUserSettingsPromise;
import net.bluemind.webmodule.server.NeedVertx;

public class WebAppHandler implements Handler<HttpServerRequest>, NeedVertx {

	public static final String ROLE_MAIL_WEBAPP = "hasMailWebapp";

	private HttpClientProvider clientProvider;

	private static final ILocator locator = (String service, AsyncHandler<String[]> asyncHandler) -> {
		String core = Topology.get().core().value.address();
		String[] resp = new String[] { core };
		asyncHandler.success(resp);
	};

	@Override
	public void setVertx(Vertx vertx) {
		clientProvider = new HttpClientProvider(vertx);
	}

	@Override
	public void handle(HttpServerRequest request) {
		Set<String> roles = request.headers().get("BMRoles") != null ? //
				new HashSet<>(Arrays.asList(request.headers().get("BMRoles").split(","))) //
				: Collections.<String>emptySet();

		if (!roles.contains(BasicRoles.ROLE_WEBMAIL)) {
			redirectToNewBanner(request);
		} else if (!roles.contains(ROLE_MAIL_WEBAPP)) {
			redirectToOldBanner(request);
		} else {
			String userUid = request.headers().get("BMUserId");
			String domainUid = request.headers().get("BMUserDomainId");
			String sessionId = request.headers().get("BMSessionId");

			final VertxPromiseServiceProvider provider = new VertxPromiseServiceProvider(clientProvider, locator,
					sessionId);
			provider.instance(IUserSettingsPromise.class, domainUid).getOne(userUid, "mail-application")
					.thenAccept((mailApplication) -> {
						if (mailApplication.equals("mail-webapp")) {
							redirectToNewBanner(request);
						} else {
							redirectToOldBanner(request);
						}
					});

		}

	}

	private void redirectToOldBanner(HttpServerRequest request) {
		HttpServerResponse response = request.response();
		response.setStatusCode(404);
		response.end();
	}

	private void redirectToNewBanner(HttpServerRequest request) {
		HttpServerResponse response = request.response();
		response.putHeader("location", "../js/net.bluemind.webapp.root.js");
		response.setStatusCode(302);
		response.end();
	}

}
