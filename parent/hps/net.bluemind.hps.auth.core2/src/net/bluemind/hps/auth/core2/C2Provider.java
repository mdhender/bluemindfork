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
package net.bluemind.hps.auth.core2;

import java.net.ConnectException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.Vertx;
import net.bluemind.authentication.api.IAuthenticationPromise;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.common.cache.persistence.CacheBackingStore;
import net.bluemind.config.Token;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.BMVersion;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.mailbox.api.IMailboxesPromise;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.network.topology.Topology;
import net.bluemind.proxy.http.ExternalCreds;
import net.bluemind.proxy.http.IAuthProvider;
import net.bluemind.proxy.http.IDecorableRequest;
import net.bluemind.proxy.http.InvalidSession;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.user.api.ChangePassword;
import net.bluemind.user.api.IUserPromise;

public class C2Provider implements IAuthProvider {
	public static final int DEFAULT_MAX_SESSIONS_PER_USER = 5;

	private static final Logger logger = LoggerFactory.getLogger(C2Provider.class);
	private final CacheBackingStore<SessionData> sessions;
	private HttpClientProvider clientProvider;
	private Supplier<Integer> maxSessionsPerUser;

	public C2Provider(Vertx vertx, CacheBackingStore<SessionData> sessions) {
		this.sessions = sessions;
		clientProvider = new HttpClientProvider(vertx);

		initMaxSessionsSupplier();
	}

	private void initMaxSessionsSupplier() {
		AtomicReference<SharedMap<String, String>> sysconf = new AtomicReference<>();
		MQ.init().thenAccept(v -> sysconf.set(MQ.sharedMap("system.configuration")));

		maxSessionsPerUser = () -> Optional.ofNullable(sysconf.get()).map(sm -> {
			try {
				return Integer.parseInt(sm.get(SysConfKeys.hps_max_sessions_per_user.name()));
			} catch (NumberFormatException nfe) {
				return DEFAULT_MAX_SESSIONS_PER_USER;
			}
		}).orElse(DEFAULT_MAX_SESSIONS_PER_USER);
	}

