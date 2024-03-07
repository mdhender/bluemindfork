/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.webmodule.authenticationfilter;

import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.webmodule.authenticationfilter.internal.AuthenticationCookie;
import net.bluemind.webmodule.authenticationfilter.internal.ExternalCreds;
import net.bluemind.webmodule.authenticationfilter.internal.SessionData;
import net.bluemind.webmodule.server.NeedVertx;

public abstract class AbstractAuthHandler implements NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(AbstractAuthHandler.class);

	protected Vertx vertx;
	protected HttpClient httpClient;

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
		this.httpClient = vertx.createHttpClient(new HttpClientOptions().setTrustAll(true).setVerifyHost(false));
	}

	protected void createSession(HttpServerRequest request, AuthProvider prov, List<String> forwadedFor,
			ExternalCreds creds, String redirectTo) {
		createSession(request, prov, forwadedFor, creds, redirectTo, sessionData -> AuthenticationCookie
				.add(request.response().headers(), AuthenticationCookie.BMSID, sessionData.authKey));
	}

	protected void createSession(HttpServerRequest request, AuthProvider prov, List<String> forwadedFor,
			ExternalCreds creds, String redirectTo, Consumer<SessionData> handlerSessionConsumer) {
		prov.sessionId(creds, forwadedFor, new AsyncHandler<SessionData>() {
			@Override
			public void success(SessionData sessionData) {
				MultiMap headers = request.response().headers();

				if (sessionData == null) {
					logger.error("Error during auth, {} login not valid (not found/archived or not user)",
							creds.getLoginAtDomain());
					headers.add(HttpHeaders.LOCATION,
							"/errors-pages/deniedAccess.html?login=" + creds.getLoginAtDomain());
					request.response().setStatusCode(302);
					request.response().end();
					return;
				}

				logger.info("[{}] Session {} for user {} created", request.path(), sessionData.authKey,
						creds.getLoginAtDomain());

				handlerSessionConsumer.accept(sessionData);

				if ("admin0@global.virt".equals(creds.getLoginAtDomain())) {
					headers.add(HttpHeaders.LOCATION, "/");
				} else {
					headers.add(HttpHeaders.LOCATION, redirectTo);
				}

				request.response().setStatusCode(302);
				request.response().end();
			}

			@Override
			public void failure(Throwable e) {
				error(request, e);
			}
		});
	}

	protected void error(HttpServerRequest req, Throwable e) {
		logger.error(e.getMessage(), e);
		AuthenticationCookie.purge(req);
		req.response().headers().add(HttpHeaders.LOCATION, "/");
		req.response().setStatusCode(302);
		req.response().end();
	}
}