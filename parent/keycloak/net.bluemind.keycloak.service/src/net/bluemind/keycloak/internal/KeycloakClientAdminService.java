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
package net.bluemind.keycloak.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.keycloak.api.IKeycloakClientAdmin;
import net.bluemind.role.api.BasicRoles;

public class KeycloakClientAdminService extends KeycloakAdminClient implements IKeycloakClientAdmin {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakClientAdminService.class);
	private RBACManager rbacManager;
	private String domainId;

	public KeycloakClientAdminService(BmContext context, String domainId) {
		this.rbacManager = new RBACManager(context);
		this.domainId = domainId;
	}

	@Override
	public void create(String clientId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);

		logger.info("Realm {}: Create client {}", domainId, clientId);

		JsonObject client = new JsonObject();
		client.put("id", clientId);
		client.put("clientId", clientId);
		client.put("enabled", true);

		JsonArray redirectUris = new JsonArray();
		redirectUris.add("*");
		client.put("redirectUris", redirectUris);

		JsonObject response = execute(String.format(CLIENTS_URL, domainId), "POST", client);
		if (response.getInteger("statusCode") != 201) {
			if (logger.isWarnEnabled()) {
				logger.warn(response.encodePrettily());
			}
			throw new ServerFault("Failed to create client");
		}

	}

	@Override
	public String getSecret(String clientId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Realm {}: Get client secret {}", domainId, clientId);
		JsonObject ret = execute(String.format(CLIENTS_CREDS_URL, domainId, clientId), "GET");
		if (ret.getInteger("statusCode") != 200) {
			if (logger.isWarnEnabled()) {
				logger.warn(ret.encodePrettily());
			}
			throw new ServerFault("Failed to get client secret");
		}

		return ret.getJsonObject("body").getString("value");

	}

}