	@Override
	public void sessionId(final String loginAtDomain, final String password, boolean privateComputer,
			List<String> remoteIps, final AsyncHandler<String> handler) {
		VertxPromiseServiceProvider sp = getProvider(null, remoteIps);

		logger.info("authenticating {}", loginAtDomain);
		IAuthenticationPromise auth = sp.instance(TagDescriptor.bm_core.getTag(), IAuthenticationPromise.class);
		auth.loginWithParams(loginAtDomain, password, "bm-hps", true).exceptionally(e -> {
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

	private void handlerLoginSuccess(LoginResponse lr, List<String> remoteIps, AsyncHandler<String> handler) {
		final SessionData sd = new SessionData(lr.authUser.value);

		sd.authKey = lr.authKey;
		sd.passwordStatus = lr.status;
		sd.userUid = lr.authUser.uid;
		sd.loginAtDomain = lr.latd;
		sd.domainUid = lr.authUser.domainUid;
		sd.setRole(lr.authUser.roles);
		sd.settings = lr.authUser.settings;

		// when creating a new session for a user, expire the oldest ones if he
		// already has MAX_SESSIONS_PER_USER.
		SessionData[] existingSessionForSameUser = sessions.getCache().asMap().values().stream()
				.filter(existingSession -> existingSession.userUid.equals(sd.userUid))
				.sorted((s1, s2) -> Long.compare(s1.createStamp, s2.createStamp)).toArray(SessionData[]::new);

		int curMax = this.maxSessionsPerUser.get();
		if (existingSessionForSameUser.length >= curMax) {
			logger.warn("Max session (active: {}/{}) exhausted for {}, ips: {}", existingSessionForSameUser.length,
					curMax, sd.loginAtDomain, remoteIps);
			for (int i = 0; i <= existingSessionForSameUser.length - curMax; i++) {
				logout(existingSessionForSameUser[i].authKey);
			}
		}

		sessions.getCache().put(sd.authKey, sd);
		handler.success(sd.authKey);
	}

	@Override
	public void sessionId(ExternalCreds externalCreds, List<String> remoteIps, AsyncHandler<String> handler) {
		if (Strings.isNullOrEmpty(externalCreds.getLoginAtDomain())
				|| !externalCreds.getLoginAtDomain().contains("@")) {
			handler.failure(new ServerFault(String.format("Invalid loginAtDomain %s from external credentials",
					externalCreds.getLoginAtDomain())));
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
						doSudo(remoteIps, handler, externalCreds);
						return;
					}

					loginAtDomainAsEmail(mailboxClient, remoteIps, externalCreds, handler, domainName);
				});
	}

	/**
	 * Search mailbox using external credential loginAtDomain as email
	 * 
	 * @param mailboxClient
	 * @param remoteIps
	 * @param externalCreds
	 * @param handler
	 * @param domainName
	 */
	private void loginAtDomainAsEmail(IMailboxesPromise mailboxClient, List<String> remoteIps,
			ExternalCreds externalCreds, AsyncHandler<String> handler, String domainName) {
		mailboxClient.byEmail(externalCreds.getLoginAtDomain()).whenComplete((mailbox, exception) -> {
			if (exception != null) {
				handler.failure(exception);
				return;
			}

			if (mailbox == null || mailbox.value.type != Type.user || mailbox.value.archived) {
				handler.success(null);
				return;
			}

			String realLoginAtdomain = String.format("%s@%s", mailbox.value.name, domainName);

			logger.info("Try sudo with login {} (Submitted login {})", realLoginAtdomain,
					externalCreds.getLoginAtDomain());
			externalCreds.setLoginAtDomain(realLoginAtdomain);
			doSudo(remoteIps, handler, externalCreds);
		});
	}

	/**
	 * Do sudo using
	 * {@link net.bluemind.proxy.http.ExternalCreds#getLoginAtDomain()} as login
	 * 
	 * @param checkLatdOnBadAuth if true and sudo login response is bad, check if
	 *                           {@link net.bluemind.proxy.http.ExternalCreds#getLoginAtDomain()}
	 *                           is the real loginAtDomain
	 * @param remoteIps
	 * @param handler
	 * @param sp
	 * @param externalCreds
	 */
	private void doSudo(List<String> remoteIps, AsyncHandler<String> handler, ExternalCreds externalCreds) {
		logger.info("[{}] sessionId (EXT)", externalCreds.getLoginAtDomain());

		getProvider(Token.admin0(), remoteIps).instance(IAuthenticationPromise.class)
				.suWithParams(externalCreds.getLoginAtDomain(), true).exceptionally(t -> null).thenAccept(lr -> {
					if (lr == null) {
						handler.failure(new ServerFault(
								String.format("Error during sudo for user %s", externalCreds.getLoginAtDomain())));
						return;
					}

					if (lr.status == Status.Ok) {
						handlerLoginSuccess(lr, remoteIps, handler);
					} else {
						handler.success(null);
					}
				});
	}

	@Override
	public void decorate(String sessionId, IDecorableRequest proxyReq) {
		if (sessionId == null) {
			logger.error("null session");
			throw new InvalidSession("null session");
		}
		SessionData sd = sessions.getIfPresent(sessionId);
		if (sd == null) {
			logger.error("session {} doesnt exists", sessionId);
			throw new InvalidSession(String.format("session %s doesnt exists", sessionId));
		}

		logger.debug("[{}] decorate from {}", sessionId, sd);
		proxyReq.addHeader("BMSessionId", sd.authKey);
		// TODO add the other stuff ...

		proxyReq.addHeader("BMUserId", "" + sd.getUserUid());

		proxyReq.addHeader("BMUserLogin", sd.login);
		proxyReq.addHeader("BMAccountType", sd.accountType);
		proxyReq.addHeader("BMUserLATD", sd.loginAtDomain);
		addIfPresent(proxyReq, sd.defaultEmail, "BMUserDefaultEmail");

		proxyReq.addHeader("BMUserDomainId", sd.domainUid);

		addIfPresent(proxyReq, sd.givenNames, "BMUserFirstName");
		addIfPresent(proxyReq, sd.familyNames, "BMUserLastName");
		addIfPresent(proxyReq, sd.formatedName, sd.login, "BMUserFormatedName");

		proxyReq.addHeader("BMRoles", sd.rolesAsString);

		proxyReq.addHeader("BMUserMailPerms", "true");

		// needed by rouncube ?

		proxyReq.addHeader("bmMailPerms", "true");
		Map<String, String> settings = sd.getSettings();
		String lang = settings.get("lang");
		proxyReq.addHeader("BMLang", lang == null ? "en" : lang);
		String defaultApp = settings.get("default_app");
		if (sd.loginAtDomain.equals("admin0@global.virt")) {
			defaultApp = "/adminconsole/";
		}
		proxyReq.addHeader("BMDefaultApp", defaultApp != null ? defaultApp : "/webmail/");
		proxyReq.addHeader("BMPrivateComputer", "" + sd.isPrivateComputer());

		// FIXME
		proxyReq.addHeader("BMHasIM", "true");
		proxyReq.addHeader("BMVersion", BMVersion.getVersion());
		proxyReq.addHeader("BMBrandVersion", BMVersion.getVersionName());

		if (sd.dataLocation != null) {
			proxyReq.addHeader("BMDataLocation", sd.dataLocation);
			proxyReq.addHeader("BMPartition", CyrusPartition.forServerAndDomain(sd.dataLocation, sd.domainUid).name);
		}

	}

	private void addIfPresent(IDecorableRequest proxyReq, String value, String fallback, String headerKey) {
		if (!addIfPresent(proxyReq, value, headerKey)) {
			addIfPresent(proxyReq, fallback, headerKey);
		}
	}

	private boolean addIfPresent(IDecorableRequest proxyReq, String value, String headerKey) {
		if (value != null) {
			proxyReq.addHeader(headerKey, java.util.Base64.getEncoder().encodeToString(value.getBytes()));
			return true;
		} else {
			return false;
		}
	}

	@Override
	public CompletableFuture<Boolean> ping(String sessionId) {
		final SessionData sess = sessions.getIfPresent(sessionId);
		if (sess == null) {
			logger.error("error during ping session for SID {} not found", sessionId);
			return CompletableFuture.completedFuture(null).handle((v, t) -> Boolean.FALSE);
		}
		String apiKey = sess.authKey;

		VertxPromiseServiceProvider sp = getProvider(apiKey, Collections.emptyList());
		return sp.instance(IAuthenticationPromise.class).ping().handle((v, t) -> {
			if (t instanceof ConnectException) {
				throw new CompletionException(t);
			}

			if (t != null) {
				logger.error("error during ping for {}:{}", sess.loginAtDomain, sess.authKey, t);
				return Boolean.FALSE;
			}

			logger.debug("ping ok for {}:{}", sess.loginAtDomain, sess.authKey);
			return Boolean.TRUE;
		});
	}

	@Override
	public void reload(String sessionId) {
		logger.debug("[{}] reload", sessionId);

	}

	private VertxPromiseServiceProvider getProvider(String apiKey, List<String> remoteIps) {
		ILocator lc = (String service, AsyncHandler<String[]> asyncHandler) -> asyncHandler.success(
				new String[] { Topology.get().anyIfPresent(service).map(s -> s.value.address()).orElse("127.0.0.1") });
		return new VertxPromiseServiceProvider(clientProvider, lc, apiKey, remoteIps);

	}

	@Override
	public boolean inRole(String sessionId, String role) {
		SessionData session = sessions.getIfPresent(sessionId);
		if (session == null) {
			return false;
		}

		return session.roles.contains(role);
	}

	@Override
	public CompletableFuture<Void> logout(String sessionId) {
		SessionData session = sessions.getIfPresent(sessionId);
		if (session == null) {
			return CompletableFuture.completedFuture(null);
		}

		return getProvider(sessionId, Collections.emptyList()).instance(IAuthenticationPromise.class).logout()
				.whenComplete((v, fn) -> {
					if (fn != null) {
						logger.error(fn.getMessage(), fn);
					}
					sessions.getCache().invalidate(sessionId);
				});
	}

	@Override
	public boolean isPasswordExpired(String sessionId) {
		SessionData session = sessions.getIfPresent(sessionId);
		if (session == null) {
			return false;
		}

		return session.passwordStatus == Status.Expired;
	}

	@Override
	public CompletableFuture<Void> updatePassword(String sessionId, String currentPassword, String newPassword,
			List<String> forwadedFor) {
		SessionData session = sessions.getIfPresent(sessionId);
		if (session == null) {
			return CompletableFuture.completedFuture(null);
		}

		return getProvider(sessionId, forwadedFor).instance(IUserPromise.class, session.domainUid)
				.setPassword(session.userUid, ChangePassword.create(currentPassword, newPassword));
	}
}
