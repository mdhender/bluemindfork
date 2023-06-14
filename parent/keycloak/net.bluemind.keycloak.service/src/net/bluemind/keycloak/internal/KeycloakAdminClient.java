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
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.TagDescriptor;

public abstract class KeycloakAdminClient {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminClient.class);

	protected static final String BASE_URL = "http://"
			+ Topology.get().any(TagDescriptor.bm_keycloak.getTag()).value.address() + ":8099";

	protected static final String MASTER_TOKEN_URL = BASE_URL + "/realms/master/protocol/openid-connect/token";
	protected static final String REALMS_ADMIN_URL = BASE_URL + "/admin/realms";
	protected static final String REALMS_URL = REALMS_ADMIN_URL + "/%s";
	protected static final String CLIENTS_URL = REALMS_URL + "/clients";
	protected static final String CLIENTS_CREDS_URL = CLIENTS_URL + "/%s/client-secret";
	protected static final String COMPONENTS_URL = REALMS_ADMIN_URL + "/%s/components";

	protected static final int TIMEOUT = 5;

	private HttpClient cli = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
			.connectTimeout(Duration.ofSeconds(TIMEOUT)).build();

	protected CompletableFuture<JsonObject> execute(String spec, HttpMethod method) {
		return execute(spec, method, null);
	}

	protected CompletableFuture<JsonObject> execute(String spec, HttpMethod method, JsonObject body) {
		return getToken().thenCompose(token -> {
			Builder requestBuilder = HttpRequest.newBuilder(URI.create(spec));
			requestBuilder.timeout(Duration.ofSeconds(TIMEOUT));
			requestBuilder.header("Authorization", "bearer " + token);
			requestBuilder.header("Charset", StandardCharsets.UTF_8.name());
			requestBuilder.header("Content-Type", "application/json");
			if (body != null) {
				byte[] data = body.toString().getBytes();
				requestBuilder.method(method.name(), HttpRequest.BodyPublishers.ofByteArray(data));
			} else {
				requestBuilder.method(method.name(), HttpRequest.BodyPublishers.noBody());
			}
			HttpRequest request = requestBuilder.build();

			return cli.sendAsync(request, BodyHandlers.ofString()).thenApply(response -> {
				if (response.statusCode() > 400) {
					return null;
				}
				JsonObject ret = new JsonObject();
				if (!Strings.isNullOrEmpty(response.body())) {
					try {
						ret = new JsonObject(response.body());
					} catch (Exception e) {
						ret.put("results", new JsonArray(response.body()));
					}
				}
				return ret;
			}).exceptionally(t -> {
				logger.error("Failed to request {} {}: {}", method.name(), spec, t.getMessage());
				return null;
			});
		});
	}

	private CompletableFuture<String> getToken() {
		String parameters = "grant_type=password&client_id=admin-cli&username=admin&password=" + Token.admin0();
		byte[] postData = parameters.getBytes(StandardCharsets.UTF_8);

		Builder requestBuilder = HttpRequest.newBuilder(URI.create(MASTER_TOKEN_URL));
		requestBuilder.timeout(Duration.ofSeconds(TIMEOUT));
		requestBuilder.header("Charset", StandardCharsets.UTF_8.name());
		requestBuilder.header("Content-Type", "application/x-www-form-urlencoded");
		requestBuilder.method("POST", HttpRequest.BodyPublishers.ofByteArray(postData));
		HttpRequest request = requestBuilder.build();

		return cli.sendAsync(request, BodyHandlers.ofString()).thenApply(response -> {
			if (response.statusCode() > 400) {
				return null;
			}
			return new JsonObject(response.body()).getString("access_token");
		}).exceptionally(t -> {
			logger.error("Failed to fetch admin token: {}", t.getMessage());
			return null;
		});

	}

	protected JsonObject call(String callUri, HttpMethod method, JsonObject body) {
		String callUrl = BASE_URL + callUri;

		CompletableFuture<JsonObject> response = execute(callUrl, method, body);
		JsonObject json;
		try {
			json = response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
		return json;

	}

}