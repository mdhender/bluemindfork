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

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.uri.Uri;

import net.bluemind.core.rest.base.BasicClientProxy;
import net.bluemind.core.rest.http.internal.AsyncHttpCallHandler;

public class HttpClientFactory<S, T> extends BasicClientProxy<S, T> {

	public HttpClientFactory(Class<S> api, Class<T> asyncApi, Uri baseUrl) {
		this(api, asyncApi, baseUrl, ahc());
	}

	private static AsyncHttpClient ahc() {
		return ClientSideServiceProvider.defaultClient.get();
	}

	HttpClientFactory(Class<S> api, Class<T> asyncApi, Uri baseUrl, AsyncHttpClient client) {
		super(new AsyncHttpCallHandler(client, baseUrl), api, asyncApi);
	}

	public static <S, T> HttpClientFactory<S, T> create(Class<S> api, Class<T> asyncApi, String baseUrl,
			AsyncHttpClient client) {
		return create(api, asyncApi, Uri.create(baseUrl), client);
	}

	public static <S, T> HttpClientFactory<S, T> create(Class<S> api, Class<T> asyncApi, Uri baseUrl,
			AsyncHttpClient client) {
		return new HttpClientFactory<>(api, asyncApi, baseUrl, client);
	}

	public static <S, T> HttpClientFactory<S, T> create(Class<S> api, Class<T> asyncApi, String baseUrl) {
		return create(api, asyncApi, Uri.create(baseUrl));
	}

	public static <S, T> HttpClientFactory<S, T> create(Class<S> api, Class<T> asyncApi, Uri baseUrl) {
		return new HttpClientFactory<>(api, asyncApi, baseUrl, ahc());
	}

}
