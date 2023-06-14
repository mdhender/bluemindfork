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
package net.bluemind.lib.grafana.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpMethod;

public class ApiHttpHelper {

	private static final Logger logger = LoggerFactory.getLogger(ApiHttpHelper.class);

	private static final int TIMEOUT = 10000;
	final String host;
	final String apiKey;

	public ApiHttpHelper(String host, String apiKey) {
		this.host = host;
		this.apiKey = apiKey;
	}

	public String execute(String spec, HttpMethod method, String body) throws Exception {
		try {
			HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
			BodyPublisher payload = body != null ? HttpRequest.BodyPublishers.ofString(body) : BodyPublishers.noBody();
			URI uri = new URI("http", "admin:admin", host, 3000, spec, null, null);
			HttpRequest httpRequest = HttpRequest.newBuilder() //
					.method(method.name(), payload) //
					.header("Authorization", "Bearer " + apiKey) //
					.header("Accept", "application/json") //
					.header("Content-Type", "application/json") //
					.uri(uri) //
					.build();

			HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
			int code = httpResponse.statusCode();
			if (code >= 400) {
				return null;
			}

			return httpResponse.body();
		} catch (Exception e) {
			logger.warn("Exception while calling {}:{}", spec, method.name(), e);
			throw e;
		}

	}
}
