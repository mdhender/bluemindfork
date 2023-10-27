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
package net.bluemind.utils;

import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;

/**
 * Helper class to disable SSL chains validation
 */
public class Trust {

	private static SSLSocketFactory defaultSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
	private static final Logger logger = LoggerFactory.getLogger(Trust.class);

	public Trust() {

	}

	public void prepareConnection(String module, HttpURLConnection con) {
		if (!(con instanceof HttpsURLConnection)) {
			return;
		}

		if (trustall(module)) {
			((HttpsURLConnection) con).setSSLSocketFactory(getSSLSocketFactory(true));
			((HttpsURLConnection) con).setHostnameVerifier(getHostNameVerifier(true));
		}
	}

	public SSLSocketFactory getSSLSocketFactory(String module) {
		return getSSLSocketFactory(trustall(module));
	}

	public SSLSocketFactory getSSLSocketFactory(boolean trustAll) {
		if (trustAll) {
			return createSSLContext().getSocketFactory();
		} else {
			return defaultSocketFactory;
		}
	}

	private static final class TrustAll implements X509TrustManager {

		private static final X509Certificate[] emptyCertificates = new X509Certificate[0];

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return emptyCertificates;
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			logger.debug("checkServerTrust, auth: {}", authType);
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}
	}

	private static final X509TrustManager trustManager = new TrustAll();
	private static final TrustManager[] trustManagers = new TrustManager[] { trustManager };

	/**
	 * Creates a trust-all ssl context
	 * 
	 * @return an accept all context
	 */
	private SSLContext createSSLContext() {
		try {
			SecureRandom secureRandom = new SecureRandom();

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustManagers, secureRandom);

			return sslContext;
		} catch (Exception e) {
			throw new Error("Failed to initialize the SSLContext", e);
		}
	}

	public final HostnameVerifier getHostNameVerifier(String module) {
		return getHostNameVerifier(trustall(module));
	}

	private final HostnameVerifier getHostNameVerifier(boolean trustAll) {
		if (trustAll) {
			return (hostname, session) -> true;
		} else {
			return HttpsURLConnection.getDefaultHostnameVerifier();
		}
	}

	private boolean trustall(String module) {
		List<String> modules = getTrustAllModules();
		if (modules.isEmpty()) {
			return false;
		}
		if (modules.size() == 1 && modules.get(0).equals("ALL")) {
			return true;
		}
		return modules.contains(module);
	}

	protected List<String> getTrustAllModules() {
		return LocalSysconfCache.get().stringList(SysConfKeys.tls_trust_allcertificates.name());
	}

}
