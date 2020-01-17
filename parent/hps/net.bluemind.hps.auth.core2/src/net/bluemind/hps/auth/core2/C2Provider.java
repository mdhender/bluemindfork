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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;

import io.vertx.core.Vertx;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.authentication.api.IAuthenticationPromise;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.config.Token;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.BMVersion;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.locator.vertxclient.VertxLocatorClient;
import net.bluemind.mailbox.api.IMailboxesPromise;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.proxy.http.ExternalCreds;
import net.bluemind.proxy.http.IAuthProvider;
import net.bluemind.proxy.http.IDecorableRequest;
import net.bluemind.proxy.http.InvalidSession;

public class C2Provider implements IAuthProvider {

	public static final int MAX_SESSIONS_PER_USER = 5;

	private static final Logger logger = LoggerFactory.getLogger(C2Provider.class);
	private final Cache<String, SessionData> sessions;
	private HttpClientProvider clientProvider;

	public C2Provider(Vertx vertx, Cache<String, SessionData> sessions) {
		this.sessions = sessions;
		clientProvider = new HttpClientProvider(vertx);
	}

	@Override
	public void sessionId(final String loginAtDomain, final String password, boolean privateComputer,
			List<String> remoteIps, final AsyncHandler<String> handler) {

		VertxPromiseServiceProvider sp = getProvider("admin0@global.virt", null, remoteIps);

		logger.info("authenticating {}", loginAtDomain);
		IAuthenticationPromise auth = sp.instance("bm/core", IAuthenticationPromise.class);
		auth.loginWithParams(loginAtDomain, password, "bm-hps", true).exceptionally(e -> {
			logger.error("error during authentication of {}", loginAtDomain, e);
			handler.failure(new ServerFault("error login: No server assigned or server not avalaible"));
			return null;
		}).thenAccept(lr -> {
			logger.info("Authenticated {}, response: {}", loginAtDomain, lr.status);
			if (lr.status == Status.Ok) {
				handlerLoginSuccess(lr, remoteIps, handler);
			} else {
				handler.failure(new ServerFault("error during login " + lr.message, ErrorCode.INVALID_PASSWORD));
			}
		});
	}

	private void handlerLoginSuccess(LoginResponse lr, List<String> remoteIps, AsyncHandler<String> handler) {

		final SessionData sd = new SessionData();

		sd.authKey = lr.authKey;
		sd.userUid = lr.authUser.uid;
		sd.user = lr.authUser.value;
		sd.loginAtDomain = lr.latd;
		sd.domainUid = lr.authUser.domainUid;
		sd.rolesAsString = Joiner.on(",").join(lr.authUser.roles);
		// for 13k sessions, we end up with 3MB of duplicate strings here
		sd.rolesAsString = sd.rolesAsString.intern();
		sd.roles = lr.authUser.roles.stream().map(s -> s.intern()).collect(Collectors.toSet());
		sd.settings = lr.authUser.settings;

		// when creating a new session for a user, expire the oldest ones if he
		// already has MAX_SESSIONS_PER_USER.
		SessionData[] existingSessionForSameUser = sessions.asMap().values().stream()
				.filter(existingSession -> existingSession.userUid.equals(sd.userUid))
				.sorted((s1, s2) -> Long.compare(s1.createStamp, s2.createStamp)).toArray(SessionData[]::new);
		if (existingSessionForSameUser.length >= MAX_SESSIONS_PER_USER) {
			logger.warn("Max session (active: {}) exhausted for {}", existingSessionForSameUser.length,
					sd.loginAtDomain);
			for (int i = 0; i <= existingSessionForSameUser.length - MAX_SESSIONS_PER_USER; i++) {
				logout(existingSessionForSameUser[i].authKey);
			}
		}

		sessions.put(sd.authKey, sd);
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

		IMailboxesPromise mailboxClient = getProvider(externalCreds.getLoginAtDomain(), Token.admin0(), remoteIps)
				.instance(IMailboxesPromise.class, domainName);

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
	 * @param checkLatdOnBadAuth
	 *                               if true and sudo login response is bad, check
	 *                               if
	 *                               {@link net.bluemind.proxy.http.ExternalCreds#getLoginAtDomain()}
	 *                               is the real loginAtDomain
	 * @param remoteIps
	 * @param handler
	 * @param sp
	 * @param externalCreds
	 */
	private void doSudo(List<String> remoteIps, AsyncHandler<String> handler, ExternalCreds externalCreds) {
		logger.info("[{}] sessionId (EXT)", externalCreds.getLoginAtDomain());

		getProvider(externalCreds.getLoginAtDomain(), Token.admin0(), remoteIps).instance(IAuthenticationPromise.class)
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

		proxyReq.addHeader("BMUserLogin", sd.getUser().login);
		proxyReq.addHeader("BMAccountType", sd.getUser().accountType.name());
		proxyReq.addHeader("BMUserLATD", sd.loginAtDomain);
		if (sd.getUser().defaultEmail() != null) {
			proxyReq.addHeader("BMUserDefaultEmail", sd.getUser().defaultEmail().address);
		}
		proxyReq.addHeader("BMUserDomainId", sd.domainUid);

		if (sd.getUser().contactInfos != null) {
			VCard card = sd.getUser().contactInfos;
			addIfPresent(proxyReq, card.identification.name.givenNames, "BMUserFirstName");
			addIfPresent(proxyReq, card.identification.name.familyNames, "BMUserLastName");
			addIfPresent(proxyReq, card.identification.formatedName.value, sd.getUser().login, "BMUserFormatedName");
		} else {
			proxyReq.addHeader("BMUserFormatedName", sd.getUser().login);
		}

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

		if (sd.getUser().dataLocation != null) {
			proxyReq.addHeader("BMDataLocation", sd.getUser().dataLocation);
			proxyReq.addHeader("BMPartition",
					CyrusPartition.forServerAndDomain(sd.getUser().dataLocation, sd.domainUid).name);
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
	public void ping(String sessionId, final AsyncHandler<Boolean> handler) {

		final SessionData sess = sessions.getIfPresent(sessionId);
		if (sess == null) {
			handler.success(Boolean.FALSE);
		}
		String login = sess.loginAtDomain;
		String apiKey = sess.authKey;

		VertxPromiseServiceProvider sp = getProvider(login, apiKey, Collections.emptyList());
		sp.instance(IAuthenticationPromise.class).ping().exceptionally(e -> {
			logger.error("error during ping", e);
			handler.success(Boolean.FALSE);
			return null;
		}).thenAccept(v -> {
			logger.debug("ping ok for {}:{}", sess.loginAtDomain, sess.authKey);
			handler.success(Boolean.TRUE);
		});

	}

	@Override
	public void reload(String sessionId) {
		logger.debug("[{}] reload", sessionId);

	}

	private VertxPromiseServiceProvider getProvider(String login, String apiKey, List<String> remoteIps) {
		VertxLocatorClient vertxLocatorClient = new VertxLocatorClient(clientProvider, login);
		return new VertxPromiseServiceProvider(clientProvider, vertxLocatorClient, apiKey, remoteIps);

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

		return getProvider(session.loginAtDomain, sessionId, Collections.emptyList())
				.instance(IAuthenticationPromise.class).logout().whenComplete((v, fn) -> {
					if (fn != null) {
						logger.error(fn.getMessage(), fn);
					}
					sessions.invalidate(sessionId);
				});
	}
}
