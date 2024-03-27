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
import java.util.ResourceBundle;

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
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.IInstallationAsync;
import net.bluemind.system.api.InstallationVersion;
import net.bluemind.webmodule.server.CSRFTokenManager;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.server.handlers.AbstractIndexHandler;

public class LoginHandler extends AbstractIndexHandler implements NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);
	static Configuration cfg;
	private static final String DEFAULT_LANGUAGE = "en";

	static {
		cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		cfg.setClassForTemplateLoading(LoginHandler.class, "/templates");
		cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
	}

	private Vertx vertx;
	private HttpClientProvider clientProvider;
	private InstallationVersion version;

	public LoginHandler() {
		// ok
	}

	@Override
	protected String getTemplateName() {
		return "login.xml";
	}

	@Override
	protected void loadPageModel(HttpServerRequest request, Map<String, Object> model) {
		super.loadPageModel(request, model);

		String csrfToken = CSRFTokenManager.INSTANCE.initRequest(request);
		ResourceBundle resourceBundle = ResourceBundle.getBundle("OSGI-INF/l10n/bundle", Locale.of(getLang(request)));

		if (request.path().endsWith("native")) {
			model.put("actionPath", "native");
		} else {
			model.put("actionPath", "index.html");
		}
		model.put("csrfToken", csrfToken);
		model.put("storedRequestId", "x");
		boolean privateComputer = isPrivateComputer(request);
		model.put("priv", "" + privateComputer);

		String userLogin = request.params().get("userLogin");
		if (null == userLogin) {
			userLogin = "";
		}
		model.put("userLogin", userLogin);

		setAuthErrorCode(request, model, resourceBundle);

		String askedUri = parseAskedUri(request);
		model.put("askedUri", askedUri);

		if (version != null) {
			model.put("bmVersion", version.versionName);
			model.put("buildVersion", version.softwareVersion);
			model.put("version-not-ok", !version.softwareVersion.equals(version.databaseVersion));
		} else {
			logger.warn("version is not available, use bundle version");
			model.put("bmVersion", BMVersion.getVersionName());
			model.put("buildVersion", BMVersion.getVersion());
		}

		model.put("msg", new MessageResolverMethod(resourceBundle, Locale.of(getLang(request))));
		logger.debug("display login page with model {}", model);
	}

	private boolean isPrivateComputer(HttpServerRequest request) {
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
		return privateComputer;
	}

	private void setAuthErrorCode(HttpServerRequest request, Map<String, Object> model, ResourceBundle resourceBundle) {
		String error = request.params().get("authErrorCode");
		if (error != null && !error.isEmpty()) {
			if (error.equals("1") || error.equals("2")) {
				model.put("authErrorMsg", resourceBundle.getString("login.error.1"));
			} else {
				// system error
				model.put("authErrorMsg", resourceBundle.getString("login.error.10"));
			}
		}
	}

	private String parseAskedUri(HttpServerRequest request) {
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
		return askedUri;
	}

	@Override
	protected String getLang(HttpServerRequest request) {
		String acceptLang = request.headers().get("Accept-Language");
		if (acceptLang == null) {
			return DEFAULT_LANGUAGE;
		}
		return Locale.LanguageRange.parse(acceptLang).stream() //
				.map(range -> Locale.forLanguageTag(range.getRange())).findFirst() //
				.orElse(new Locale.Builder().setLanguage(DEFAULT_LANGUAGE).build()).getLanguage().toLowerCase();
	}

	private ITaggedServiceProvider getProvider() {
		ILocator lc = (String service, AsyncHandler<String[]> asyncHandler) -> {
			String ip = Topology.getIfAvailable().flatMap(tp -> tp.anyIfPresent(service).map(s -> s.value.address()))
					.orElse("127.0.0.1");
			asyncHandler.success(new String[] { ip });
		};

		return new VertxServiceProvider(clientProvider, lc, Token.admin0());
	}

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
		clientProvider = new HttpClientProvider(vertx);
		loadVersion();
	}

	private void loadVersion() {
		vertx.setTimer(2000, timerId -> getProvider().instance(TagDescriptor.bm_core.getTag(), IInstallationAsync.class)
				.getVersion(new AsyncHandler<InstallationVersion>() {

					@Override
					public void success(InstallationVersion value) {
						LoginHandler.this.version = value;

					}

					@Override
					public void failure(Throwable e) {
						logger.error("error retrieving installation version (message:{}), try again ", e.getMessage());
						loadVersion();
					}
				}));
	}
}
