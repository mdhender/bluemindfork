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
package net.bluemind.core.rest.http;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.utils.Trust;

public class ClientSideServiceProvider implements IServiceProvider {

	private static final Logger logger = LoggerFactory.getLogger(ClientSideServiceProvider.class);
	private String base;
	private String apiKey;
	private List<String> remoteIps;
	private String origin;
	static final AsyncHttpClient defaultClient;
	private final AsyncHttpClient client;

	static {
		defaultClient = createClient(true, 120);
	}

	private static AsyncHttpClient createClient(boolean pooled, int timeoutInSeconds) {
		AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setSSLContext(Trust.createSSLContext()) //
				.setHostnameVerifier(Trust.acceptAllVerifier()) //
				.setConnectTimeout(timeoutInSeconds * 1000) //
				.setReadTimeout(timeoutInSeconds * 1000) //
				.setRequestTimeout(timeoutInSeconds * 1000) //
				.setFollowRedirect(false) //
				.setMaxRedirects(0) //
				.setMaxRequestRetry(0) //
				.setAllowPoolingConnections(pooled)//
				.setSSLContext(Trust.createSSLContext()) //
				.setAcceptAnyCertificate(true)//
				.build();
		return new AsyncHttpClient(config);
	}

	public static ClientSideServiceProvider getProvider(String base, String apiKey) {
		return new ClientSideServiceProvider(base, apiKey, ClientSideServiceProvider.defaultClient);
	}

	public static ClientSideServiceProvider getProvider(String base, String apiKey, int timeoutInSeconds) {
		return new ClientSideServiceProvider(base, apiKey, createClient(false, timeoutInSeconds));
	}

	private ClientSideServiceProvider(String base, String apiKey, AsyncHttpClient client) {
		this.base = base;
		this.apiKey = apiKey;
		this.client = client;
	}

	@Override
	public <T> T instance(Class<T> interfaceClass, String... params) {
		logger.debug("Creating with base: {}", base);
		HttpClientFactory<T, Object> factory = new HttpClientFactory<>(interfaceClass, null, base, client);
		if (remoteIps != null) {
			factory.setRemoteIps(remoteIps);
		}
		if (origin != null) {
			factory.setOrigin(origin);
		}
		return factory.syncClient(apiKey, params);
	}

	public <T, A> A instance(Class<T> interfaceClass, Class<A> asyncInterface, String... params) {
		HttpClientFactory<T, A> factory = new HttpClientFactory<>(interfaceClass, asyncInterface, base, client);
		if (remoteIps != null) {
			factory.setRemoteIps(remoteIps);
		}

		return factory.client(apiKey, params);
	}

	public ClientSideServiceProvider withRemoteIps(List<String> remotes) {
		this.remoteIps = remotes;
		return this;
	}

	public ClientSideServiceProvider setOrigin(String origin) {
		this.origin = origin;
		return this;

	}
}
