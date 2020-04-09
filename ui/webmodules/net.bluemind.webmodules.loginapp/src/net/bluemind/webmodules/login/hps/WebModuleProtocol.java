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
package net.bluemind.webmodules.login.hps;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.netflix.spectator.api.Registry;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.proxy.http.IAuthProvider;
import net.bluemind.proxy.http.auth.api.AuthRequirements;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.IAuthProtocol;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer.ISessionStore;
import net.bluemind.proxy.http.auth.api.SecurityConfig;
import net.bluemind.webmodule.server.WebModuleRootHandler;
import net.bluemind.webmodules.login.CSRFTokenManager;
import net.bluemind.webmodules.login.UpdatePasswordHandler;

public class WebModuleProtocol implements IAuthProtocol {

	private static final Logger logger = LoggerFactory.getLogger(WebModuleProtocol.class);
	private static final Registry registry = MetricsRegistry.get();
	private final IdFactory idFactory;
	private WebModuleRootHandler rootHandler;

	public WebModuleProtocol(Vertx vertx) {
		this.idFactory = new IdFactory(registry, WebModuleProtocol.class);
		rootHandler = WebModuleRootHandler.build(vertx);
	}

	@Override
	public void proceed(AuthRequirements authState, ISessionStore ss, IAuthProvider provider, HttpServerRequest req) {
		final String uri = req.uri();
		if (logger.isDebugEnabled()) {
			logger.debug("proceed {}...", uri);
		}

		if (managedUpdatePasswordUrls(authState, ss, provider, req)) {
			return;
		}

		if (managedLoginUrls(authState, ss, provider, req)) {
			return;
		}

		if (uri.startsWith("/login")) {
			rootHandler.handle(req);
		} else {
			req.response().setStatusCode(302);
			try {
				req.response().headers().add("Location",
						"/login/index.html?askedUri=" + URLEncoder.encode(req.uri(), "utf-8"));
			} catch (UnsupportedEncodingException e) {
				req.response().headers().add("Location", "/login/index.html");
			}
			req.response().end();
		}
	}

	private boolean managedLoginUrls(AuthRequirements authState, ISessionStore ss, IAuthProvider provider,
			HttpServerRequest req) {
		if (!authState.authNeeded && authState.sessionId != null
				&& (req.path().equals("/login/index.html") || req.path().equals("/login/native"))) {
			redirectAlreadyAuthenticated(req);
			return true;
		}

		// login form data
		if (req.method() == HttpMethod.POST && (req.uri().endsWith("index.html") || req.uri().endsWith("native"))) {
			req.setExpectMultipart(true);
			req.endHandler(new Handler<Void>() {

				@Override
				public void handle(Void event) {
					loginFormSubmitted(provider, ss, req, authState.protocol);
				}

			});

			return true;
		}

		return false;
	}

	private void redirectAlreadyAuthenticated(HttpServerRequest req) {
		String askedUri = checkAskedUri(req.params());
		HttpServerResponse resp = req.response();
		resp.headers().add("Location", askedUri);
		resp.setStatusCode(302);
		resp.end();
	}

