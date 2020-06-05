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

package net.bluemind.vertx.common.http;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.authentication.api.IAuthenticationPromise;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.config.Token;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.mailbox.api.IMailboxesPromise;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.network.topology.TopologyException;

public class BasicAuthHandler implements Handler<HttpServerRequest> {
	public final Handler<AuthenticatedRequest> lh;
	private final String origin;
	private final String role;
	private IAuthenticationPromise authApi;
	private VertxPromiseServiceProvider adminProv;
	private static final Logger logger = LoggerFactory.getLogger(BasicAuthHandler.class);

	private static class ValidatedAuth {
		public ValidatedAuth(String login, String sid, Routing r) {
			this.login = login;
			this.sid = sid;
			this.routing = r;
		}

		String login;
		String sid;
		Routing routing;

	}

	private static Cache<String, ValidatedAuth> validated = CacheBuilder.newBuilder()
			.recordStats()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(BasicAuthHandler.class, validated);
		}
	}

	public static final class AuthenticatedRequest {

		public final HttpServerRequest req;
		public final String login;
		public final String sid;
		public final Routing routing;

		public AuthenticatedRequest(HttpServerRequest r, String l, String s, Routing routing) {
			this.req = r;
			this.login = l;
			this.sid = s;
			this.routing = routing;
		}
	}

	public BasicAuthHandler(Vertx vertx, String origin, Handler<AuthenticatedRequest> lh) {
		this(vertx, origin, null, lh);
	}

	public BasicAuthHandler(Vertx vertx, String origin, String role, Handler<AuthenticatedRequest> lh) {
		this.lh = lh;
		this.origin = origin;
		this.role = role;
		ILocator topoLocator = (String service, AsyncHandler<String[]> asyncHandler) -> {
			Optional<IServiceTopology> topo = Topology.getIfAvailable();
			if (topo.isPresent()) {
				String core = topo.get().core().value.address();
				String[] resp = new String[] { core };
				asyncHandler.success(resp);
			} else {
				asyncHandler.failure(new TopologyException("topology not available"));
			}
		};
		VertxPromiseServiceProvider prov = new VertxPromiseServiceProvider(new HttpClientProvider(vertx), topoLocator,
				null);
		this.authApi = prov.instance(IAuthenticationPromise.class);
		adminProv = new VertxPromiseServiceProvider(new HttpClientProvider(vertx), topoLocator, Token.admin0());
	}

	private static final class Creds {

		public Creds(String login, String password) {
			this.login = login;
			this.password = password;
		}

		private final String login;
		private final String password;

		public String getLogin() {
			return login;
		}

		public String getPassword() {
			return password;
		}

	}

	private static class WithRouting {
		LoginResponse lr;
		Mailbox.Routing mboxRouting;

		public WithRouting(LoginResponse lr, Routing r) {
			this.lr = lr;
			this.mboxRouting = r;
		}
	}

	private boolean roleCheck(String role, LoginResponse lr) {
		return role == null || lr.authUser.roles.contains(SecurityContext.ROLE_SYSTEM)
				|| lr.authUser.roles.contains(role);
	}

	@Override
	public void handle(final HttpServerRequest r) {
		MultiMap headers = r.headers();
		final String auth = headers.get("Authorization");

		if (auth == null) {
			logger.debug("Missing Auth header => 401");
			r.response().putHeader("WWW-Authenticate", "Basic realm=\"bm.basic.auth\"").setStatusCode(401).end();
		} else {
			ValidatedAuth cached = validated.getIfPresent(auth);
			if (cached != null) {
				lh.handle(new AuthenticatedRequest(r, cached.login, cached.sid, cached.routing));
				return;
			}
			final Creds creds = getCredentials(auth, StandardCharsets.UTF_8);
			if (creds == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("401 for auth header '{}', cookies: {}", auth, headers.get("Cookie"));
				}
				r.response().putHeader("WWW-Authenticate", "Basic realm=\"bm.basic.auth\"").setStatusCode(401).end();
				return;
			}
			r.pause();

			CompletableFuture<WithRouting> loginResp = authApi.login(creds.getLogin(), creds.getPassword(), origin)
					.thenCompose(lr -> {
						if (lr.status == Status.Ok) {
							// check role then
							return CompletableFuture.completedFuture(roleCheck(role, lr) ? lr : null);
						} else {
							Creds other = getCredentials(auth, StandardCharsets.ISO_8859_1);
							return authApi.login(other.getLogin(), other.getPassword(), origin)
									.thenApply(altLr -> roleCheck(role, altLr) ? altLr : null);
						}
					}).thenCompose(lrOrNull -> {
						if (lrOrNull == null) {
							return CompletableFuture.completedFuture(null);
						} else {
							IMailboxesPromise mboxesApi = adminProv.instance(IMailboxesPromise.class,
									lrOrNull.authUser.domainUid);
							return mboxesApi.byName(lrOrNull.authUser.value.login)
									.thenApply(mbox -> new WithRouting(lrOrNull, mbox.value.routing));
						}
					});

			loginResp.whenComplete((loginRespAndRouting, ex) -> {
				r.resume();
				if (ex != null) {
					logger.warn("{}", ex.getMessage());
					r.response().putHeader("WWW-Authenticate", "Basic realm=\"bm.basic.auth\"").setStatusCode(401)
							.end();
				} else if (loginRespAndRouting == null) {
					r.response().putHeader("WWW-Authenticate", "Basic realm=\"bm.basic.auth\"").setStatusCode(401)
							.end();
				} else {
					ValidatedAuth va = new ValidatedAuth(loginRespAndRouting.lr.latd, loginRespAndRouting.lr.authKey,
							loginRespAndRouting.mboxRouting);
					validated.put(auth, va);
					lh.handle(new AuthenticatedRequest(r, loginRespAndRouting.lr.latd, loginRespAndRouting.lr.authKey,
							loginRespAndRouting.mboxRouting));
				}
			});
		}
	}

	private Creds getCredentials(String auth, Charset charset) {
		if (!auth.startsWith("Basic ")) {
			return null;
		}
		String sub = auth.substring(6);
		String decStr = new String(java.util.Base64.getDecoder().decode(sub), charset);

		int idx = decStr.indexOf(':');
		String login = decStr.substring(0, idx);
		String pass = decStr.substring(idx + 1);

		// support windows style login: Domain.com\User
		int backslash = login.indexOf('\\');
		if (backslash > 0) {
			String tmpLogin = login.substring(backslash + 1);
			if (!tmpLogin.contains("@")) {
				tmpLogin = login.substring(backslash + 1) + "@" + login.substring(0, backslash);
			}
			login = tmpLogin;
		}

		if (!login.contains("@")) {
			logger.warn("Missing domainpart in login '{}'", login);
			return null;
		}

		login = login.toLowerCase();

		logger.info("creds: {}", login);
		return new Creds(login, pass);
	}

	public static void purgeSessions() {
		validated.invalidateAll();
	}

	public static String getSid(String auth) {
		return validated.getIfPresent(auth).sid;
	}

}
