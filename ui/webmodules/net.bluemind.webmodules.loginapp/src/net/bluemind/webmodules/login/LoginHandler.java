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
package net.bluemind.webmodules.login;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.config.Token;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.BMVersion;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.ITaggedServiceProvider;
import net.bluemind.core.rest.http.VertxServiceProvider;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.network.topology.Topology;
import net.bluemind.system.api.IInstallationAsync;
import net.bluemind.system.api.InstallationVersion;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.server.handlers.AbstractIndexHandler;

public class LoginHandler extends AbstractIndexHandler implements NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);
	static Configuration cfg;
	private static final String defaultLanguage = "en";

	static {
		cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		cfg.setClassForTemplateLoading(LoginHandler.class, "/templates");
		cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);

	}

	private Vertx vertx;
	private HttpClientProvider clientProvider;

	private InstallationVersion version;

	private Supplier<Optional<String>> defaultDomain;

	public LoginHandler() {
		AtomicReference<SharedMap<String, String>> sysconf = new AtomicReference<>();
		MQ.init().thenAccept(v -> sysconf.set(MQ.sharedMap("system.configuration")));

		defaultDomain = () -> Optional.ofNullable(sysconf.get())
				.map(sm -> Optional.ofNullable(sm.get(SysConfKeys.default_domain.name()) != null
						&& !sm.get(SysConfKeys.default_domain.name()).isEmpty()
								? sm.get(SysConfKeys.default_domain.name())
								: null))
				.orElse(Optional.empty());
	}

	@Override
	protected String getTemplateName() {
		return "login.xml";
	}

	@Override
	protected void loadPageModel(HttpServerRequest request, Map<String, Object> model) {
		super.loadPageModel(request, model);

		String csrfToken = CSRFTokenManager.INSTANCE.initRequest(request);
		ResourceBundle resourceBundle = ResourceBundle.getBundle("OSGI-INF/l10n/bundle", new Locale(getLang(request)));

		if (request.path().endsWith("native")) {
			model.put("actionPath", "native");
		} else {
			model.put("actionPath", "index.html");
		}
		model.put("csrfToken", csrfToken);
		model.put("storedRequestId", "x");
		boolean privateComputer = true;
		String cooks = request.headers().get(HttpHeaders.COOKIE);
		if (cooks != null) {

			for (Cookie c : ServerCookieDecoder.LAX.decode(cooks)) {
				if ("BMPRIVACY".equalsIgnoreCase(c.name())) {
					privateComputer = "true".equals(c.value());
					break;
				}
			}
		}

		String userLogin = request.params().get("userLogin");
		if (null == userLogin) {
			userLogin = "";
		}
		model.put("userLogin", userLogin);

		String error = request.params().get("authErrorCode");
		if (error != null && !error.isEmpty()) {
			if (error.equals("1") || error.equals("2")) {
				model.put("authErrorMsg", resourceBundle.getString("login.error.1"));
			} else {
				// system error
				model.put("authErrorMsg", resourceBundle.getString("login.error.10"));
			}
		}

		model.put("priv", "" + privateComputer);

		String askedUri = request.params().get("askedUri");
		if (askedUri == null) {
			askedUri = "/";
		} else {
			try {
				new URI(askedUri);
			} catch (URISyntaxException e) {
				logger.warn("asked uri is not un uri : {} ", askedUri, e);
				askedUri = "/";
			}

		}
		model.put("askedUri", askedUri);
		if (version != null) {
			model.put("bmVersion", version.versionName);
			model.put("buildVersion", version.softwareVersion);
			if (!version.softwareVersion.equals(version.databaseVersion)) {
				model.put("version-not-ok", true);
			} else {
				model.put("version-not-ok", false);
			}
		} else {
			logger.warn("version is not available, use bundle version");
			model.put("bmVersion", BMVersion.getVersionName());
			model.put("buildVersion", BMVersion.getVersion());
		}

		defaultDomain.get().ifPresent(dd -> model.put("defaultDomain", dd));

		model.put("msg", new MessageResolverMethod(resourceBundle, new Locale(getLang(request))));
		logger.debug("display login page with model {}", model);
	}

	@Override
	protected String getLang(HttpServerRequest request) {
		String acceptLang = request.headers().get("Accept-Language");
		if (acceptLang == null) {
			return defaultLanguage;
		}
		return Locale.LanguageRange.parse(acceptLang).stream() //
				.map(range -> Locale.forLanguageTag(range.getRange())).findFirst() //
				.orElse(new Locale.Builder().setLanguage(defaultLanguage).build()).getLanguage().toLowerCase();
	}

	private ITaggedServiceProvider getProvider() {
		ILocator lc = (String service, AsyncHandler<String[]> asyncHandler) -> asyncHandler.success(
				new String[] { Topology.get().anyIfPresent(service).map(s -> s.value.address()).orElse("127.0.0.1") });

		return new VertxServiceProvider(clientProvider, lc, Token.admin0());
	}

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
		clientProvider = new HttpClientProvider(vertx);
		loadVersion();
	}

	private void loadVersion() {
		vertx.setTimer(2000, timerId -> {
			getProvider().instance("bm/core", IInstallationAsync.class)
					.getVersion(new AsyncHandler<InstallationVersion>() {

						@Override
						public void success(InstallationVersion value) {
							LoginHandler.this.version = value;

						}

						@Override
						public void failure(Throwable e) {
							logger.error("error retrieving installation version (message:{}), try again ",
									e.getMessage());
							loadVersion();
						}
					});
		});
	}
}