	private void loginFormSubmitted(IAuthProvider prov, final ISessionStore ss, HttpServerRequest req,
			IAuthProtocol protocol) {
		MultiMap attributes = req.formAttributes();

		String askedUri = checkAskedUri(attributes);

		String login = attributes.get("login");
		String pass = attributes.get("password");
		if (login == null || login.isEmpty()) {
			handleAuthFailure(req, new ServerFault("invalid login", ErrorCode.INVALID_PARAMETER));
			return;
		}

		if (!CSRFTokenManager.INSTANCE.checkToken(req, attributes.get("csrfToken"))) {
			handleAuthFailure(req, new ServerFault("invalid token", ErrorCode.INVALID_PARAMETER));
			return;
		}
		final boolean privateComputer = "priv".equals(attributes.get("priv"));

		if (attributes.get("domain") != null && !login.contains("@")) {
			login += "@" + attributes.get("domain");
		}
		logger.info("[{}] l: '{}', p: '{}' for {}", prov, login, "****", askedUri);

		final HttpServerResponse resp = req.response();

		List<String> forwadedFor = new ArrayList<>(req.headers().getAll("X-Forwarded-For"));
		forwadedFor.add(req.remoteAddress().host());
		prov.sessionId(login, pass, privateComputer, forwadedFor, new AsyncHandler<String>() {

			@Override
			public void success(String sid) {
				registry.counter(idFactory.name("authCount", "status", "success")).increment();

				// get cookie...
				String proxySid = ss.newSession(sid, protocol);
				boolean updatePasswordNeeded = prov.isPasswordExpired(sid);
				logger.info("Got sid: {}, proxySid: {}, updatePasswordNeeded: {}", sid, proxySid, updatePasswordNeeded);
				resp.headers().add("Location", updatePasswordNeeded ? "/login/updatepassword.html" : askedUri);
				resp.setStatusCode(302);
				Cookie co = new DefaultCookie("BMHPS", proxySid);
				co.setPath("/");
				co.setHttpOnly(true);
				if (SecurityConfig.secureCookies) {
					co.setSecure(true);
				}

				Cookie privacyCo = new DefaultCookie("BMPRIVACY", Boolean.toString(privateComputer));
				privacyCo.setPath("/");
				if (SecurityConfig.secureCookies) {
					privacyCo.setSecure(true);
				}
				resp.headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(co));
				resp.headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(privacyCo));

				cleanupWebmailCookies(resp);
				resp.end();
			}

			@Override
			public void failure(Throwable e) {
				registry.counter(idFactory.name("authCount", "status", "failure")).increment();
				handleAuthFailure(req, e);
			}
		});
	}

	private void cleanupWebmailCookies(HttpServerResponse resp) {

		DefaultCookie webmailCookie = new DefaultCookie("roundcube_sessauth", "del");
		webmailCookie.setPath("/webmail");
		webmailCookie.setSecure(true);
		webmailCookie.setMaxAge(0);
		webmailCookie.setHttpOnly(true);
		resp.headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(webmailCookie));

		webmailCookie = new DefaultCookie("roundcube_sessauth", "del");
		webmailCookie.setPath("/");
		webmailCookie.setSecure(true);
		webmailCookie.setMaxAge(0);
		webmailCookie.setHttpOnly(true);
		resp.headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(webmailCookie));

		webmailCookie = new DefaultCookie("roundcube_sessid", "del");
		webmailCookie.setPath("/");
		webmailCookie.setSecure(true);
		webmailCookie.setMaxAge(0);
		webmailCookie.setHttpOnly(true);
		resp.headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(webmailCookie));
		webmailCookie = new DefaultCookie("roundcube_sessid", "del");
		webmailCookie.setPath("/webmail");
		webmailCookie.setSecure(true);
		webmailCookie.setMaxAge(0);
		webmailCookie.setHttpOnly(true);
		resp.headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(webmailCookie));
	}

	protected void handleAuthFailure(HttpServerRequest req, Throwable e) {
		MultiMap attributes = req.formAttributes();
		final String askedUri = checkAskedUri(attributes);

		String code = "10";
		if (e instanceof ServerFault) {
			ServerFault sf = (ServerFault) e;
			if (sf.getCode() == ErrorCode.INVALID_PARAMETER) {
				code = "1";
			} else if (sf.getCode() == ErrorCode.INVALID_PASSWORD) {
				code = "2";
			}
		}
		logger.warn("Auth failure ({}), display login page.", e.getMessage(), e);
		String q = "?authErrorCode=" + code;
		if (askedUri != null) {
			try {
				new URI(askedUri);
				q += "&askedUri=" + URLEncoder.encode(askedUri, "utf-8");
			} catch (URISyntaxException | UnsupportedEncodingException e1) {
				logger.warn("asked uri is not a valid uri : {} ", e1);
			}
		}

		String login = URLEncoder.encode(attributes.get("login"));
		q += "&userLogin=" + login;

		final boolean privateComputer = "priv".equals(attributes.get("priv"));

		Cookie privacyCo = new DefaultCookie("BMPRIVACY", Boolean.toString(privateComputer));
		privacyCo.setPath("/");
		if (SecurityConfig.secureCookies) {
			privacyCo.setSecure(true);
		}

		HttpServerResponse resp = req.response();
		resp.headers().add("Set-Cookie", ServerCookieEncoder.LAX.encode(privacyCo));
		resp.headers().add("Location", String.format("%s%s", req.path(), q));
		resp.setStatusCode(302);
		resp.end();
	}

	@Override
	public void logout(HttpServerRequest event) {
		HttpServerResponse resp = event.response();
		resp.headers().add("Location", "/");
		resp.setStatusCode(302);
		resp.end();
	}

	private boolean managedUpdatePasswordUrls(AuthRequirements authState, ISessionStore ss, IAuthProvider provider,
			HttpServerRequest req) {
		if ((authState.authNeeded || authState.sessionId == null) && req.path().equals("/login/updatepassword.html")) {
			req.response().headers().add("Location", "/");
			req.response().setStatusCode(302);
			req.response().end();
			return true;
		}

		if (!authState.authNeeded && authState.sessionId != null && provider.isPasswordExpired(authState.sessionId)
				&& !req.path().startsWith("/login/")) {
			req.response().headers().add("Location", "/login/updatepassword.html");
			req.response().setStatusCode(302);
			req.response().end();
			return true;
		}

		if (!authState.authNeeded && authState.sessionId != null && !provider.isPasswordExpired(authState.sessionId)
				&& req.path().equals("/login/updatepassword.html")) {
			req.response().headers().add("Location", "/");
			req.response().setStatusCode(302);
			req.response().end();
			return true;
		}

		// updatepassword form data
		if (req.method() == HttpMethod.POST && req.uri().endsWith("/updatepassword.html")) {
			req.setExpectMultipart(true);
			req.endHandler(new Handler<Void>() {

				@Override
				public void handle(Void event) {
					updatePasswordFormSubmitted(provider, ss, req, authState);
				}
			});

			return true;
		}

		return false;
	}

	private String checkAskedUri(MultiMap attributes) {
		String askedUri = attributes.get("askedUri") != null ? attributes.get("askedUri") : "/";

		try {
			new URI(askedUri);
		} catch (URISyntaxException e1) {
			logger.warn("asked uri is not un uri : {} ", e1);
			askedUri = "/";
		}

		return askedUri;
	}

	private void updatePasswordFormSubmitted(IAuthProvider provider, ISessionStore ss, HttpServerRequest req,
			AuthRequirements authRequirements) {
		MultiMap attributes = req.formAttributes();

		String askedUri = checkAskedUri(attributes);

		if (!CSRFTokenManager.INSTANCE.checkToken(req, attributes.get("csrfToken"))) {
			req.response().headers().add("Location", "/bluemind_sso_logout");
			req.response().setStatusCode(302);
			req.response().end();
			return;
		}

		String currentPassword = attributes.get("password");
		String newPassword = attributes.get("new-password");
		String confirmNewPassword = attributes.get("confirm-password");

		if (Strings.isNullOrEmpty(currentPassword) || Strings.isNullOrEmpty(newPassword)
				|| Strings.isNullOrEmpty(confirmNewPassword)) {
			req.response().headers().add("Location", String.format("/login/updatepassword.html?authErrorCode=%s",
					UpdatePasswordHandler.MsgErrorCode.nullpassword));
			req.response().setStatusCode(302);
			req.response().end();
			return;
		}

		if (!newPassword.equals(confirmNewPassword)) {
			req.response().headers().add("Location", String.format("/login/updatepassword.html?authErrorCode=%s",
					UpdatePasswordHandler.MsgErrorCode.passwordnotmatch));
			req.response().setStatusCode(302);
			req.response().end();
			return;
		}

		logger.info("[{}] session: '{}' expired password update, askedUri: '{}'", provider, authRequirements.sessionId,
				askedUri);

		List<String> forwadedFor = new ArrayList<>(req.headers().getAll("X-Forwarded-For"));
		forwadedFor.add(req.remoteAddress().host());
		provider.updatePassword(authRequirements.sessionId, currentPassword, newPassword, forwadedFor)
				.whenComplete((r, fn) -> {
					if (fn != null) {
						String authErrorMsg = UpdatePasswordHandler.MsgErrorCode.unknown.name();
						if (fn instanceof ServerFault) {
							switch (((ServerFault) fn).getCode()) {
							case NOT_FOUND:
								authErrorMsg = UpdatePasswordHandler.MsgErrorCode.usernotfound.name();
								break;
							case AUTHENTICATION_FAIL:
								authErrorMsg = UpdatePasswordHandler.MsgErrorCode.invalidpassword.name();
								break;
							case INVALID_PASSWORD:
								authErrorMsg = UpdatePasswordHandler.MsgErrorCode.invalidnewpassword.name();
								try {
									authErrorMsg = URLEncoder.encode(fn.getMessage(), "utf-8");
								} catch (UnsupportedEncodingException e) {
								}
								break;
							case INVALID_PARAMETER:
								authErrorMsg = UpdatePasswordHandler.MsgErrorCode.invalidParameter.name();
								if (fn.getMessage().equalsIgnoreCase("password must not be empty")) {
									authErrorMsg = UpdatePasswordHandler.MsgErrorCode.emptyNewPassword.name();
									break;
								}

								if (fn.getMessage().equalsIgnoreCase("invalid character in password")) {
									authErrorMsg = UpdatePasswordHandler.MsgErrorCode.invalidCharacterNewPassword
											.name();
									break;
								}

								if (fn.getMessage().equalsIgnoreCase("current and new password must not be the same")) {
									authErrorMsg = UpdatePasswordHandler.MsgErrorCode.mustNotTheSame.name();
									break;
								}

								try {
									authErrorMsg = URLEncoder.encode(fn.getMessage(), "utf-8");
								} catch (UnsupportedEncodingException e) {
								}
								break;
							default:
								break;
							}
						}

						req.response().headers().add("Location",
								String.format("/login/updatepassword.html?authErrorCode=%s", authErrorMsg));
						req.response().setStatusCode(302);
						req.response().end();
						return;
					}

					req.response().headers().add("Location", "/bluemind_sso_logout");
					req.response().setStatusCode(302);
					req.response().end();
					return;
				});
	}
}
