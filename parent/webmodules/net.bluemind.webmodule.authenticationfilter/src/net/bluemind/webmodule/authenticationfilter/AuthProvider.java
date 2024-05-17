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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.Vertx;
import net.bluemind.authentication.api.IAuthenticationPromise;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.common.cache.persistence.CacheBackingStore;
import net.bluemind.config.Token;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.mailbox.api.IMailboxesPromise;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.webmodule.authenticationfilter.internal.ExternalCreds;
import net.bluemind.webmodule.authenticationfilter.internal.SessionData;
import net.bluemind.webmodule.authenticationfilter.internal.SessionsCache;

public class AuthProvider {
	private static final Logger logger = LoggerFactory.getLogger(AuthProvider.class);

	public static final int DEFAULT_MAX_SESSIONS_PER_USER = 5;
	private static final String BM_WEBSERVER_AUTHFILTER = "bm-webserver-authfilter";

	private HttpClientProvider clientProvider;
	private Supplier<Integer> maxSessionsPerUser;

	public AuthProvider(Vertx vertx) {
		clientProvider = new HttpClientProvider(vertx);
		initMaxSessionsSupplier();
	}

	private void initMaxSessionsSupplier() {
		AtomicReference<SharedMap<String, String>> sysconf = new AtomicReference<>();
		MQ.init().thenAccept(v -> sysconf.set(MQ.sharedMap(Shared.MAP_SYSCONF)));

		maxSessionsPerUser = () -> Optional.ofNullable(sysconf.get()).map(sm -> {
			try {
				return Integer.parseInt(sm.get(SysConfKeys.hps_max_sessions_per_user.name()));
			} catch (NumberFormatException nfe) {
				return DEFAULT_MAX_SESSIONS_PER_USER;
			}
		}).orElse(DEFAULT_MAX_SESSIONS_PER_USER);

	}

	public void sessionId(ExternalCreds externalCreds, List<String> remoteIps, AsyncHandler<SessionData> handler) {
		if (Strings.isNullOrEmpty(externalCreds.getLoginAtDomain())
				|| !externalCreds.getLoginAtDomain().contains("@")) {
			handler.failure(new ServerFault(
					"Invalid loginAtDomain " + externalCreds.getLoginAtDomain() + " from external credentials"));
			return;
		}

		if ("admin0@global.virt".equals(externalCreds.getLoginAtDomain())) {
			doSudo(remoteIps, externalCreds, handler);
			return;
		}

		String domainName = externalCreds.getLoginAtDomain().split("@")[1];

		IMailboxesPromise mailboxClient = getProvider(Token.admin0(), remoteIps).instance(IMailboxesPromise.class,
				domainName);

		mailboxClient
				.byName(externalCreds.getLoginAtDomain().substring(0, externalCreds.getLoginAtDomain().indexOf('@')))
				.whenComplete((mailbox, exception) -> {
					if (exception != null) {
						handler.failure(exception);
						return;
					}

					if (mailbox != null && mailbox.value.type == Type.user && !mailbox.value.archived) {
						doSudo(remoteIps, externalCreds, handler);
						return;
					}

					loginAtDomainAsEmail(mailboxClient, remoteIps, externalCreds, domainName, handler);
				});
	}

	public void sessionId(final String loginAtDomain, final String password, List<String> remoteIps,
			final AsyncHandler<SessionData> handler) {
		VertxPromiseServiceProvider sp = getProvider(null, remoteIps);

		logger.info("authenticating {}", loginAtDomain);
		IAuthenticationPromise auth = sp.instance(TagDescriptor.bm_core.getTag(), IAuthenticationPromise.class);
		auth.loginWithParams(loginAtDomain.toLowerCase(), password, BM_WEBSERVER_AUTHFILTER, true).exceptionally(e -> {
			logger.error("error during authentication of {}", loginAtDomain, e);
			handler.failure(new ServerFault("error login: No server assigned or server not avalaible"));
			return null;
		}).thenAccept(lr -> {
			logger.info("Authenticated {}, response: {}", loginAtDomain, lr.status);
			if (lr.status == Status.Ok || lr.status == Status.Expired) {
				handlerLoginSuccess(lr, remoteIps, handler);
			} else {
				handler.failure(new ServerFault("error during login " + lr.message, ErrorCode.INVALID_PASSWORD));
			}
		});
	}

