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
package net.bluemind.videoconferencing.starleaf.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

import com.google.common.base.Strings;

import io.netty.handler.codec.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;

public abstract class SLClient {

	protected static final String BASE_URL = "https://api.starleaf.com/v1/";
	private static final int TIMEOUT = 5000;

	private String token;

	public SLClient(String token) {
		this.token = token;

	}

	protected JsonObject execute(String spec, HttpMethod method) {
		return execute(spec, method, null);
	}

	protected JsonObject execute(String spec, HttpMethod method, JsonObject body) {

		try {
			URL url = new URL(spec);
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod(method.name());
			conn.setRequestProperty("X-SL-AUTH-TOKEN", token);
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json; utf-8");
			conn.setConnectTimeout(TIMEOUT);
			conn.setReadTimeout(TIMEOUT);

			if (body != null) {
				conn.setDoOutput(true);
				byte[] data = body.toString().getBytes();
				try (OutputStream os = conn.getOutputStream()) {
					os.write(data, 0, data.length);
				}
			}

			conn.disconnect();

			int responseCode = conn.getResponseCode();
			if (responseCode >= 400) {
				StringBuilder response = getResponse(conn.getErrorStream());
				throw new ServerFault(response.toString());
			}

			StringBuilder response = getResponse(conn.getInputStream());
			if (!Strings.isNullOrEmpty(response.toString())) {
				return new JsonObject(response.toString());
			}

			return new JsonObject();

		} catch (Exception e) {
			throw new ServerFault(e.getMessage());
		}

	}

	private StringBuilder getResponse(InputStream is) throws IOException, UnsupportedEncodingException {
		StringBuilder response = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8.name()))) {
			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
		}
		return response;
	}

}
