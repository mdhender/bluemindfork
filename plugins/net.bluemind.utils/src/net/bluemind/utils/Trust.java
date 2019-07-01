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

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to disable SSL chains validation
 */
public class Trust {
	private static final Logger logger = LoggerFactory.getLogger(Trust.class);

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

	private static final HostnameVerifier acceptAll = new HostnameVerifier() {

		@Override
		public boolean verify(String hostname, SSLSession session) {
			logger.debug("verify '{}', session: {}", hostname, session);
			return true;
		}
	};

	/**
	 * Creates a trust-all ssl context
	 * 
	 * @return an accept all context
	 */
	public static SSLContext createSSLContext() {
		try {
			SecureRandom secureRandom = new SecureRandom();

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustManagers, secureRandom);

			return sslContext;
		} catch (Exception e) {
			throw new Error("Failed to initialize the SSLContext", e);
		}
	}

	public static X509TrustManager createTrustManager() {
		return trustManager;
	}

	public static HostnameVerifier acceptAllVerifier() {
		return acceptAll;
	}
}
