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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpServerRequest;

import net.bluemind.proxy.http.NeedVertx;
import net.bluemind.proxy.http.auth.api.AuthRequirements;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer;

public class CasAuthEnforcer implements IAuthEnforcer, NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(CasAuthEnforcer.class);
	private String casURL;
	private String casDomain;
	private String callbackURL;
	private boolean casEnabled;
	private HttpClient httpClient;
	private String baseUri;

	public CasAuthEnforcer() {
		File bm = new File("/etc/bm/bm.ini");
		Ini bmINI = null;
		try {
			bmINI = new Ini(bm);
			Section section = bmINI.get("global");
			casURL = section.get("casUrl");
			casDomain = section.get("casDomain");
			callbackURL = section.get("external-protocol") + "://" + section.get("external-url");
			if (callbackURL.endsWith("/")) {
				callbackURL = callbackURL.substring(0, callbackURL.length() - 1);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		casEnabled = (casURL != null) && (casDomain != null);
		logger.info("[CAS] casEnabled=" + casEnabled + " -> casURL=" + casURL);
	}

	@Override
	public AuthRequirements enforce(ISessionStore checker, HttpServerRequest req) {
		if (!casEnabled) {
			return AuthRequirements.notHandle();
		}

		// make /login/native happy ( and login public ressources available )
		if (req.path().startsWith("/login/") && !req.path().equals("/login/index.html")) {
			return AuthRequirements.notHandle();
		}
		if ("GET".equals(req.method())) {
			// only redirect GET to not pass
			CasProtocol protocol = new CasProtocol(httpClient, casURL, baseUri, casDomain, callbackURL);
			return AuthRequirements.needSession(protocol);
		} else {
			return AuthRequirements.notHandle();
		}
	}

	@Override
	public void setVertx(Vertx vertx) {
		if (casEnabled) {
			httpClient = vertx.createHttpClient();
			URL url = null;
			try {
				url = new URL(casURL);
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
			baseUri = url.getPath();
			httpClient.setHost(url.getHost());
			httpClient.setSSL(url.getProtocol().equals("https"));
			httpClient.setPort(url.getPort() != -1 ? url.getPort() : (url.getProtocol().equals("https") ? 443 : 80));
			if (httpClient.isSSL()) {
				httpClient.setTrustAll(true);
				httpClient.setVerifyHost(false);
			}
			logger.info("cas client {} {}", httpClient.getHost(), httpClient.getPort());

		}
	}

}
