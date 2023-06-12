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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.lib.vertx.VertxPlatform;
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

	public KeycloakAdminClient() {

	}

	protected CompletableFuture<JsonObject> execute(String spec, HttpMethod method) {
		return execute(spec, method, null);
	}

	protected CompletableFuture<JsonObject> execute(String spec, HttpMethod method, JsonObject body) {
		CompletableFuture<JsonObject> future = new CompletableFuture<>();
		getToken().thenAccept(token -> {
			try {
				URI uri = new URI(spec);
				HttpClient client = initHttpClient(uri);
				client.request(method, uri.getPath() + (uri.getQuery() != null ? "?" + uri.getQuery() : ""),
						reqHandler -> {
							if (reqHandler.succeeded()) {
								HttpClientRequest r = reqHandler.result();
								r.response(responseHandler(future, uri));
								MultiMap headers = r.headers();
								headers.add(HttpHeaders.AUTHORIZATION,
										String.format("bearer %s", token.getString("access_token")));
								headers.add(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
								headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
								if (body != null) {
									byte[] data = body.toString().getBytes();
									headers.add(HttpHeaders.CONTENT_LENGTH, Integer.toString(data.length));
									r.write(Buffer.buffer(data));
								}
								r.end();
							}
						});
			} catch (Exception e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	private CompletableFuture<JsonObject> getToken() {
		CompletableFuture<JsonObject> future = new CompletableFuture<>();

		try {
			URI uri = new URI(MASTER_TOKEN_URL);
			HttpClient client = initHttpClient(uri);
			client.request(HttpMethod.POST, uri.getPath(), reqHandler -> {
				if (reqHandler.succeeded()) {
					HttpClientRequest r = reqHandler.result();
					r.response(responseHandler(future, uri));
					MultiMap headers = r.headers();
					headers.add(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
					headers.add(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
					String params = "grant_type=password&client_id=admin-cli&username=admin&password=" + Token.admin0();
					byte[] postData = params.getBytes(StandardCharsets.UTF_8);
					headers.add(HttpHeaders.CONTENT_LENGTH, Integer.toString(postData.length));
					r.write(Buffer.buffer(postData));
					r.end();
				} else {
					future.completeExceptionally(reqHandler.cause());
				}

			});

		} catch (Exception e) {
			future.completeExceptionally(e);
		}
		return future;
	}

	private Handler<AsyncResult<HttpClientResponse>> responseHandler(CompletableFuture<JsonObject> future, URI uri) {
		return respHandler -> {
			if (respHandler.succeeded()) {
				HttpClientResponse resp = respHandler.result();
				if (resp.statusCode() > 400) {
					future.complete(null);
					if (logger.isWarnEnabled()) {
						logger.warn("Failed to perform request {}: {}", uri, resp.statusMessage());
					}
					return;
				}
				resp.body(body -> {
					if (body.result().length() > 0) {
						try {
							future.complete(new JsonObject(body.result()));
						} catch (Exception e) {

							JsonObject json = new JsonObject();
							json.put("results", new JsonArray(body.result()));
							future.complete(json);
						}

					} else {
						future.complete(null);
					}
				});
			} else {
				future.completeExceptionally(respHandler.cause());
			}
		};

	}

	private static HttpClient initHttpClient(URI uri) {
		HttpClientOptions opts = new HttpClientOptions();
		opts.setDefaultHost(uri.getHost());
		opts.setSsl(uri.getScheme().equalsIgnoreCase("https"));
		opts.setDefaultPort(
				uri.getPort() != -1 ? uri.getPort() : (uri.getScheme().equalsIgnoreCase("https") ? 443 : 80));
		if (opts.isSsl()) {
			opts.setTrustAll(true);
			opts.setVerifyHost(false);
		}
		return VertxPlatform.getVertx().createHttpClient(opts);
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