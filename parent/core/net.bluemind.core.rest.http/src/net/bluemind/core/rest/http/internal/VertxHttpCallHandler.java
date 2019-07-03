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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.streams.Pump;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.base.IRestCallHandler;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;

public class VertxHttpCallHandler implements IRestCallHandler {

	private HttpClient client;
	private String baseUrl;

	public VertxHttpCallHandler(HttpClient client, String baseUrl) {
		this.client = client;
		this.baseUrl = baseUrl;
	}

	@Override
	public void call(RestRequest request, final AsyncHandler<RestResponse> response) {
		String p = baseUrl + request.path;
		String q = "";
		for (Map.Entry<String, String> entry : request.params.entries()) {
			if (q.isEmpty()) {
				q = q + "?";
			} else {
				q += "&";
			}

			String encoded = null;
			try {
				encoded = URLEncoder.encode(entry.getValue(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				encoded = entry.getValue();
			}
			q += entry.getKey() + "=" + encoded;
		}

		p += q;
		Handler<HttpClientResponse> responseHandler = new Handler<HttpClientResponse>() {

			@Override
			public void handle(final HttpClientResponse resp) {
				if ("chunked".equals(resp.headers().get("Transfer-Encoding"))) {
					RestResponse rr = RestResponse.stream(resp);
					rr.headers = resp.headers();
					response.success(rr);
				} else {
					resp.bodyHandler(buffer -> {
						response.success(RestResponse.ok(resp.headers(), resp.statusCode(), buffer));
					});
				}
			}
		};

		final HttpClientRequest req = client.request(request.method, p, responseHandler);
		req.headers().add(request.headers);

		req.headers().add("X-Forwarded-For", request.remoteAddresses);

		if (request.origin != null) {
			req.headers().add("X-BM-Origin", request.origin);
		}

		req.exceptionHandler(e -> response.failure(e));
		if (request.body != null) {
			req.end(request.body);
		} else if (request.bodyStream != null) {
			req.setChunked(true);
			request.bodyStream.endHandler(v -> req.end());
			Pump.createPump(request.bodyStream, req).start();
		} else {
			req.end();
		}
	}

}
