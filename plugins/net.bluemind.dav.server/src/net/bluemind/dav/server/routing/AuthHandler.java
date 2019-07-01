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
package net.bluemind.dav.server.routing;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.vertx.common.CoreSession;
import net.bluemind.vertx.common.LoginHandler;

public class AuthHandler implements Handler<HttpServerRequest> {

	private static final Cache<Creds, LoggedCore> loggedCoreCache = CacheBuilder.newBuilder()
			.expireAfterAccess(10, TimeUnit.MINUTES).build();
	private static final String wwwAuth = "basic realm=\"bm.cal.dav\"";
	private IPostAuthHandler success;
	private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);

	@Override
	public void handle(final HttpServerRequest r) {
		String auth = r.headers().get("Authorization");

		if (auth == null) {
			logger.debug("Missing Auth header => 401");
			r.response().putHeader("WWW-Authenticate", wwwAuth).setStatusCode(401).end();
		} else {
			r.pause();
			Creds creds = getCredentials(auth);
			validate(creds, new Handler<LoggedCore>() {

				@Override
				public void handle(LoggedCore lc) {
					if (lc != null) {
						success.handle(lc, r);
						MDC.put("user", "anonymous");
						r.resume();
					} else {
						logger.error("Creds did not validate");
						r.resume();
						r.response().setStatusCode(403).end();
					}
				}
			});
		}
	}

	private void validate(Creds creds, final Handler<LoggedCore> handler) {
		if (creds == null) {
			handler.handle(null);
			return;
		}

		LoggedCore core = loggedCoreCache.getIfPresent(creds);
		if (core != null) {
			handler.handle(core);
			return;
		}

		CoreSession.attemptWithRole(creds.getLogin(), creds.getPassword(), "bm-dav", BasicRoles.ROLE_DAV,
				new LoginHandler() {

					@Override
					public void ok(String coreUrl, String sid, String principal) {
						IServiceProvider core = ClientSideServiceProvider.getProvider(coreUrl, sid);
						LoggedCore lc = new LoggedCore(core);
						logger.info("cache {}", creds);
						loggedCoreCache.put(creds, lc);
						handler.handle(lc);
					}

					@Override
					public void ko() {
						handler.handle(null);
					}
				});

	}

	private Creds getCredentials(String auth) {
		if (!auth.startsWith("Basic ")) {
			return null;
		}
		String sub = auth.substring(6);
		byte[] dec = Base64.getDecoder().decode(sub);
		String decStr = new String(dec);
		int idx = decStr.indexOf(':');
		String login = decStr.substring(0, idx);
		String pass = decStr.substring(idx + 1);
		if (!login.contains("@")) {
			logger.warn("Missing domainpart in login '{}'", login);
			return null;
		}
		logger.debug("[{}] creds: {}", Thread.currentThread().getName(), login);
		MDC.put("user", login.replace("@", "_at_"));
		return new Creds(login, pass);
	}

	public AuthHandler successHandler(IPostAuthHandler postAuthHandler) {
		this.success = postAuthHandler;
		return this;
	}

	public static void invalidateAll() {
		loggedCoreCache.invalidateAll();
	}

}
