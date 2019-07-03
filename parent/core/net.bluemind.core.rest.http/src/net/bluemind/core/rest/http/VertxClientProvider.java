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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.http.internal.VertxHttpClientFactory;

public class VertxClientProvider {

	private static final Logger logger = LoggerFactory.getLogger(VertxClientProvider.class);

	private static enum Port {
		bmcore(8090);
		private final int port;

		private Port(int port) {
			this.port = port;
		}

		public int getPort() {
			return port;
		}
	}

	private Map<String, String[]> hostBytagMap = new HashMap<>();
	private String key;
	private ILocator locator;
	private HttpClientProvider httpClientProvider;

	public VertxClientProvider(HttpClientProvider httpClientProvider, ILocator locator, String key) {
		this.httpClientProvider = httpClientProvider;
		this.locator = locator;
		this.key = key;

	}

	public <S, A> void service(final String tag, final Class<S> syncApi, final Class<A> asyncApi,
			final String[] paramters, final AsyncHandler<A> serviceHandler) {
		locate(tag, new AsyncHandler<String[]>() {

			@Override
			public void success(String[] hostname) {
				VertxHttpClientFactory<S, A> factory = new VertxHttpClientFactory<>(syncApi, asyncApi, "",
						httpClientProvider.getClient(hostname[0], getPort(tag)));
				serviceHandler.success(factory.client(key, paramters));
			}

			@Override
			public void failure(Throwable e) {
				serviceHandler.failure(e);

			}
		});
	}

	public <S, A> A service(String tag, final Class<S> syncApi, final Class<A> asyncApi, String... params) {
		String[] hostname = hostBytagMap.get(tag);
		logger.debug("client tag {} host : {} api : {}", tag, hostname[0], asyncApi);
		return new VertxHttpClientFactory<>(syncApi, asyncApi, "",
				httpClientProvider.getClient(hostname[0], getPort(tag))).client(key, params);
	}

	private void locate(final String tag, final AsyncHandler<String[]> asyncHandler) {
		if (hostBytagMap.containsKey(tag)) {
			asyncHandler.success(hostBytagMap.get(tag));
			return;
		}
		locator.locate(tag, new AsyncHandler.ForwardFailure<String[]>(asyncHandler) {

			@Override
			public void success(String[] value) {
				hostBytagMap.put(tag, value);
				asyncHandler.success(value);
			}
		});
	}

	private int getPort(String tag) {
		return Port.valueOf(tag.replace("/", "")).getPort();
	}

	public void setKey(String authKey) {
		this.key = authKey;

	}

	public void resolveTags(String[] tags, AsyncHandler<Void> asyncHandler) {

		List<String> toFound = new LinkedList<>(Arrays.asList(tags));
		doLocate(toFound, asyncHandler);
	}

	private void doLocate(final List<String> toFound, final AsyncHandler<Void> asyncHandler) {
		logger.debug("resolve tags {}, resolved {}", toFound, hostBytagMap);

		if (toFound.size() == 0) {
			asyncHandler.success(null);
			return;
		}
		locate(toFound.get(0), new AsyncHandler<String[]>() {

			@Override
			public void success(String[] value) {
				toFound.remove(0);
				doLocate(toFound, asyncHandler);
			}

			@Override
			public void failure(Throwable e) {
				asyncHandler.failure(e);
			}

		});
	}
}
