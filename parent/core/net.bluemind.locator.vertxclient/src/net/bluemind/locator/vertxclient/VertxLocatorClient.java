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
package net.bluemind.locator.vertxclient;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;

public class VertxLocatorClient implements ILocator {
	private static final Logger logger = LoggerFactory.getLogger(VertxLocatorClient.class);
	private final String login;
	private final HttpClient client;

	private static final CharSequence ORIGIN_HEADER = HttpHeaders.createOptimized("X-Bm-Origin");
	private static final CharSequence ORIGIN_VALUE = HttpHeaders
			.createOptimized(System.getProperty("net.bluemind.property.product", "unknown"));

	public VertxLocatorClient(HttpClientProvider clientProvider, String login) {
		this.login = login;
		client = clientProvider.getClient(NonOsgiActivator.get().getHost(), 8084);
	}

	public CompletableFuture<String[]> locate(String service) {
		CompletableFuture<String[]> ret = new CompletableFuture<>();
		locate(service, new AsyncHandler<String[]>() {

			@Override
			public void success(String[] value) {
				ret.complete(value);
			}

			@Override
			public void failure(Throwable e) {
				ret.completeExceptionally(e);
			}
		});
		return ret;
	}

	@Override
	public void locate(String service, final AsyncHandler<String[]> asyncHandler) {
		logger.debug("locate {} for {}", service, login);
		HttpClientRequest req = client.request(HttpMethod.GET, "/location/host/" + service + "/" + login,
				handleResponse(asyncHandler));
		// to be able to identify which component is asking for a location
		req.headers().add(ORIGIN_HEADER, ORIGIN_VALUE);
		req.exceptionHandler(new Handler<Throwable>() {

			@Override
			public void handle(Throwable e) {
				logger.error("error during locating ", e);
				asyncHandler.failure(new ServerFault("error during locating " + e.getMessage(), e));
			}
		});
		req.end();
	}

	private Handler<HttpClientResponse> handleResponse(final AsyncHandler<String[]> asyncHandler) {
		return new Handler<HttpClientResponse>() {

			@Override
			public void handle(final HttpClientResponse resp) {
				if (resp.statusCode() != 200) {
					asyncHandler.failure(new ServerFault("not found .."));
				} else {
					resp.bodyHandler(new Handler<Buffer>() {

						@Override
						public void handle(Buffer serverList) {
							asyncHandler.success(serverList.toString().split("\n"));
						}

					});
				}
			}
		};
	}

}
