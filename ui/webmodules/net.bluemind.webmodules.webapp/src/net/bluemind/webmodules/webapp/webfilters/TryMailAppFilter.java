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
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.VertxServiceProvider;
import net.bluemind.locator.vertxclient.VertxLocatorClient;
import net.bluemind.user.api.IUserSettingsAsync;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.server.handlers.PermanentRedirectHandler;

public class TryMailAppFilter implements IWebFilter, NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(TryMailAppFilter.class);
	
	private HttpClientProvider clientProvider;

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request) {
		CompletableFuture<HttpServerRequest> completableFuture = new CompletableFuture<>();
		String userUid = request.headers().get("BMUserId");
		String domainUid = request.headers().get("BMUserDomainId");
		String roles = request.headers().get("BMRoles");
		boolean hasBothWebmailRoles = roles.contains("hasWebmail") && roles.contains("hasMailWebapp");
		
		if (request.path().equals("/webapp/index.html") && hasBothWebmailRoles) {
			VertxServiceProvider provider = new VertxServiceProvider(clientProvider,
					new VertxLocatorClient(clientProvider, SecurityContext.ANONYMOUS.getSubject()), null).from(request);

			provider.instance("bm/core", IUserSettingsAsync.class, domainUid).getOne(userUid, "mail-application", new AsyncHandler<String>() {
					
				@Override
				public void success(String value) {
					boolean tryNewWebmail = value.equals("mail-webapp");
					
					if (!tryNewWebmail) {
						logger.info("Redirecting /webapp/index.html to /webmail/ for user {} on domain {}", 
								userUid, domainUid);
						new PermanentRedirectHandler("/webmail/").handle(request);
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
