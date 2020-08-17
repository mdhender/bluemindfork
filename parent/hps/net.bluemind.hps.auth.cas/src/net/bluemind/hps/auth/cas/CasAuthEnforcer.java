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
package net.bluemind.hps.auth.cas;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.proxy.http.NeedVertx;
import net.bluemind.proxy.http.auth.api.AuthRequirements;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer;
import net.bluemind.system.api.SysConfKeys;

public class CasAuthEnforcer implements IAuthEnforcer, NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(CasAuthEnforcer.class);
	private final Supplier<String> casURL;
	private final Supplier<String> casDomain;
	private final Supplier<String> callbackURL;
	private final Supplier<Boolean> casEnabled;
	private Vertx vertx;
	private Optional<HttpClient> httpClient = Optional.empty();
	private boolean wasEnabled;

	public CasAuthEnforcer() {
		AtomicReference<SharedMap<String, String>> sysconf = new AtomicReference<>();
		MQ.init().thenAccept(v -> sysconf.set(MQ.sharedMap("system.configuration")));

		casURL = () -> Optional.ofNullable(sysconf.get())
				.map(sm -> sm.get(SysConfKeys.cas_url.name()) != null && !sm.get(SysConfKeys.cas_url.name()).isEmpty()
						? sm.get(SysConfKeys.cas_url.name())
						: null)
				.orElse(null);

		casDomain = () -> Optional.ofNullable(sysconf.get())
				.map(sm -> !Strings.isNullOrEmpty(sm.get(SysConfKeys.cas_domain.name()))
						? sm.get(SysConfKeys.cas_domain.name())
						: null)
				.orElse(null);

		callbackURL = () -> Optional.ofNullable(sysconf.get())
				.map(sm -> !Strings.isNullOrEmpty(sm.get(SysConfKeys.external_url.name())) ? String.format("%s://%s",
						!Strings.isNullOrEmpty(sm.get(SysConfKeys.external_protocol.name()))
								? sm.get(SysConfKeys.external_protocol.name())
								: "https",
						sm.get(SysConfKeys.external_url.name())) : null)
				.orElse(null);

		Supplier<Boolean> casAuthType = () -> Optional.ofNullable(sysconf.get())
				.map(sm -> sm.get(SysConfKeys.auth_type.name()) != null
						&& sm.get(SysConfKeys.auth_type.name()).equalsIgnoreCase("cas"))
				.orElse(false);

		casEnabled = () -> (casAuthType.get() && casURL.get() != null && casDomain.get() != null
				&& callbackURL.get() != null);
	}

	@Override
	public AuthRequirements enforce(ISessionStore checker, HttpServerRequest req) {
		if (!casEnabled.get()) {
			logStatus(false);
			return AuthRequirements.notHandle();
		}

		logStatus(true);

		// make /login/native happy ( and login public ressources available )
		if (req.path().startsWith("/login/") && !req.path().equals("/login/index.html")) {
			return AuthRequirements.notHandle();
		}
		if (io.vertx.core.http.HttpMethod.GET == req.method()) {
			// only redirect GET to not pass
			CasProtocol protocol = new CasProtocol(httpClient.orElseGet(() -> initHttpClient()), casURL.get(),
					casDomain.get(), callbackURL.get());
			return AuthRequirements.needSession(protocol);
		} else {
			return AuthRequirements.notHandle();
		}
	}

	private void logStatus(boolean enabled) {
		if (enabled != wasEnabled) {
			logger.info("[CAS] casEnabled={}, casURL={}, casDomain={}, callBackURL={}", casEnabled.get(), casURL.get(),
					casDomain.get(), callbackURL.get());
			wasEnabled = enabled;
		}
	}

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	private HttpClient initHttpClient() {
		URL url = null;
		try {
			url = new URL(casURL.get());
		} catch (MalformedURLException e) {
			logger.error("Invalid CAS URL {} ?!", casURL.get());
			throw new RuntimeException(e);
		}

		HttpClientOptions opts = new HttpClientOptions();
		opts.setDefaultHost(url.getHost());
		opts.setSsl(url.getProtocol().equalsIgnoreCase("https"));
		opts.setDefaultPort(
				url.getPort() != -1 ? url.getPort() : (url.getProtocol().equalsIgnoreCase("https") ? 443 : 80));
		if (opts.isSsl()) {
			opts.setTrustAll(true);
			opts.setVerifyHost(false);
		}

		logger.info("CAS client {} {}", opts.getDefaultHost(), opts.getDefaultPort());

		HttpClient httpClient = vertx.createHttpClient(opts);
		this.httpClient = Optional.of(httpClient);
		return httpClient;
	}
}
