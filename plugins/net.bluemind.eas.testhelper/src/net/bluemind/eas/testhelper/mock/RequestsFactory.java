/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.eas.testhelper.mock;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.eas.http.AuthenticatedEASQuery;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.EasUrls;
import net.bluemind.eas.http.query.EASQueryBuilder;
import net.bluemind.eas.testhelper.mock.RequestObject.HttpMethod;
import net.bluemind.vertx.common.http.BasicAuthHandler.AuthenticatedRequest;

public class RequestsFactory {

	public final String latd;
	public final String pass;
	public final String baseUrl;

	public RequestsFactory(String latd, String pass, String baseUrl) {
		this.latd = latd;
		this.pass = pass;
		this.baseUrl = baseUrl;
	}

	public AuthenticatedEASQuery authenticatedEas(String devId, String devType, ImmutableMap<String, String> headers,
			ImmutableMap<String, String> queryParams) {
		AuthenticatedRequest ar = authenticated(devId, devType, headers, queryParams);
		AuthenticatedEASQuery decoded = EASQueryBuilder.from(ar);
		return decoded;
	}

	public AuthorizedDeviceQuery authorized(Vertx vertx, String devId, String devType,
			ImmutableMap<String, String> headers, ImmutableMap<String, String> queryParams, String partnershipId) {
		AuthenticatedEASQuery decoded = authenticatedEas(devId, devType, headers, queryParams);
		AuthorizedDeviceQuery authorizedDevice = new AuthorizedDeviceQuery(vertx, decoded, partnershipId);
		return authorizedDevice;
	}

	public AuthenticatedRequest authenticated(String devId, String devType, ImmutableMap<String, String> headers,
			ImmutableMap<String, String> queryParams) {
		Map<String, String> mutableHeaders = new HashMap<String, String>(headers);
		mutableHeaders.put("Authorization", "Basic " + b64((latd + ":" + pass).getBytes()));

		Map<String, String> mutableParams = new HashMap<>(queryParams);
		mutableParams.put("User", latd);
		mutableParams.put("DeviceId", devId);
		mutableParams.put("DeviceType", devType);

		HttpServerRequest req = new RequestObject(HttpMethod.POST, mutableHeaders, baseUrl, EasUrls.ROOT,
				mutableParams);
		AuthenticatedRequest ar = new AuthenticatedRequest(req, latd, pass);
		return ar;
	}

	private String b64(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	/**
	 * Creates an {@link AuthenticatedRequest} for a base64-style eas request
	 * 
	 * @param headers
	 * @param query
	 * @return
	 */
	public AuthenticatedRequest authenticated(ImmutableMap<String, String> headers, String query) {
		Map<String, String> mutableHeaders = new HashMap<>(headers);
		mutableHeaders.put("Authorization", "Basic " + b64((latd + ":" + pass).getBytes()));

		HttpServerRequest req = new RequestObject(HttpMethod.POST, mutableHeaders, baseUrl, EasUrls.ROOT, query);
		AuthenticatedRequest ar = new AuthenticatedRequest(req, latd, pass);
		return ar;
	}
}
