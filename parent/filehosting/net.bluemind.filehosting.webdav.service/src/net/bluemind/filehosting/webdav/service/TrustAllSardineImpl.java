/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.filehosting.webdav.service;

import java.io.IOException;
import java.net.ProxySelector;
import java.security.cert.X509Certificate;
import java.time.Duration;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.VersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sardine.Version;
import com.github.sardine.impl.SardineImpl;

import net.bluemind.utils.Trust;

public class TrustAllSardineImpl extends SardineImpl {

	private static final int CONNECTION_TIMEOUT = Duration.ofMinutes(2).toMillisPart();
	private static final int SOCKET_TIMEOUT = Duration.ofMinutes(4).toMillisPart();

	private static final Logger logger = LoggerFactory.getLogger(TrustAllSardineImpl.class);

	public TrustAllSardineImpl(String username, String password) {
		super(username, password);
	}

	@Override
	protected HttpClientBuilder configure(ProxySelector selector, CredentialsProvider credentials) {
		Registry<ConnectionSocketFactory> schemeRegistry = this.createDefaultSchemeRegistry();
		HttpClientConnectionManager cm = this.createDefaultConnectionManager(schemeRegistry);
		String version = Version.getSpecification();
		if (version == null) {
			version = VersionInfo.UNAVAILABLE;
		}
		return HttpClients.custom().setUserAgent("Sardine/" + version).setDefaultCredentialsProvider(credentials)
				.setRedirectStrategy(this.createDefaultRedirectStrategy())
				.setDefaultRequestConfig(RequestConfig.custom()
						// Only selectively enable this for PUT but not all entity enclosing methods
						.setExpectContinueEnabled(false) //
						.setConnectTimeout(CONNECTION_TIMEOUT) //
						.setConnectionRequestTimeout(CONNECTION_TIMEOUT) //
						.setSocketTimeout(SOCKET_TIMEOUT) //
						.build())
				.setConnectionManager(cm)
				.setRoutePlanner(this.createDefaultRoutePlanner(this.createDefaultSchemePortResolver(), selector));
	}

	@Override
	protected ConnectionSocketFactory createDefaultSecureSocketFactory() {
		try {
			return getSSLConnectionSocketFactory();
		} catch (Exception e) {
			logger.warn("Cannot configure TrustAll ConnectionSocketFactory", e);
			return super.createDefaultSecureSocketFactory();
		}
	}

	private static final X509HostnameVerifier verifier = new X509HostnameVerifier() {

		@Override
		public boolean verify(String arg0, SSLSession arg1) {
			return true;
		}

		@Override
		public void verify(String arg0, SSLSocket arg1) throws IOException {
		}

		@Override
		public void verify(String arg0, X509Certificate arg1) throws SSLException {
		}

		@Override
		public void verify(String arg0, String[] arg1, String[] arg2) throws SSLException {
		}

	};

	private SSLConnectionSocketFactory getSSLConnectionSocketFactory() throws Exception {
		return new SSLConnectionSocketFactory(Trust.createSSLContext(), verifier);
	}

}
