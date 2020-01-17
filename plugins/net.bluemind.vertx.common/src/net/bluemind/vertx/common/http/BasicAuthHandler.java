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
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.vertx.common.CoreSession;
import net.bluemind.vertx.common.LoginHandler;

public class BasicAuthHandler implements Handler<HttpServerRequest> {

	public final Handler<AuthenticatedRequest> lh;
	private final String origin;
	private final String role;
	private static final Logger logger = LoggerFactory.getLogger(BasicAuthHandler.class);

	private static class ValidatedAuth {
		public ValidatedAuth(String login, String sid) {
			this.login = login;
			this.sid = sid;
		}

		String login;
		String sid;
	}

	private static Cache<String, ValidatedAuth> validated = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES).build();

	public static final class AuthenticatedRequest {
		public final HttpServerRequest req;
		public final String login;
		public final String sid;

		public AuthenticatedRequest(HttpServerRequest r, String l, String s) {
			this.req = r;
			this.login = l;
			this.sid = s;
		}
	}

	public BasicAuthHandler(String origin, Handler<AuthenticatedRequest> lh) {
		this.lh = lh;
		this.origin = origin;
		this.role = null;
	}

	public BasicAuthHandler(String origin, String role, Handler<AuthenticatedRequest> lh) {
		this.lh = lh;
		this.origin = origin;
		this.role = role;
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
				lh.handle(new AuthenticatedRequest(r, cached.login, cached.sid));
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

			CoreSession.attemptWithRole(creds.getLogin(), creds.getPassword(), origin, role, new LoginHandler() {

				@Override
				public void ok(String coreUrl, String sid, String principal) {
					r.resume();
					logger.info("successfull login by {} ", principal);
					validated.put(auth, new ValidatedAuth(principal, sid));
					lh.handle(new AuthenticatedRequest(r, principal, sid));
				}

				@Override
				public void ko() {
					// iOS does iso-8859-1
					final Creds creds = getCredentials(auth, StandardCharsets.ISO_8859_1);
					logger.info("login failed for {} try iOs encoding ", creds.getLogin());
					CoreSession.attemptWithRole(creds.getLogin(), creds.getPassword(), origin, role,
							new LoginHandler() {

								@Override
								public void ok(String coreUrl, String sid, String principal) {
									r.resume();
									logger.info("successfull login by {} iOs encoding", principal);
									validated.put(auth, new ValidatedAuth(principal, sid));
									lh.handle(new AuthenticatedRequest(r, principal, sid));
								}

								@Override
								public void ko() {
									logger.info("login failed for {} ", creds.getLogin());
									r.resume();
									r.response().putHeader("WWW-Authenticate", "Basic realm=\"bm.basic.auth\"")
											.setStatusCode(401).end();
								}
							});

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
