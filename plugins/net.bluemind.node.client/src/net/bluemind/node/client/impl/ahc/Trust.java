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
package net.bluemind.node.client.impl.ahc;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Helper class to disable SSL chains validation
 */
public class Trust {

	private static final HostnameVerifier ACCEPT_ALL = new HostnameVerifier() {

		@Override
		public final boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

	public static HostnameVerifier acceptAllVerifier() {
		return ACCEPT_ALL;
	}

	/**
	 * Creates a trust-all ssl context
	 * 
	 * @return an accept all context
	 */
	public static SSLContext createSSLContext() {
		try {
			TrustManager[] trustManagers = new TrustManager[] { new X509TrustManager() {

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}
			} };
			SecureRandom secureRandom = new SecureRandom();

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustManagers, secureRandom);

			return sslContext;
		} catch (Exception e) {
			throw new Error("Failed to initialize the SSLContext", e);
		}
	}

	public static X509TrustManager createTrustManager() {
		return new X509TrustManager() {

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
		};
	}
}
