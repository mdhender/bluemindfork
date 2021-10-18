/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.cti.wazo.api.client.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.cti.wazo.api.client.exception.WazoApiResponseException;
import net.bluemind.cti.wazo.api.client.exception.WazoConnectionException;
import net.bluemind.cti.wazo.api.client.exception.WazoUnauthorizedException;
import net.bluemind.cti.wazo.config.WazoEndpoints;
import net.bluemind.utils.Trust;

public class HttpsWazoApiConnection implements AutoCloseable {

	private HttpsURLConnection connection;

	private String host;
	private WazoEndpoints endpoint;

	public void init(String host, WazoEndpoints endpoint) {
		this.host = host;
		this.endpoint = endpoint;
	}

	public void executePost(JsonObject payload, String authKey, String authValue) {
		openAndPreparePost(payload, authKey, authValue);
	}

	public void executeGet(String authKey, String authValue) {
		openAndPrepareGet(authKey, authValue);
	}

	private void openAndPreparePost(JsonObject payload, String authKey, String authValue) {
		openConnection(authKey, authValue);
		preparePost(payload);
	}

	private void openAndPrepareGet(String authKey, String authValue) {
		openConnection(authKey, authValue);
		prepareGet();
	}

	private void openConnection(String authKey, String authValue) {
		SSLContext context = Trust.createSSLContext();
		HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

		URL url;
		try {
			url = new URL(host.concat(endpoint.endpoint()));
			connection = (HttpsURLConnection) url.openConnection();
			connection.setHostnameVerifier(Trust.acceptAllVerifier());
			connection.setRequestProperty(authKey, authValue);
		} catch (IOException e) {
			throw new WazoConnectionException(host, endpoint, e);
		}
	}

	private void prepareGet() {
		try {
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Content-Type", "application/json");
		} catch (ProtocolException e) {
			throw new WazoConnectionException(host, endpoint, e);
		}
	}

	private void preparePost(JsonObject payload) {
		try {
			connection.setRequestMethod("POST");
			connection.setRequestProperty("accept", "application/json");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);

			if (payload != null) {
				byte[] data = payload.toString().getBytes();
				try (OutputStream os = connection.getOutputStream()) {
					os.write(data, 0, data.length);
				}
			}
		} catch (IOException e) {
			throw new WazoConnectionException(host, endpoint, e);
		}

	}

	public void manageApiResponse(int expected) {

		try {
			int responseCode = connection.getResponseCode();
			if (expected == responseCode) {
				return;
			}

			switch (responseCode) {
			case 401:
				throw new WazoUnauthorizedException(host, endpoint);
			default:
				throw new WazoApiResponseException(host, endpoint, connection.getResponseMessage());
			}
		} catch (IOException e) {
			throw new WazoConnectionException(host, endpoint, e);
		}

	}

	public String readResponse() {

		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
			StringBuilder responseStr = new StringBuilder();
			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				responseStr.append(responseLine.trim());
			}
			return responseStr.toString();
		} catch (IOException e) {
			throw new WazoApiResponseException(host, endpoint, e);
		}

	}

	@Override
	public void close() {

		try {
			if (connection != null) {
				connection.disconnect();
			}
			HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
			connection = null;
		} catch (Exception e) {
			throw new WazoConnectionException(host, endpoint, e);
		}
	}
}
