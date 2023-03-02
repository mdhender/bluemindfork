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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.videoconferencing.webex.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.netty.handler.codec.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.videoconferencing.webex.WebexProvider;
import net.bluemind.videoconferencing.webex.dto.WebexConference;
import net.bluemind.videoconferencing.webex.dto.WebexDialInfo;

public class WebexConferenceClient {

	private final String bearerToken;
	private final BmContext su;
	private static final int TIMEOUT = 10000;
	private static final String apiEndpoint = "https://webexapis.com/v1/";
	private static final String meetingsPath = "meetings";
	private static final Logger logger = LoggerFactory.getLogger(WebexConferenceClient.class);

	public WebexConferenceClient(BmContext context) {
		this.bearerToken = context.getSecurityContext().getUserAccessToken(WebexProvider.ID).get().token;
		this.su = context.su();
	}

	public WebexDialInfo create(WebexConference conference) {
		String url = apiEndpoint + meetingsPath;
		JsonObject response = execute(url, HttpMethod.POST, conference.toJson());
		return WebexDialInfo.fromJson(response);
	}

	public WebexDialInfo update(String conferenceId, WebexConference conference) {
		String url = apiEndpoint + meetingsPath + "/" + conferenceId;
		JsonObject response = execute(url, HttpMethod.PATCH, conference.toJson());
		return WebexDialInfo.fromJson(response);
	}

	public void delete(String conferenceId) {
		String url = apiEndpoint + meetingsPath + "/" + conferenceId;
		execute(url, HttpMethod.DELETE, null);
	}

	private HttpURLConnection connect(String url) throws MalformedURLException, IOException {
		Map<String, String> sysConfMap = su.provider().instance(ISystemConfiguration.class).getValues().values;
		String proxyEnabled = sysConfMap.get(SysConfKeys.http_proxy_enabled.name());
		if (proxyEnabled == null || proxyEnabled.trim().isEmpty() || !proxyEnabled.equals("true")) {
			return (HttpURLConnection) new URL(url).openConnection();
		} else {
			Proxy proxy = new Proxy(Proxy.Type.HTTP,
					new InetSocketAddress(sysConfMap.get(SysConfKeys.http_proxy_hostname.name()),
							Integer.valueOf(sysConfMap.get(SysConfKeys.http_proxy_port.name()))));
			return (HttpURLConnection) new URL(url).openConnection(proxy);
		}

	}

	protected JsonObject execute(String spec, HttpMethod method, String body) {
		HttpURLConnection conn = null;
		try {
			conn = connect(spec);
			conn.setRequestMethod(method.name());
			conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json; utf-8");
			conn.setConnectTimeout(TIMEOUT);
			conn.setReadTimeout(TIMEOUT);

			if (body != null) {
				conn.setDoOutput(true);
				byte[] data = body.getBytes();
				try (OutputStream os = conn.getOutputStream()) {
					os.write(data, 0, data.length);
				}
			}

			conn.disconnect();

			int responseCode = conn.getResponseCode();
			if (responseCode >= 400) {
				String response = getResponse(conn.getErrorStream());
				throw new ServerFault(response);
			}

			String response = getResponse(conn.getInputStream());

			if (!Strings.isNullOrEmpty(response)) {
				return new JsonObject(response);
			}

			return new JsonObject();

		} catch (Exception e) {
			logger.warn("Exception while calling {}:{}", spec, method.name(), e);
			throw new ServerFault(e.getMessage());
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

	}

	private String getResponse(InputStream is) throws IOException, UnsupportedEncodingException {
		StringBuilder response = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8.name()))) {
			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
		}
		return response.toString();
	}

}
