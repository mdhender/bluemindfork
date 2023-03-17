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
package net.bluemind.webmodule.openidhandler;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.authentication.api.AccessTokenInfo;
import net.bluemind.authentication.api.IUserAccessTokenAsync;
import net.bluemind.config.Token;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.ITaggedServiceProvider;
import net.bluemind.core.rest.http.VertxServiceProvider;
import net.bluemind.network.topology.Topology;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.server.WebserverConfiguration;

public class OpenIdAuthCodeHandler implements IWebFilter, NeedVertx {
	Logger logger = LoggerFactory.getLogger(OpenIdAuthCodeHandler.class);
	private HttpClientProvider clientProvider;

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request, WebserverConfiguration conf) {
		final HttpServerResponse resp = request.response();

		String path = request.path();
		if (path.contains("bm-openid/auth")) {

			if (request.getParam("error") != null) {
				resp.setStatusCode(500).end("RET: " + request.query());
			}

			String state = request.getParam("state");
			String code = request.getParam("code");
			logger.info("Handling OpenId authCode reception for state {}", state);

			getService(request).authCodeReceived(state, code, new AsyncHandler<AccessTokenInfo>() {

				@Override
				public void success(AccessTokenInfo info) {
					resp.headers().set("Content-Type", "text/html");
					resp.setStatusCode(200).end(
							"<html><body><script type='text/javascript'>window.opener.bmOpenIdAuthicationCallback.resolve();window.close();</script>Authentication OK, You can close this window</body></html>");
				}

				@Override
				public void failure(Throwable e) {
					
					resp.headers().set("Content-Type", "text/html");
					resp.setStatusCode(500).end(
							"<html><body><script type='text/javascript'>window.opener.bmOpenIdAuthicationCallback.reject();window.close();</script>Authentication failed, You can close this window</body></html>");

				}
			});

			return CompletableFuture.completedFuture(null);
		}

		return CompletableFuture.completedFuture(request);
	}

	protected IUserAccessTokenAsync getService(HttpServerRequest request) {
		ITaggedServiceProvider sp = getProvider(Token.admin0(), request);
		return sp.instance("bm/core", IUserAccessTokenAsync.class);
	}

	private static final ILocator locator = (String service, AsyncHandler<String[]> asyncHandler) -> {
		String core = Topology.get().core().value.address();
		String[] resp = new String[] { core };
		asyncHandler.success(resp);
	};

	private ITaggedServiceProvider getProvider(String apiKey, HttpServerRequest request) {
		return new VertxServiceProvider(clientProvider, locator, apiKey).from(request);
	}

	@Override
	public void setVertx(Vertx vertx) {
		clientProvider = new HttpClientProvider(vertx);
	}

}
