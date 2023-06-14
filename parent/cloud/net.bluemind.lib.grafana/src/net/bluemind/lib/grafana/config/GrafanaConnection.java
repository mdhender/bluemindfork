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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.netty.handler.codec.http.HttpMethod;

public class GrafanaConnection {
	public static final Logger logger = LoggerFactory.getLogger(GrafanaConnection.class);

	private static final String SERVICE_ACCOUNT_PATH = "/api/serviceaccounts/";

	public final String host;
	public final String apiKey;
	private ServiceAccountInfo serviceAccountInfo;

	public GrafanaConnection(String host, String token, Integer accountId, String accountName) throws Exception {
		this.host = host;
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

	private String refreshToken(String accountName, Integer accountId) throws Exception {
		if (accountId != null && getServiceAccount(accountId) && this.serviceAccountInfo.accountInit()) {
			createServiceAccountToken();
		} else {
			createServiceAccount(accountName);
			createServiceAccountToken();
		}
		return serviceAccountInfo.tokenKey();
	}

	private boolean getServiceAccount(Integer accountId) throws Exception {
		HttpResponse<String> response = executeGetRequest(SERVICE_ACCOUNT_PATH + accountId);
		boolean responseStatus = response != null && response.statusCode() <= 201;
		if (responseStatus) {
			this.serviceAccountInfo.setServiceAccount(response.body());
		}
		return responseStatus;
	}

	private void createServiceAccount(String accountName) throws Exception {
		String body = "{\"name\":\"" + accountName + "\", \"role\":\"Admin\"}";
		HttpResponse<String> response = executePostRequest(SERVICE_ACCOUNT_PATH, body);
		boolean responseStatus = response != null && response.statusCode() <= 201;
		if (responseStatus) {
			this.serviceAccountInfo.setServiceAccount(response.body());
		}
	}

	private void createServiceAccountToken() throws Exception {
		String body = "{\"name\": \"" + serviceAccountInfo.accountName() + "-token\"}";
		String path = SERVICE_ACCOUNT_PATH + serviceAccountInfo.accountId() + "/tokens";
		HttpResponse<String> response = executePostRequest(path, body);
		boolean responseStatus = response != null && response.statusCode() <= 201;
		if (responseStatus) {
			this.serviceAccountInfo.setServiceAccountToken(response.body());
		}

	}

	private HttpResponse<String> executePostRequest(String path, String body) throws Exception {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
		URI uri = new URI("http", "admin:admin", host, 3000, path, null, null);
		HttpRequest httpRequest = HttpRequest.newBuilder() //
				.method(HttpMethod.POST.name(), HttpRequest.BodyPublishers.ofString(body)) //
				.header("Content-Type", "application/json") //
				.header("Authorization", "Basic YWRtaW46YWRtaW4=") //
				.uri(uri) //
				.build();

		return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> executeGetRequest(String path) throws Exception {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
		URI uri = new URI("http", "admin:admin", host, 3000, path, null, null);
		HttpRequest httpRequest = HttpRequest.newBuilder() //
				.method(HttpMethod.GET.name(), BodyPublishers.noBody()) //
				.header("Content-Type", "application/json") //
				.header("Authorization", "Basic YWRtaW46YWRtaW4=") //
				.uri(uri) //
				.build();

		return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
	}

}