	private void doSudo(List<String> remoteIps, ExternalCreds externalCreds, final AsyncHandler<SessionData> handler) {
		getProvider(Token.admin0(), remoteIps).instance(IAuthenticationPromise.class)
				.suWithParams(externalCreds.getLoginAtDomain(), true).exceptionally(t -> null).thenAccept(lr -> {
					if (lr == null) {
						handler.failure(
								new ServerFault("Error during sudo for user " + externalCreds.getLoginAtDomain()));
						return;
					}

					if (lr.status == Status.Ok) {
						handlerLoginSuccess(lr, remoteIps, handler);
					} else {
						handler.success(null);
					}
				});
	}

	private void loginAtDomainAsEmail(IMailboxesPromise mailboxClient, List<String> remoteIps,
			ExternalCreds externalCreds, String domainName, AsyncHandler<SessionData> handler) {
		mailboxClient.byEmail(externalCreds.getLoginAtDomain()).whenComplete((mailbox, exception) -> {
			if (exception != null) {
				handler.failure(exception);
				return;
			}

			if (mailbox != null) {
				if (mailbox.value.type != Type.user || mailbox.value.archived) {
					handler.success(null);
					return;
				}

				String realLoginAtdomain = mailbox.value.name + "@" + domainName;

				logger.info("Try sudo with login {} (Submitted login {})", realLoginAtdomain,
						externalCreds.getLoginAtDomain());
				externalCreds.setLoginAtDomain(realLoginAtdomain);
			} else {
				logger.info("Try sudo with login {}", externalCreds.getLoginAtDomain());
			}

			doSudo(remoteIps, externalCreds, handler);
		});
	}

	private void handlerLoginSuccess(LoginResponse lr, List<String> remoteIps, AsyncHandler<SessionData> handler) {
		final SessionData sd = new SessionData(lr);

		// when creating a new session for a user, expire the oldest ones if he
		// already has MAX_SESSIONS_PER_USER.
		SessionData[] existingSessionForSameUser;
		CacheBackingStore<SessionData> cache = SessionsCache.get();
		synchronized (cache) {
			existingSessionForSameUser = cache.getCache().asMap().values().stream()
					.filter(existingSession -> existingSession.userUid.equals(sd.userUid))
					.sorted((s1, s2) -> Long.compare(s1.createStamp, s2.createStamp)).toArray(SessionData[]::new);
		}

		int curMax = this.maxSessionsPerUser.get();
		if (existingSessionForSameUser.length >= curMax) {
			logger.warn("Max session (active: {}/{}) exhausted for {}, ips: {}", existingSessionForSameUser.length,
					curMax, sd.loginAtDomain, remoteIps);
			for (int i = 0; i <= existingSessionForSameUser.length - curMax; i++) {
				logout(existingSessionForSameUser[i]);
			}
		}

		synchronized (cache) {
			cache.put(sd.authKey, sd);
		}

		handler.success(sd);
	}

	public CompletableFuture<Void> logout(String sid) {
		SessionData sessionData = null;

		CacheBackingStore<SessionData> cache = SessionsCache.get();
		synchronized (cache) {
			sessionData = cache.getIfPresent(sid);
		}

		if (sessionData != null) {
			return logout(sessionData);
		}

		return logout("Unknown", sid);
	}

	public CompletableFuture<Void> logout(SessionData sessionData) {
		return logout(sessionData.loginAtDomain, sessionData.authKey);
	}

	/**
	 * Logout on core as core HZ message will invalidate webserver session
	 * 
	 * See {@link AuthenticationFilter#setVertx(Vertx)}
	 * 
	 * @param id
	 * @param sessionId
	 * @return
	 */
	private CompletableFuture<Void> logout(String id, String sessionId) {
		logger.info("Log out session {} for {}", sessionId, id);
		return getProvider(sessionId, Collections.emptyList()).instance(IAuthenticationPromise.class).logout()
				.whenComplete((v, fn) -> {
					if (fn != null) {
						logger.error(fn.getMessage(), fn);
					}
				});
	}

	private VertxPromiseServiceProvider getProvider(String apiKey, List<String> remoteIps) {
		ILocator lc = (String service, AsyncHandler<String[]> asyncHandler) -> asyncHandler.success(
				new String[] { Topology.get().anyIfPresent(service).map(s -> s.value.address()).orElse("127.0.0.1") });
		VertxPromiseServiceProvider prov = new VertxPromiseServiceProvider(clientProvider, lc, apiKey, remoteIps);
		prov.setOrigin(BM_WEBSERVER_AUTHFILTER);
		return prov;
	}
}
