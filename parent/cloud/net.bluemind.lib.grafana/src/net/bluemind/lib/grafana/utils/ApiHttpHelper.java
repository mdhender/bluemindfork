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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpMethod;
import net.bluemind.lib.grafana.exception.GrafanaBadRequestException;
import net.bluemind.lib.grafana.exception.GrafanaException;
import net.bluemind.lib.grafana.exception.GrafanaNotFoundException;
import net.bluemind.lib.grafana.exception.GrafanaPreconditionException;
import net.bluemind.lib.grafana.exception.GrafanaServerException;
import net.bluemind.lib.grafana.exception.GrafanaUnauthorizedException;

public class ApiHttpHelper {

	private static final Logger logger = LoggerFactory.getLogger(ApiHttpHelper.class);

	private final String host;
	private final String apiKey;

	public ApiHttpHelper(String host, String apiKey) {
		this.host = host;
		this.apiKey = apiKey;
	}

	public String execute(String spec, HttpMethod method, String body) throws GrafanaException, InterruptedException {
		try {
			HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
			BodyPublisher payload = body != null ? HttpRequest.BodyPublishers.ofString(body) : BodyPublishers.noBody();
			URI uri = new URI("http", null, host, 3000, spec, null, null);
			HttpRequest httpRequest = HttpRequest.newBuilder() //
					.method(method.name(), payload) //
					.header("Authorization", "Bearer " + apiKey) //
					.header("Accept", "application/json") //
					.header("Content-Type", "application/json") //
					.uri(uri) //
					.build();

			HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
			checkResponse(httpResponse);

			return httpResponse.body();
		} catch (URISyntaxException | IOException e) {
			logger.error("Exception while calling {}:{}", spec, method.name(), e);
			throw new GrafanaException(e);
		}

	}

	public static void checkResponse(HttpResponse<String> response) throws GrafanaException {

		switch (response.statusCode()) {
		case 400: {
			throw new GrafanaBadRequestException(response.body());
		}
		case 401, 403: {
			throw new GrafanaUnauthorizedException(response.body());
		}
		case 404: {
			throw new GrafanaNotFoundException(response.body());
		}
		case 412: {
			throw new GrafanaPreconditionException(response.body());
		}
		case 500, 502, 503, 504: {
			throw new GrafanaServerException(response.body());
		}
		default:
			if (response.statusCode() > 400) {
				throw new GrafanaException(response.body());
			}
			break;
		}
	}
}
