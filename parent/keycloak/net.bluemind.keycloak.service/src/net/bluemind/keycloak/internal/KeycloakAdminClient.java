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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.keycloak.internal;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.google.common.base.Strings;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.TagDescriptor;

public abstract class KeycloakAdminClient {
	protected static final String BASE_URL = "http://"
			+ Topology.get().any(TagDescriptor.bm_keycloak.getTag()).value.address() + ":8099";

	protected static final String MASTER_TOKEN_URL = BASE_URL + "/realms/master/protocol/openid-connect/token";
	protected static final String REALMS_ADMIN_URL = BASE_URL + "/admin/realms";
	protected static final String REALMS_URL = REALMS_ADMIN_URL + "/%s";
	protected static final String CLIENTS_URL = REALMS_URL + "/clients";
	protected static final String CLIENTS_CREDS_URL = CLIENTS_URL + "/%s/client-secret";
	protected static final String COMPONENTS_URL = REALMS_ADMIN_URL + "/%s/components";

	private static final int TIMEOUT = 5000;

	public KeycloakAdminClient() {
	}

	protected JsonObject execute(String spec, String method) {
		return execute(spec, method, null);
	}

	protected JsonObject execute(String spec, String method, JsonObject body) {
		try {

			Builder requestBuilder = HttpRequest.newBuilder(new URI(spec));
			requestBuilder.timeout(Duration.of(TIMEOUT, ChronoUnit.MILLIS));

			requestBuilder.header("Authorization", "bearer " + getToken());
			requestBuilder.header("Charset", StandardCharsets.UTF_8.name());
			requestBuilder.header("Content-Type", "application/json");

			if (body != null) {
				byte[] data = body.toString().getBytes();
				requestBuilder.method(method, HttpRequest.BodyPublishers.ofByteArray(data));
			} else {
				requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
			}
			HttpRequest request = requestBuilder.build();
			HttpClient cli = HttpClient.newHttpClient();
			HttpResponse<String> resp = cli.send(request, BodyHandlers.ofString());
			JsonObject ret = new JsonObject();
			ret.put("statusCode", resp.statusCode());
			if (!Strings.isNullOrEmpty(resp.body())) {
				try {
					ret.put("body", new JsonObject(resp.body()));
				} catch (Exception e) {
					ret.put("body", new JsonArray(resp.body()));
				}
			}

			return ret;
		} catch (Exception e) {
			throw new ServerFault(e.getMessage());
		}

	}

	private String getToken() {
		String parameters = "grant_type=password&client_id=admin-cli&username=admin&password=" + Token.admin0();
		byte[] postData = parameters.getBytes(StandardCharsets.UTF_8);

		try {
			Builder requestBuilder = HttpRequest.newBuilder(new URI(MASTER_TOKEN_URL));
			requestBuilder.timeout(Duration.of(TIMEOUT, ChronoUnit.MILLIS));

			requestBuilder.header("Charset", StandardCharsets.UTF_8.name());
			requestBuilder.header("Content-Type", "application/x-www-form-urlencoded");

			requestBuilder.method("POST", HttpRequest.BodyPublishers.ofByteArray(postData));
			HttpRequest request = requestBuilder.build();
			HttpClient cli = HttpClient.newHttpClient();

			HttpResponse<String> resp = cli.send(request, BodyHandlers.ofString());

			return new JsonObject(resp.body()).getString("access_token");
		} catch (Exception e) {
			throw new ServerFault(e.getMessage());
		}
	}

}