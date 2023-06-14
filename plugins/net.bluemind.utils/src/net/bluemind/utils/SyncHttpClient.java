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
package net.bluemind.utils;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;

public class SyncHttpClient {

	private SyncHttpClient() {

	}

	private static ProxySelector proxy() {
		Map<String, String> sysConfMap = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class).getValues().values;
		String proxyEnabled = sysConfMap.get(SysConfKeys.http_proxy_enabled.name());
		if (proxyEnabled == null || proxyEnabled.trim().isEmpty() || !proxyEnabled.equals("true")) {
			return ProxySelector.getDefault();
		} else {
			String host = sysConfMap.get(SysConfKeys.http_proxy_hostname.name());
			int port = Integer.parseInt(sysConfMap.get(SysConfKeys.http_proxy_port.name()));
			return ProxySelector.of(new InetSocketAddress(host, port));
		}

	}

	public static String get(String url) throws Exception {
		HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).proxy(proxy())
				.connectTimeout(Duration.ofSeconds(20)).build();
		BodyPublisher payload = BodyPublishers.noBody();
		HttpRequest httpRequest = HttpRequest.newBuilder() //
				.method("GET", payload) //
				.uri(new URI(url)) //
				.build();

		HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
		int code = httpResponse.statusCode();
		if (code != 200) {
			throw new Exception(httpResponse.body());
		}

		return httpResponse.body();
	}

}
