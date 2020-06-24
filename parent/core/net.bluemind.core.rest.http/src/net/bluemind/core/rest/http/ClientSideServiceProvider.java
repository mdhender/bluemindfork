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

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.uri.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import net.bluemind.core.rest.IServiceProvider;

public class ClientSideServiceProvider implements IServiceProvider {

	private static final Logger logger = LoggerFactory.getLogger(ClientSideServiceProvider.class);
	private Uri base;
	private String apiKey;
	private List<String> remoteIps;
	private String origin;
	static final AsyncHttpClient defaultClient;
	private final AsyncHttpClient client;

	static {
		defaultClient = createClient(40);
	}

	private static AsyncHttpClient createClient(int timeoutInSeconds) {
		DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
		builder.setUseNativeTransport(Epoll.isAvailable() || KQueue.isAvailable());
		int to = timeoutInSeconds * 1000;
		builder.setConnectTimeout(to).setReadTimeout(to).setRequestTimeout(to).setFollowRedirect(false);
		builder.setTcpNoDelay(true).setThreadPoolName("client-side-provider-ahc").setUseInsecureTrustManager(true);
		builder.setSoReuseAddress(true);
		builder.setMaxRequestRetry(0);
		return new DefaultAsyncHttpClient(builder.build());

	}

	public static ClientSideServiceProvider getProvider(String base, String apiKey) {
		return new ClientSideServiceProvider(base, apiKey, ClientSideServiceProvider.defaultClient);
	}

	public static ClientSideServiceProvider getProvider(String base, String apiKey, int timeoutSeconds) {
		return new ClientSideServiceProvider(base, apiKey, createClient(timeoutSeconds));
	}

	private ClientSideServiceProvider(String base, String apiKey, AsyncHttpClient client) {
		this.base = Uri.create(base);
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
