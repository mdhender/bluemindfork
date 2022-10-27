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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.node.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.INodeClientFactory;
import net.bluemind.node.client.impl.okhttp.OkHttpNodeClient;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpNodeClientFactory implements INodeClientFactory {

	public static final File TRUSTSTORE = new File("/etc/bm/bm.jks");
	public static final File KEYSTORE = new File("/etc/bm/nodeclient_keystore.jks");
	private static final ConcurrentHashMap<String, OkHttpNodeClient> clients = new ConcurrentHashMap<>();
	private static final Logger logger = LoggerFactory.getLogger(OkHttpNodeClientFactory.class);

	@Override
	public synchronized INodeClient create(String hostIpAddress) throws ServerFault {
		OkHttpNodeClient okCli = clients.get(hostIpAddress);
		if (okCli == null) {
			okCli = createNew(hostIpAddress);
			clients.put(hostIpAddress, okCli);
		} else if (!okCli.isSsl() && KEYSTORE.exists()) {
			okCli.close();
			okCli = createNew(hostIpAddress);
			clients.put(hostIpAddress, okCli);
		}
		return okCli;
	}

	private OkHttpNodeClient createNew(String hostIpAddress) {
		Builder builder = new OkHttpClient.Builder();
		int port = 8021;
		boolean trySsl = false;
		if (KEYSTORE.exists() && TRUSTSTORE.exists()) {
			SSLSocketFactory sslSocketFactory;
			try {

				SSLContext sslContext = SSLContext.getInstance("TLS");
				KeyManagerFactory keyMgr = getKeyMgrs(KEYSTORE.getAbsolutePath(), "password");

				sslContext.init(keyMgr.getKeyManagers(), trustManagers, null);
				sslSocketFactory = sslContext.getSocketFactory();
			} catch (Exception e) {
				throw new ServerFault(e);
			}
			builder.sslSocketFactory(sslSocketFactory, trustManager);
			trySsl = true;
		}
		builder.followRedirects(false);
		builder.callTimeout(1, TimeUnit.HOURS);
		builder.retryOnConnectionFailure(false);
		builder.hostnameVerifier((h, s) -> true);// NOSONAR
		OkHttpClient client = builder.build();
		if (trySsl) {
			Request req = new Request.Builder().url("https://" + hostIpAddress + ":8022/").method("OPTIONS", null)
					.build();
			try (Response response = client.newCall(req).execute()) {
				if (response.isSuccessful()) {
					port = 8022;
				}
			} catch (IOException ioe) {
				// Avoid bombarding tests output with ConnectException
				if (Boolean.getBoolean("ahcnode.fail.https.ok")) {
					logger.info("Error testing SSL connection to {}", hostIpAddress);
				} else {
					logger.info("Error testing SSL connection to {}", hostIpAddress, ioe);
				}
			}
		}
		return new OkHttpNodeClient(client, port,
				port == 8022 ? "https://" + hostIpAddress + ":8022" : "http://" + hostIpAddress + ":8021",
				port == 8022 ? "wss://" + hostIpAddress + ":8022/ws" : "ws://" + hostIpAddress + ":8021/ws");
	}

	@Override
	public void delete(String hostIpAddress) throws ServerFault {
		OkHttpNodeClient removed = clients.remove(hostIpAddress);
		if (removed != null) {
			removed.close();
		}
	}

	@Override
	public int getPriority() {
		return 1;
	}

	private static KeyManagerFactory getKeyMgrs(final String ksPath, final String ksPassword) throws Exception {
		KeyManagerFactory fact = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		KeyStore ks = loadStore(ksPath, ksPassword);
		fact.init(ks, ksPassword != null ? ksPassword.toCharArray() : null);
		return fact;
	}

	private static KeyStore loadStore(String path, final String ksPassword) throws Exception {
		KeyStore ks = KeyStore.getInstance("JKS");
		try (InputStream in = Files.newInputStream(Paths.get(path))) {
			ks.load(in, ksPassword != null ? ksPassword.toCharArray() : null);
		}
		return ks;
	}

	private static final class TrustAll implements X509TrustManager {

		private static final X509Certificate[] emptyCertificates = new X509Certificate[0];

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return emptyCertificates;
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}
	}

	private static final X509TrustManager trustManager = new TrustAll();
	private static final TrustManager[] trustManagers = new TrustManager[] { trustManager };

}
