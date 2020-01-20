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
package net.bluemind.proxy.http.impl.vertx;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import com.netflix.spectator.api.Registry;

import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.proxy.http.Activator;
import net.bluemind.proxy.http.HttpProxyServer;
import net.bluemind.proxy.http.IAuthProvider;
import net.bluemind.proxy.http.IAuthProviderFactory;
import net.bluemind.proxy.http.auth.api.AuthRequirements;
import net.bluemind.proxy.http.auth.api.CookieHelper;
import net.bluemind.proxy.http.auth.api.CookieHelper.CookieState;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer;
import net.bluemind.proxy.http.config.ForwardedLocation;
import net.bluemind.proxy.http.impl.SessionStore;

public final class ProtectedLocationHandler implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(ProtectedLocationHandler.class);
	private static final Registry registry = MetricsRegistry.get();
	private final IdFactory idFactory;
	private final ForwardedLocation fl;
	private final SessionStore ss;
	private final AuthenticatedHandler proxy;
	private final IAuthProvider authProv;
	private List<IAuthEnforcer> enforcers;
	private CoreState coreState;
	private SudoProtocol authKeyProtocol = new SudoProtocol();

	public ProtectedLocationHandler(Vertx vertx, List<IAuthEnforcer> enforcers, ForwardedLocation fl, SessionStore ss,
			CoreState coreState) {
		idFactory = new IdFactory(registry, HttpProxyServer.class);
		this.coreState = coreState;
		this.enforcers = enforcers;
		this.fl = fl;
		String authKind = fl.getRequiredAuthKind();
		IAuthProviderFactory authFactory = Activator.getAuthProvider(authKind);
		this.authProv = authFactory != null ? authFactory.get(vertx) : null;
		logger.debug("{} auth provided by {}", authKind, authProv);
		this.ss = ss;
		this.proxy = new AuthenticatedHandler(vertx, fl, registry, idFactory);
	}

	@Override
	public void handle(HttpServerRequest event) {
		registry.counter(idFactory.name("requestsCount", "kind", "protected")).increment();
		logger.debug("Protected location {}:{}{}", fl.getHost(), fl.getPort(), fl.getPathPrefix());

		event.response().putHeader("Content-Security-Policy",
				"default-src 'self' 'unsafe-inline' 'unsafe-eval'; img-src 'self' data: ");

		event.response().putHeader("Feature-Policy",
				"accelerometer 'none'; ambient-light-sensor 'none'; autoplay 'self'; battery 'none';"
						+ " camera 'none'; display-capture 'none'; document-domain 'none'; encrypted-media 'none';"
						+ " execution-while-not-rendered 'self'; execution-while-out-of-viewport 'self';"
						+ " fullscreen 'self'; geolocation 'none'; gyroscope 'none'; layout-animations 'none'; layout-animations 'none';"
						+ " layout-animations 'none'; legacy-image-formats 'none'; magnetometer 'none'; microphone 'none';"
						+ " midi 'none'; navigation-override 'none'; oversized-images 'none'; payment 'none'; picture-in-picture 'none';"
						+ " publickey-credentials 'none'; sync-xhr 'none'; usb 'none'; vr 'none'; wake-lock 'none'; xr-spatial-tracking 'none'; ");

		AuthRequirements reqs = authenticated(event);
		if (!reqs.authNeeded && reqs.sessionId != null) {
			if (event.path().equals("/login/index.html") || event.path().equals("/login/native")) {
				String redirectTo = "/";
				String askedUri = event.params().get("askedUri");
				if (askedUri != null) {
					try {
						new URI(askedUri);
					} catch (URISyntaxException e1) {
						logger.warn("asked uri is not un uri : {} ", e1);
						askedUri = "/";
					}
					redirectTo = askedUri;
				}

				HttpServerResponse resp = event.response();
				resp.headers().add("Location", redirectTo);
				resp.setStatusCode(302);
				resp.end();

			} else if (event.path().endsWith("bluemind_sso_logout")) {
				authProv.logout(reqs.sessionId).thenAccept(action -> {
					ss.purgeSession(reqs.sessionId);
					CookieHelper.purgeSessionCookie(event.response().headers());

					if (reqs.protocol == null) {
						HttpServerResponse resp = event.response();
						resp.headers().add("Location", "/");
						resp.setStatusCode(302);
						resp.end();
					} else {
						reqs.protocol.logout(event);
					}
				});
			} else {
				handleAuthenticated(event, reqs);
			}
		} else if (!reqs.authNeeded && reqs.sessionId == null) {
			UserReq ur = new UserReq(reqs.sessionId, event, null, ss);
			proxy.handle(ur);
		} else {
			logger.debug("Must authenticate event {}", event.uri());

			if (coreState.needUpgrade()) {
				event.response().setStatusCode(503);
				event.response().end();
				return;
			}
			// maintenance => show /login/index.html
			// when maintenance=true param is present
			if (coreState.maintenace() && event.method().equals("GET") && ( //
			!(event.path().equals("/login/index.html") && "true".equals(event.params().get("maintenance")))
					&& !(!event.path().equals("/login/index.html") && event.path().startsWith("/login"))//
			)) {
				event.response().setStatusCode(503);
				event.response().end();
				return;
			}

			reqs.protocol.proceed(reqs, ss, authProv, event);
		}

	}

	private void handleAuthenticated(HttpServerRequest event, AuthRequirements reqs) {

		if (fl.getRole() == null || authProv.inRole(reqs.sessionId, fl.getRole())) {
			UserReq ur = new UserReq(reqs.sessionId, event, authProv, ss);
			proxy.handle(ur);
		} else if (isFromLogin(event)) {
			// just logged in, was redirect to inacessible application,
			// redirect to /
			event.response().setStatusCode(302);
			event.response().headers().add("Location", "/");
			event.response().end();
		} else {
			logger.info("try to access to a forbidden uri {} but not in role {}", event.uri(), fl.getRole());
			// TODO forbidden may be handled by IAuthEnforcer ?
			event.response().setStatusCode(403).end();
		}
	}

	private boolean isFromLogin(HttpServerRequest req) {
		String ref = req.headers().get("Referer");
		if (ref == null || ref.isEmpty()) {
			return false;
		}

		try {
			URL url = new URL(ref);

			return "/login/index.html".equals(url.getPath());
		} catch (Exception e) {
			logger.warn("could not parse Referer {} : {}", ref, e.getMessage());
			return false;
		}
	}

	private AuthRequirements authenticated(HttpServerRequest event) {

		if (event.absoluteURI().getPath().endsWith("bluemind_sso_security")) {
			return AuthRequirements.needSession(authKeyProtocol);
		}

		if (CookieHelper.check(ss, event).state == CookieState.Ok) {
			return AuthRequirements.existingSession(ss, event);
		}

		String uri = event.uri();
		if (fl.isWhitelisted(uri)) {
			logger.info("Whitelisted URL {}", uri);
			return AuthRequirements.noNeedSession();
		}

		AuthRequirements ar = null;
		for (IAuthEnforcer af : enforcers) {
			ar = af.enforce(ss, event);
			if (ar.authNeeded) {
				return ar;
			}
		}

		throw new RuntimeException("should not happen");
	}

}
