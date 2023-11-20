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
package net.bluemind.elastic.topology.service.esclient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;

public class DumbElasticClient implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(DumbElasticClient.class);
	private final HttpClient client;
	private final String uriBase;

	public DumbElasticClient(String host) {
		this(host, 9200);
	}

	public DumbElasticClient(String host, int port) {
		this.client = HttpClient.newBuilder().followRedirects(Redirect.NEVER).build();
		this.uriBase = "http://%s:%d".formatted(host, port);
	}

	public static record ClusterId(String name, String uuid) {
	}

	public ClusterId clusterUUID() {
		return getJson("", jsResp -> new ClusterId(jsResp.getString("cluster_name"), jsResp.getString("cluster_uuid")));
	}

	private <T> T getJson(String uriSuffix, Function<JsonObject, T> mapper) {
		try {
			HttpRequest req = HttpRequest.newBuilder()//
					.version(Version.HTTP_1_1)//
					.GET()//
					.header("Accept", "application/json")//
					.uri(new URI("%s%s".formatted(uriBase, uriSuffix))).build();
			HttpResponse<String> result = client.send(req, BodyHandlers.ofString());
			JsonObject jsResp = new JsonObject(result.body());
			return mapper.apply(jsResp);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DumbClientException();
		} catch (Exception e) {
			throw new DumbClientException();
		}
	}

	private void putJson(String uriSuffix, Supplier<String> encodedJson) {
		String uri = "%s%s".formatted(uriBase, uriSuffix);
		try {
			HttpRequest req = HttpRequest.newBuilder()//
					.version(Version.HTTP_1_1)//
					.PUT(BodyPublishers.ofString(encodedJson.get()))//
					.header("Content-Type", "application/json")//
					.uri(new URI(uri))//
					.build();
			HttpResponse<String> result = client.send(req, BodyHandlers.ofString());
			logger.debug("PUT {} -> {}", uri, result.statusCode());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DumbClientException();
		} catch (Exception e) {
			throw new DumbClientException();
		}
	}

	/**
	 * This generates a 404 when we don't have any index
	 * 
	 * @param extraCopies
	 */
	public void setNumberOfCopies(int extraCopies) {
		JsonObject js = new JsonObject().put("index", new JsonObject().put("number_of_replicas", extraCopies));
		putJson("/_settings", js::encode);
	}

	@Override
	public void close() {
		client.close();
	}

}
