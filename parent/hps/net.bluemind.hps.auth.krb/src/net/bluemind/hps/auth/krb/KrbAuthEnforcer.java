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
package net.bluemind.hps.auth.krb;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpServerRequest;
import net.bluemind.proxy.http.auth.api.AuthRequirements;
import net.bluemind.proxy.http.auth.api.IAuthEnforcer;

public class KrbAuthEnforcer implements IAuthEnforcer {
	private static final Logger logger = LoggerFactory.getLogger(KrbAuthEnforcer.class);
	private boolean kerberosEnabled;
	private Map<String, String> domainMappings;

	public KrbAuthEnforcer() {

		kerberosEnabled = new File("/etc/bm-hps/hps.keytab").exists()
				&& (System.getProperty("java.security.auth.login.config") != null)
				&& (System.getProperty("java.security.krb5.conf") != null);
		logger.info("*** kerberos enabled: {}", kerberosEnabled);
		domainMappings = new HashMap<>();
		File f = new File("/etc/bm-hps/mappings.ini");
		if (f.exists()) {
			try {
				Ini krb5ini = new Ini(f);
				Section section = krb5ini.get("bm_mappings");
				for (Map.Entry<String, String> e : section.entrySet()) {
					logger.info(" * AD domain {} mapped to {} in BM", e.getKey(), e.getValue());
					domainMappings.put(e.getKey(), e.getValue());
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

	}

	@Override
	public AuthRequirements enforce(ISessionStore checker, HttpServerRequest req) {
		if (!kerberosEnabled) {
			return AuthRequirements.notHandle();
		}

		// make /login/native happy ( and login public ressources available )
		if (req.path().startsWith("/login/") && !req.path().equals("/login/index.html")) {
			return AuthRequirements.notHandle();
		}

		String authHeader = req.headers().get("Authorization");
		if (authHeader != null && authHeader.startsWith("Negotiate ")) {
			return AuthRequirements.needSession(getProtocol());
		} else {
			// /login/index.html will replay here
			req.response().headers().add("WWW-Authenticate", "Negotiate");
			return AuthRequirements.notHandle();
		}
	}

	@Override
	public IAuthProtocol getProtocol() {
		return new KrbProtocol(domainMappings);
	}
}
