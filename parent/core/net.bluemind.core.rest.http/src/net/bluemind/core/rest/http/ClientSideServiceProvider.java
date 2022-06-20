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

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.uri.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;

import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import net.bluemind.core.rest.IServiceProvider;

public class ClientSideServiceProvider implements IServiceProvider {

	private static final Logger logger = LoggerFactory.getLogger(ClientSideServiceProvider.class);
	private Uri base;
	private String apiKey;
	private List<String> remoteIps;
	private String origin;
	static final Supplier<AsyncHttpClient> defaultClient;
	static final Supplier<AsyncHttpClient> longRequestClient;
	static final Supplier<AsyncHttpClient> infiniteClient;
	private final AsyncHttpClient client;

	public static final int TIMEOUT_LONG_REQUEST = (int) TimeUnit.HOURS.toSeconds(4);
	public static final int TIMEOUT_INFINITE_REQUEST = -1;

	static {
		defaultClient = Suppliers.memoize(() -> createClient(40, 40, 40));
		longRequestClient = Suppliers.memoize(() -> createClient(40, TIMEOUT_LONG_REQUEST, TIMEOUT_LONG_REQUEST));
		infiniteClient = Suppliers.memoize(() -> createClient(40, TIMEOUT_INFINITE_REQUEST, TIMEOUT_INFINITE_REQUEST));
	}

	private static final boolean EPOLL_DISABLED = new File("/etc/bm/netty.epoll.disabled").exists();

	private static AsyncHttpClient createClient(int connectTimeoutSeconds, int readTimeoutSeconds,
			int requestTimeoutSeconds) {
		DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
		builder.setUseNativeTransport((Epoll.isAvailable() || KQueue.isAvailable()) && !EPOLL_DISABLED);
		builder.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(connectTimeoutSeconds));
		builder.setReadTimeout((int) (readTimeoutSeconds == -1 ? TIMEOUT_INFINITE_REQUEST
				: TimeUnit.SECONDS.toMillis(readTimeoutSeconds)));
		builder.setRequestTimeout((int) (requestTimeoutSeconds == -1 ? TIMEOUT_INFINITE_REQUEST
				: TimeUnit.SECONDS.toMillis(requestTimeoutSeconds)));
		builder.setFollowRedirect(false);
		builder.setTcpNoDelay(true);
		builder.setThreadPoolName("client-side-provider-ahc");
		builder.setUseInsecureTrustManager(true);
		builder.setSoReuseAddress(true);
		builder.setHttpAdditionalChannelInitializer(ch -> {
			ch.config().setOption(ChannelOption.TCP_FASTOPEN_CONNECT, true);
		});
		builder.setMaxRequestRetry(0);
		return new DefaultAsyncHttpClient(builder.build());
	}

	public static ClientSideServiceProvider getProvider(String base, String apiKey) {
		return new ClientSideServiceProvider(base, apiKey, ClientSideServiceProvider.defaultClient.get());
	}

	public static ClientSideServiceProvider getProvider(String base, String apiKey, int connectTimeoutSeconds,
			int readTimeoutSeconds, int requestTimeoutSeconds) {
		AsyncHttpClient client;
		if (readTimeoutSeconds == TIMEOUT_INFINITE_REQUEST && requestTimeoutSeconds == TIMEOUT_INFINITE_REQUEST) {
			client = infiniteClient.get();
		} else if (readTimeoutSeconds == requestTimeoutSeconds && readTimeoutSeconds == TIMEOUT_LONG_REQUEST) {
			client = longRequestClient.get();
		} else {
			logger.warn(
					"Using a non standard read timeout {}s (request timeout: {}s): AHC client will not be closed and will LEAK!",
					readTimeoutSeconds, requestTimeoutSeconds, new Throwable());
			client = createClient(connectTimeoutSeconds, readTimeoutSeconds, requestTimeoutSeconds);
		}
		return new ClientSideServiceProvider(base, apiKey, client);
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
