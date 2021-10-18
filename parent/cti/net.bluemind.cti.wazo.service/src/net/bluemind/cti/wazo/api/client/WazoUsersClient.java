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
package net.bluemind.cti.wazo.api.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.cti.wazo.api.client.connection.HttpsWazoApiConnection;
import net.bluemind.cti.wazo.api.client.exception.WazoApiResponseException;
import net.bluemind.cti.wazo.config.WazoEndpoints;
import net.bluemind.user.api.UserAccountInfo;

public class WazoUsersClient extends WazoAuthentifiedApiClient {

	class BlueMindWazoUsersListResponse {
		public int total;
		public List<BlueMindWazoUserResponse> items = new ArrayList<>();

		class BlueMindWazoUserResponse {
			String uid;
			String email;

			public BlueMindWazoUserResponse(String uid, String email) {
				this.uid = uid;
				this.email = email;
			}
		}

		public void addItem(String uid, String email) {
			BlueMindWazoUserResponse user = new BlueMindWazoUserResponse(uid, email);
			items.add(user);
		}

	}

	public WazoUsersClient(String domainUid, UserAccountInfo userAccountInfo) {
		super(domainUid, userAccountInfo);
	}

	public List<String> getUsers() {
		return confdUsers();
	}

	private List<String> confdUsers() {

		BlueMindWazoUsersListResponse response = null;

		try (HttpsWazoApiConnection connection = getConnection(WazoEndpoints.CONFD)) {
			connection.executeGet("X-Auth-Token", getToken());
			connection.manageApiResponse(200);
			response = decodeJsonResponse(connection.readResponse());
		}

		return response.items.stream().map(e -> e.email).collect(Collectors.toList());
	}

	private BlueMindWazoUsersListResponse decodeJsonResponse(String jsonmessage) {

		JsonFactory jsonfactory = new JsonFactory();
		try (JsonParser parser = jsonfactory.createParser(jsonmessage)) {

			BlueMindWazoUsersListResponse jresp = new BlueMindWazoUsersListResponse();

			ObjectMapper objectMapper = new ObjectMapper();
			TreeNode rootNode = objectMapper.readTree(parser);

			jresp.total = Integer.valueOf(rootNode.get("total").toString());
			TreeNode itemsNode = rootNode.get("items");
			if (itemsNode.isArray()) {
				for (int i = 0; i < itemsNode.size(); i++) {
					TreeNode item = itemsNode.get(i);
					jresp.addItem(trimQuotes(item.get("uuid")), trimQuotes(item.get("email")));
				}
			}
			return jresp;
		} catch (IOException e) {
			throw new WazoApiResponseException(e);
		}
	}

	private String trimQuotes(TreeNode treeNode) {
		String nodeStrValue = treeNode.toString();
		return nodeStrValue.startsWith("\"") && nodeStrValue.endsWith("\"") ? nodeStrValue.replace("\"", "")
				: nodeStrValue;
	}

}
