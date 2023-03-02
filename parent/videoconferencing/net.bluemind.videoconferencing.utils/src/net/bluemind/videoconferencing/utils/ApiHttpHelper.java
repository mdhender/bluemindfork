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
package net.bluemind.videoconferencing.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.netty.handler.codec.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;

public class ApiHttpHelper {

	private static final Logger logger = LoggerFactory.getLogger(ApiHttpHelper.class);
	private final String bearerToken;

	private static final int TIMEOUT = 10000;

	public ApiHttpHelper(String bearerToken) {
		this.bearerToken = bearerToken;
	}

	private ProxySelector proxy() throws MalformedURLException, IOException {
		Map<String, String> sysConfMap = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class).getValues().values;
		String proxyEnabled = sysConfMap.get(SysConfKeys.http_proxy_enabled.name());
		if (proxyEnabled == null || proxyEnabled.trim().isEmpty() || !proxyEnabled.equals("true")) {
			return ProxySelector.getDefault();
		} else {
			String host = sysConfMap.get(SysConfKeys.http_proxy_hostname.name());
			int port = Integer.valueOf(sysConfMap.get(SysConfKeys.http_proxy_port.name()));
			return ProxySelector.of(new InetSocketAddress(host, port));
		}

	}

	public JsonObject execute(String spec, HttpMethod method, String body) {
		try {
			HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).proxy(proxy())
					.connectTimeout(Duration.ofSeconds(20)).build();
			BodyPublisher payload = body != null ? HttpRequest.BodyPublishers.ofString(body) : BodyPublishers.noBody();
			HttpRequest httpRequest = HttpRequest.newBuilder() //
					.method(method.name(), payload) //
					.header("Authorization", "Bearer " + bearerToken) //
					.header("Accept", "application/json") //
					.header("Content-Type", "application/json; utf-8") //
					.uri(new URI(spec)) //
					.build();

			HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
			int code = httpResponse.statusCode();
			if (code >= 400) {
				throw new ServerFault(httpResponse.body());
			}

			String retBody = httpResponse.body();
			if (Strings.isNullOrEmpty(retBody)) {
				return new JsonObject();
			}
			return new JsonObject(retBody);
		} catch (Exception e) {
			logger.warn("Exception while calling {}:{}", spec, method.name(), e);
			throw new ServerFault(e.getMessage());
		}

	}
}
