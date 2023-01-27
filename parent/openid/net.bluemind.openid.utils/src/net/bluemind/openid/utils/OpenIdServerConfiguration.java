/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.openid.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;

public class OpenIdServerConfiguration {

	private static Map<String, JsonObject> configuration = new HashMap<>();
	private static Map<String, Map<String, String>> domainSettings = new HashMap<>();

	private OpenIdServerConfiguration() {

	}

	public static void setDomainSettings(String domainId, Map<String, String> settings) {
		domainSettings.put(domainId, settings);
	}

	public static JsonObject get(String domainId) {

		Map<String, String> settings = domainSettings.get(domainId);
		String realm = settings.get("openid_realm");

		if (!configuration.containsKey(realm)) {
			String host = settings.get("openid_host");
			String clientId = settings.get("openid_client_id");
			String clientSecret = settings.get("openid_client_secret");

			if (Strings.isNullOrEmpty(host)) {
				throw new ServerFault("Invalid OpenId configuration");
			}

			try {
				Builder requestBuilder = HttpRequest.newBuilder(new URI(host));
				requestBuilder.GET().build();
				HttpRequest request = requestBuilder.build();
				HttpClient cli = HttpClient.newHttpClient();
				HttpResponse<String> resp = cli.send(request, BodyHandlers.ofString());
				JsonObject conf = new JsonObject(resp.body());
				conf.put("clientId", clientId);
				conf.put("clientSecret", clientSecret);

				configuration.put(realm, conf);
			} catch (Exception e) {
				throw new ServerFault(e.getMessage());
			}

		}

		return configuration.get(realm);
	}

	public static void invalidate(String domainId) {
		configuration.remove(domainId);
		domainSettings.remove(domainId);
	}

}
