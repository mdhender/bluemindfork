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

import java.io.IOException;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import io.vertx.core.json.JsonObject;
import net.bluemind.cti.wazo.api.client.WazoApiClient;
import net.bluemind.cti.wazo.api.client.exception.WazoApiResponseException;
import net.bluemind.cti.wazo.config.WazoEndpoints;
import net.bluemind.user.api.UserAccountInfo;

public class WazoAuthenticationClient extends WazoApiClient {

	private String auth;

	class BlueMindWazoTokenResponse {
		public String token;
	}

	public WazoAuthenticationClient(String domainUid, UserAccountInfo userAccountInfo) {
		super(domainUid);
		this.auth = encodeUserAuth(userAccountInfo);
	}

	private static String encodeUserAuth(UserAccountInfo userAccountInfo) {
		StringBuilder sb = new StringBuilder();
		sb.append(userAccountInfo.login).append(":").append(userAccountInfo.credentials);
		return Base64.getEncoder().encodeToString(sb.toString().getBytes());
	}

	public String getToken() {

		BlueMindWazoTokenResponse response = null;

		JsonObject payload = new JsonObject();
		payload.put("backend", "wazo_user");
		payload.put("expiration", 3600);
		payload.put("access_type", "offline");
		payload.put("client_id", "Bluemind");

		try (HttpsWazoApiConnection connection = getConnection(WazoEndpoints.TOKEN)) {
			connection.executePost(payload, "Authorization", "Basic " + auth);
			connection.manageApiResponse(200);
			response = decodeJsonResponse(connection.readResponse());
		}

		return response.token;
	}

	private BlueMindWazoTokenResponse decodeJsonResponse(String jsonmessage) {

		JsonFactory jsonfactory = new JsonFactory();
		try (JsonParser parser = jsonfactory.createParser(jsonmessage)) {
			BlueMindWazoTokenResponse jresp = new BlueMindWazoTokenResponse();
			parser.nextToken();
			if (parser.currentToken() != JsonToken.START_OBJECT) {
				throw new IllegalStateException("Expected an object");
			}

			while (parser.nextToken() != JsonToken.END_OBJECT) {
				String fieldName = parser.getCurrentName();
				if (fieldName == null || !"token".equals(fieldName)) {
					continue;
				}
				jresp.token = parser.nextTextValue();
				break;
			}
			return jresp;
		} catch (IOException e) {
			throw new WazoApiResponseException(e);
		}
	}

}
