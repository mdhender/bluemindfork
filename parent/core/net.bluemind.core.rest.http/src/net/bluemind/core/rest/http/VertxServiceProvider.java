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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.vertx.java.core.http.HttpServerRequest;

import net.bluemind.core.api.BMAsyncApi;
import net.bluemind.core.rest.http.internal.LocateJITVertxHttpClientFactory;

public class VertxServiceProvider implements ITaggedServiceProvider {

	private String apiKey;
	private ILocator locator;
	private HttpClientProvider clientProvider;
	private List<String> remoteIps;

	public VertxServiceProvider(HttpClientProvider httpClientProvider, ILocator locator, String apiKey) {
		this(httpClientProvider, locator, apiKey, Collections.emptyList());
	}

	public VertxServiceProvider from(HttpServerRequest req) {
		List<String> forwadedFor = new ArrayList<>(req.headers().getAll("X-Forwarded-For"));
		forwadedFor.add(req.remoteAddress().getAddress().getHostAddress());
		this.remoteIps = forwadedFor;
		return this;

	}

	public VertxServiceProvider(HttpClientProvider httpClientProvider, ILocator locator, String apiKey,
			List<String> remoteIps) {
		this.locator = locator;
		this.apiKey = apiKey;
		this.clientProvider = httpClientProvider;
		this.remoteIps = remoteIps;
	}

	@Override
	public <A> A instance(String tag, Class<A> interfaceClass, String... params) {
		Class<?> syncApi = interfaceClass.getAnnotation(BMAsyncApi.class).value();
		LocateJITVertxHttpClientFactory<?, A> factory = new LocateJITVertxHttpClientFactory<>(syncApi, interfaceClass,
				apiKey, clientProvider, locator, tag);
		factory.setRemoteIps(remoteIps);
		return factory.client(apiKey, params);
	}

}
