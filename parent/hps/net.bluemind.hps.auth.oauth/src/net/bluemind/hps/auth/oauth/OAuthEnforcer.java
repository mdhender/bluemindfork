/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.hps.auth.oauth;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.proxy.http.NeedVertx;
import net.bluemind.proxy.http.auth.api.AuthRequirements;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer;

public class OAuthEnforcer implements IAuthEnforcer, NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(OAuthEnforcer.class);

	private OAuthConf oAuthConf;
	private Boolean enabled;
	private Vertx vertx;
	private Optional<HttpClient> httpClient = Optional.empty();

	public OAuthEnforcer() {
		File ini = new File("/etc/bm/oauth.ini");
		enabled = ini.exists();
		if (enabled.booleanValue()) {
			OAuthIni oAuthIni = new OAuthIni("/etc/bm/oauth.ini");
			String host = oAuthIni.getProperty("host");
			int port = Integer.parseInt(oAuthIni.getProperty("port"));
			String realm = oAuthIni.getProperty("realm");
			String clientId = oAuthIni.getProperty("client-id");
			String clientSecret = oAuthIni.getProperty("client-secret");
			oAuthConf = new OAuthConf(host, port, realm, clientId, clientSecret);
		}
	}

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public AuthRequirements enforce(ISessionStore checker, HttpServerRequest req) {
		if (!enabled.booleanValue()) {
			return AuthRequirements.notHandle();
		}

		// make /login/native happy ( and login public ressources available )
		if (req.path().startsWith("/login/") && !req.path().equals("/login/index.html")) {
			return AuthRequirements.notHandle();
		}

		if (io.vertx.core.http.HttpMethod.GET == req.method()) {
			return AuthRequirements.needSession(getProtocol());
		} else {
			return AuthRequirements.notHandle();
		}
	}

	@Override
	public IAuthProtocol getProtocol() {
		return new OAuthProtocol(httpClient.orElseGet(this::initHttpClient), oAuthConf);
	}

	private HttpClient initHttpClient() {
		URL url = null;
		try {
			url = new URL(String.format("%s://%s", oAuthConf.port() == 443 ? "https" : "http", oAuthConf.host()));
		} catch (MalformedURLException e) {
			logger.error("Invalid URL '{}' ?", oAuthConf.host());
			throw new RuntimeException(e);
		}

		HttpClientOptions opts = new HttpClientOptions();
		opts.setDefaultHost(url.getHost());
		opts.setSsl(false);
		opts.setDefaultPort(oAuthConf.port());
		httpClient = Optional.of(vertx.createHttpClient(opts));

		return httpClient.get();
	}

}
