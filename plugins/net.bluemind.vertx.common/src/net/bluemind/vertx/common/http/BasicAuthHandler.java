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

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;

import io.netty.util.concurrent.FastThreadLocal;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.authentication.api.IAuthenticationPromise;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.config.Token;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxesPromise;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.network.topology.TopologyException;
import net.bluemind.system.api.SystemState;
import net.bluemind.vertx.common.CoreStateListener;

public class BasicAuthHandler implements Handler<HttpServerRequest> {
	public final Handler<AuthenticatedRequest> lh;
	private final String origin;
	private final String role;
	private IAuthenticationPromise authApi;
	private VertxPromiseServiceProvider adminProv;
	private static final Logger logger = LoggerFactory.getLogger(BasicAuthHandler.class);

	/**
	 * https://docs.microsoft.com/en-us/azure/active-directory/authentication/concept-sspr-policy#password-policies-that-only-apply-to-cloud-user-accounts
	 */
	private static final CharMatcher azureAdMatcher = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z'))
			.or(CharMatcher.inRange('0', '9')).or(CharMatcher.anyOf(" @#$%^&*-_!+=[]{}|\\:',.?/`~\"();<>"));

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

	private static Cache<String, ValidatedAuth> validated = Caffeine.newBuilder().recordStats()
			.expireAfterWrite(10, TimeUnit.MINUTES).build();

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

	@VisibleForTesting
	public static final class Creds {

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

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(Creds.class).add("l", login).add("p", password).toString();
		}

	}

	private static class WithRouting {
		LoginResponse lr;
		Mailbox.Routing mboxRouting;

		public WithRouting(LoginResponse lr, ItemValue<Mailbox> mbox) {
			this.lr = lr;
			this.mboxRouting = mbox != null ? mbox.value.routing : Routing.none;
		}
	}

	private boolean roleCheck(String role, LoginResponse lr) {
		return role == null || lr.authUser.roles.contains(SecurityContext.ROLE_SYSTEM)
				|| lr.authUser.roles.contains(role);
	}

	private static final CharSequence WWW_AUTHENTICATE = HttpHeaders.createOptimized("WWW-Authenticate");

	private static final CharSequence WWW_AUTHENTICATE_VALUE = HttpHeaders
			.createOptimized("Basic realm=\"bm.basic.auth.v2\", charset=\"UTF-8\"");

	@Override
	public void handle(final HttpServerRequest r) {

		if (CoreStateListener.state != SystemState.CORE_STATE_RUNNING) {
			VertxPlatform.getVertx().setTimer(500, timerId -> {
				logger.warn("Core state is {}", CoreStateListener.state);
				r.response().setStatusCode(503).setStatusMessage("Service Unavailable").end();
			});
			return;
		}

		MultiMap headers = r.headers();
		final String auth = headers.get(HttpHeaders.AUTHORIZATION);

		if (auth == null) {
			logger.debug("Missing Auth header => 401");
			r.response().putHeader(WWW_AUTHENTICATE, WWW_AUTHENTICATE_VALUE).setStatusCode(401).end();
		} else {
			ValidatedAuth cached = validated.getIfPresent(auth);
			if (cached != null) {
				lh.handle(new AuthenticatedRequest(r, cached.login, cached.sid, cached.routing));
				return;
			}
			final Creds creds = getCredentials(auth);
			if (creds == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("401 for auth header '{}', cookies: {}", auth, headers.get("Cookie"));
				}
				r.response().putHeader(WWW_AUTHENTICATE, WWW_AUTHENTICATE_VALUE).setStatusCode(401).end();
				return;
			}
			r.pause();

			CompletableFuture<WithRouting> loginResp = authApi.login(creds.getLogin(), creds.getPassword(), origin)
					.thenApply(lr -> {
						if (lr.status == Status.Ok) {
							// check role then
							return roleCheck(role, lr) ? lr : null;
						} else {
							return null;
						}
					}).thenCompose(lrOrNull -> {
						if (lrOrNull == null) {
							return CompletableFuture.completedFuture(null);
						} else {

							IMailboxesPromise mboxesApi = adminProv.instance(IMailboxesPromise.class,
									lrOrNull.authUser.domainUid);
							return mboxesApi.byName(lrOrNull.authUser.value.login)
									.thenApply(mbox -> new WithRouting(lrOrNull, mbox));
						}
					});

			loginResp.whenComplete((loginRespAndRouting, ex) -> {
				r.resume();
				if (ex != null) {
					logger.warn("auth problem, check core.log ({})", ex.getMessage());
					r.response().putHeader(WWW_AUTHENTICATE, WWW_AUTHENTICATE_VALUE).setStatusCode(401).end();
				} else if (loginRespAndRouting == null) {
					r.response().putHeader(WWW_AUTHENTICATE, WWW_AUTHENTICATE_VALUE).setStatusCode(401).end();
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

	@VisibleForTesting
	public Creds getCredentials(String auth) {
		if (!auth.startsWith("Basic ")) {
			return null;
		}
		byte[] chars = Base64.getDecoder().decode(auth.substring(6));
		int idx = 0;
		for (; idx < chars.length && chars[idx] != ':'; idx++)
			;
		int pwdLen = chars.length - (idx + 1);
		if (pwdLen <= 0) {
			logger.warn("Can't extract password bytes from {}", auth);
			return null;
		}
		byte[] tgt = new byte[pwdLen];
		System.arraycopy(chars, idx + 1, tgt, 0, pwdLen);

		String login = new String(chars, 0, idx);
		Charset guessedEncoding = guessEncoding(login, tgt);

		String pass = new String(tgt, guessedEncoding);

		if (!azureAdMatcher.matchesAllOf(pass) && logger.isWarnEnabled()) {
			logger.warn("[{}] Password contains error-prone characters ({})", login, azureAdMatcher.removeFrom(pass));
		}

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

	private static final FastThreadLocal<CharsetDecoder> localUtf8 = new FastThreadLocal<>();
	private static final FastThreadLocal<CharsetDecoder> localIso = new FastThreadLocal<>();

	private Charset guessEncoding(String login, byte[] tgt) {
		CharsetDecoder dec;

		dec = decoder(StandardCharsets.UTF_8, localUtf8);
		if (checkDec(tgt, dec)) {
			return StandardCharsets.UTF_8;
		}

		dec = decoder(StandardCharsets.ISO_8859_1, localIso);
		if (checkDec(tgt, dec)) {
			return StandardCharsets.ISO_8859_1;
		}

		logger.warn("[{}] password bytes are not compatible with utf-8 nor iso-8859-1", login);
		return StandardCharsets.UTF_8;
	}

	private boolean checkDec(byte[] tgt, CharsetDecoder dec) {
		try {
			dec.decode(ByteBuffer.wrap(tgt));
			return true;
		} catch (CharacterCodingException e) {
			return false;
		}
	}

	private CharsetDecoder decoder(Charset cs, FastThreadLocal<CharsetDecoder> local) {
		CharsetDecoder dec = local.get();
		if (dec == null) {
			dec = cs.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);
			local.set(dec);
		}
		return dec;
	}

	public static void purgeSessions() {
		validated.invalidateAll();
	}

	public static String getSid(String auth) {
		return validated.getIfPresent(auth).sid;
	}

}
