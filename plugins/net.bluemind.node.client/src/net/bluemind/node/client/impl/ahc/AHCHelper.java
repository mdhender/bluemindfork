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

import javax.net.ssl.KeyManagerFactory;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public final class AHCHelper {

	private static final Logger logger = LoggerFactory.getLogger(AHCHelper.class);

	private static AsyncHttpClient client;
	private static boolean ssl;
	private static final int TIMEOUT = 60000 * 60;
	private static final int DEFAULT_IDLE_TIMEOUT = 60000;

	public static final File TRUSTSTORE = new File("/etc/bm/bm.jks");
	public static final File KEYSTORE = new File("/etc/bm/nodeclient_keystore.jks");

	private static final boolean EPOLL_DISABLED = new File("/etc/bm/netty.epoll.disabled").exists();

	static {
		client = newClient();
	}

	private AHCHelper() {
	}

	private static AsyncHttpClient newClient() {
		AsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder() //
				.setFollowRedirect(false) //
				.setMaxRedirects(0) //
				.setPooledConnectionIdleTimeout(10000) //
				.setMaxRequestRetry(0) //
				.setHandshakeTimeout(30000) //
				.setRequestTimeout(TIMEOUT) //
				.setReadTimeout(DEFAULT_IDLE_TIMEOUT) //
				.setSslContext(buildSSLContext()) //
				.setUseNativeTransport(!EPOLL_DISABLED).build();
		return new DefaultAsyncHttpClient(config);
	}

	public static AsyncHttpClient get() {
		return client;
	}

	private static SslContext buildSSLContext() {
		File ks = KEYSTORE;
		File ts = TRUSTSTORE;
		try {
			if (ts.exists() && ks.exists()) {

				SslContext ctx = SslContextBuilder.forClient().keyManager(getKeyMgrs(ks.getAbsolutePath(), "password"))
						.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
				logger.info("secure context created.");

				ssl = true;
				return ctx;
			} else {
				logger.info("unsecure context created.");
				return SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	private static KeyManagerFactory getKeyMgrs(final String ksPath, final String ksPassword) throws Exception {
		KeyManagerFactory fact = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		KeyStore ks = loadStore(ksPath, ksPassword);
		fact.init(ks, ksPassword != null ? ksPassword.toCharArray() : null);
		return fact;
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

	public static synchronized AsyncHttpClient rebuild() {
		if (KEYSTORE.exists()) {
			try {
				client.close();
			} catch (IOException e) {
				// ok
			}
			client = newClient();
		}
		return client;
	}

}
