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
package net.bluemind.core.rest.http.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpClient;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.base.BasicClientProxy;
import net.bluemind.core.rest.base.IRestCallHandler;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;

public class LocateJITVertxHttpClientFactory<S, T> extends BasicClientProxy<S, T> {

	private static final Logger logger = LoggerFactory.getLogger(LocateJITVertxHttpClientFactory.class);

	public LocateJITVertxHttpClientFactory(Class<S> api, Class<T> asyncApi, String key,
			HttpClientProvider clientProvider, ILocator locator, String tag) {
		super(new LocatorProxyCallHandler(locator, clientProvider, tag), api, asyncApi);
	}

	public static class LocatorProxyCallHandler implements IRestCallHandler {
		private ILocator locator;

		private String tag;

		private HttpClientProvider clientProvider;

		public LocatorProxyCallHandler(ILocator locator, HttpClientProvider clientProvider, String tag) {
			this.locator = locator;
			this.clientProvider = clientProvider;
			this.tag = tag;
		}

		@Override
		public void call(final RestRequest request, final AsyncHandler<RestResponse> response) {
			logger.debug("Locating {}...", tag);
			locator.locate(tag, new AsyncHandler<String[]>() {

				@Override
				public void success(String[] hosts) {
					String host = hosts[0];
					int port = PortByTag.getPort(tag);
					HttpClient client = clientProvider.getClient(host, port);
					new VertxHttpCallHandler(client, "").call(request, response);
				}

				@Override
				public void failure(Throwable e) {
					response.failure(e);
				}

			});
		}
	}

}
