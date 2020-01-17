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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;

public class HttpClientProvider {
	private static final Logger logger = LoggerFactory.getLogger(HttpClientProvider.class);

	private Vertx vertx;
	private Map<String, HttpClient> clients = new HashMap<>();

	public HttpClientProvider(Vertx vertx) {
		this.vertx = vertx;
	}

	public HttpClient getClient(String hostname, int port) {
		String key = hostname + "-" + port;
		HttpClient ret = clients.get(key);
		if (ret == null) {
			logger.debug("create client for {}:{}", hostname, port);
			ret = vertx.createHttpClient(new HttpClientOptions().setKeepAlive(true).setTcpKeepAlive(true)
					.setTcpNoDelay(true).setMaxPoolSize(200).setDefaultHost(hostname).setDefaultPort(port));
			clients.put(key, ret);
		}

		return ret;
	}
}
