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
package net.bluemind.lib.grafana.config;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.netty.handler.codec.http.HttpMethod;
import net.bluemind.lib.grafana.client.GrafanaService;
import net.bluemind.lib.grafana.exception.GrafanaException;
import net.bluemind.lib.grafana.exception.GrafanaServerException;
import net.bluemind.lib.grafana.utils.ApiHttpHelper;

public class GrafanaConnection {
	public static final Logger logger = LoggerFactory.getLogger(GrafanaConnection.class);

	public final String host;
	public final Integer port;
	public final String apiKey;
	private ServiceAccountInfo serviceAccountInfo;
	public final String userInfo;

	public GrafanaConnection(String userInfo, String host, Integer port, String token, Integer accountId,
			String accountName) throws GrafanaException {
		this.userInfo = userInfo;
		this.host = host;
		this.port = port;
		this.serviceAccountInfo = new ServiceAccountInfo();
		this.apiKey = Strings.isNullOrEmpty(token) ? refreshToken(accountName, accountId) : token;
	}

	public Integer tokenId() {
		return this.serviceAccountInfo.tokenId();
	}

	public String tokenKey() {
		return this.serviceAccountInfo.tokenKey();
	}

	public Integer accountId() {
		return this.serviceAccountInfo.accountId();
	}

	public String accountName() {
		return this.serviceAccountInfo.accountName();
	}

	public boolean withServiceAccount() {
		return this.serviceAccountInfo.accountInit() && this.serviceAccountInfo.accountTokenInit();
	}

	private String refreshToken(String accountName, Integer accountId) throws GrafanaException {
		if (accountId != null && getServiceAccount(accountId) && this.serviceAccountInfo.accountInit()) {
			createServiceAccountToken();
		} else {
			createServiceAccount(accountName);
			createServiceAccountToken();
		}

		if (Strings.isNullOrEmpty(serviceAccountInfo.tokenKey())) {
			throw new GrafanaException("Cannot continue without token");
		}
		return serviceAccountInfo.tokenKey();
	}

	private boolean getServiceAccount(Integer accountId) throws GrafanaException {
		HttpResponse<String> response = executeGetRequest(GrafanaService.serviceAccountsPath(accountId));
		this.serviceAccountInfo.setServiceAccount(response.body());
		return response.statusCode() <= 201;
	}

	private void createServiceAccount(String accountName) throws GrafanaException {
		String body = "{\"name\":\"" + accountName + "\", \"role\":\"Admin\"}";
		HttpResponse<String> response = executePostRequest(GrafanaService.serviceAccountsPath(), body);
		this.serviceAccountInfo.setServiceAccount(response.body());
	}

	private void createServiceAccountToken() throws GrafanaException {
		String body = "{\"name\": \"" + serviceAccountInfo.accountName() + "-token\"}";
		String path = GrafanaService.serviceAccountsTokenPath(serviceAccountInfo.accountId());
		HttpResponse<String> response = executePostRequest(path, body);
		this.serviceAccountInfo.setServiceAccountToken(response.body());
	}

	private HttpResponse<String> executePostRequest(String path, String body) throws GrafanaException {
		try {
			HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
			URI uri = new URI("http", userInfo, host, port, path, null, null);
			HttpRequest httpRequest = HttpRequest.newBuilder() //
					.method(HttpMethod.POST.name(), HttpRequest.BodyPublishers.ofString(body)) //
					.header("Content-Type", "application/json") //
					.header("Authorization", "Basic " + encodedUserInfo()) //
					.uri(uri) //
					.build();
			HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
			ApiHttpHelper.checkResponse(httpResponse);

			return httpResponse;
		} catch (ConnectException ce) {
			throw new GrafanaServerException(String.format("Grafana server %s:%d cannot be reached.", host, port));
		} catch (URISyntaxException | IOException | InterruptedException e) {
			logger.error("Exception while calling {}:{}", path, HttpMethod.POST.name(), e);
			throw new GrafanaException(e);
		}
	}

	private HttpResponse<String> executeGetRequest(String path) throws GrafanaException {
		try {
			HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
			URI uri = new URI("http", userInfo, host, port, path, null, null);
			HttpRequest httpRequest = HttpRequest.newBuilder() //
					.method(HttpMethod.GET.name(), BodyPublishers.noBody()) //
					.header("Content-Type", "application/json") //
					.header("Authorization", "Basic " + encodedUserInfo()) //
					.uri(uri) //
					.build();

			HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
			ApiHttpHelper.checkResponse(httpResponse);

			return httpResponse;
		} catch (ConnectException ce) {
			throw new GrafanaServerException(String.format("Grafana server %s:%d cannot be reached.", host, port));
		} catch (URISyntaxException | IOException | InterruptedException e) {
			throw new GrafanaException(e);
		}
	}

	private String encodedUserInfo() {
		return Base64.getEncoder().encodeToString(userInfo.getBytes());
	}

}
