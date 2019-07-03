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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public final class AHCHelper {

	private static final Logger logger = LoggerFactory.getLogger(AHCHelper.class);

	private static AsyncHttpClient client;
	private static boolean ssl;
	private static final int TIMEOUT = 60000 * 60;
	private static final int DEFAULT_IDLE_TIMEOUT = 60000;

	public static final File TRUSTSTORE = new File("/etc/bm/bm.jks");
	public static final File KEYSTORE = new File("/etc/bm/nodeclient_keystore.jks");

	static {
		client = newClient();
	}

	private static AsyncHttpClient newClient() {
		AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setFollowRedirect(false).setMaxRedirects(0)
				.setPooledConnectionIdleTimeout(60000).setMaxRequestRetry(0).setRequestTimeout(TIMEOUT)
				.setReadTimeout(DEFAULT_IDLE_TIMEOUT).setSSLContext(buildSSLContext()).setAcceptAnyCertificate(true)
				.setAllowPoolingConnections(true).setHostnameVerifier(new HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
				}).setAllowPoolingSslConnections(true).build();
		AsyncHttpClient httpClient = new AsyncHttpClient(config);
		return httpClient;
	}

	public static AsyncHttpClient get() {
		return client;
	}

	private static SSLContext buildSSLContext() {
		File ks = KEYSTORE;
		File ts = TRUSTSTORE;
		if (ts.exists() && ks.exists()) {
			try {
				SSLContext context = SSLContext.getInstance("TLS");
				KeyManager[] keyMgrs = getKeyMgrs(ks.getAbsolutePath(), "password");
				TrustManager[] trustMgrs = getTrustMgrs(ts.getAbsolutePath(), "bluemind");
				context.init(keyMgrs, trustMgrs, new SecureRandom());
				logger.info("secure context created.");
				ssl = true;
				return context;
			} catch (Exception e) {
				logger.error("Failed to create secure context", e);
				throw new RuntimeException(e.getMessage());
			}
		} else {
			logger.info("unsecure context created.");
			return Trust.createSSLContext();
		}
	}

	private static TrustManager[] getTrustMgrs(final String tsPath, final String tsPassword) throws Exception {
		TrustManagerFactory fact = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		KeyStore ts = loadStore(tsPath, tsPassword);
		fact.init(ts);
		return fact.getTrustManagers();
	}

	private static KeyManager[] getKeyMgrs(final String ksPath, final String ksPassword) throws Exception {
		KeyManagerFactory fact = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		KeyStore ks = loadStore(ksPath, ksPassword);
		fact.init(ks, ksPassword != null ? ksPassword.toCharArray() : null);
		return fact.getKeyManagers();
	}

	private static KeyStore loadStore(String path, final String ksPassword) throws Exception {
		KeyStore ks = KeyStore.getInstance("JKS");
		InputStream in = null;
		try {
			in = new FileInputStream(new File(path));
			ks.load(in, ksPassword != null ? ksPassword.toCharArray() : null);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignore) {
				}
			}
		}
		return ks;
	}

	public static boolean hasClientCertificate() {
		return ssl;
	}

	public static boolean mayRebuild() {
		return KEYSTORE.exists();
	}

	public synchronized static AsyncHttpClient rebuild() {
		if (KEYSTORE.exists()) {
			client.close();
			client = newClient();
		}
		return client;
	}

}
