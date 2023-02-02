/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.Realm;
import net.bluemind.role.api.BasicRoles;

public class KeycloakAdminService extends KeycloakAdminClient implements IKeycloakAdmin {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminService.class);

	private static final String REALMS_ADMIN_URL = BASE_URL + "/admin/realms";

	private RBACManager rbacManager;

	public KeycloakAdminService(BmContext context) {
		rbacManager = new RBACManager(context);
	}

	@Override
	public void createRealm(String domainId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);

		logger.info("Create realm {}", domainId);

		JsonObject realm = new JsonObject();
		realm.put("id", domainId);
		realm.put("realm", domainId);
		realm.put("enabled", true);
		realm.put("loginWithEmailAllowed", true);

		JsonObject reseponse = execute(REALMS_ADMIN_URL, "POST", realm);
		if (reseponse.getInteger("statusCode") != 201) {
			if (logger.isWarnEnabled()) {
				logger.warn(reseponse.encodePrettily());
			}
			throw new ServerFault("Failed to create realm");
		}

	}

	@Override
	public void deleteRealm(String domainId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Delete realm {}", domainId);
		JsonObject response = execute(REALMS_ADMIN_URL + "/" + domainId, "DELETE");

		if (response.getInteger("statusCode") != 204) {
			if (logger.isWarnEnabled()) {
				logger.warn(response.encodePrettily());
			}
		}

	}

	@Override
	public List<Realm> allRealms() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);

		logger.info("Get realms");

		JsonObject response = execute(REALMS_ADMIN_URL, "GET");
		List<Realm> ret = new ArrayList<>();

		JsonArray realms = response.getJsonArray("body");
		realms.forEach(realm -> {
			ret.add(jsonToRealm((JsonObject) realm));
		});

		return ret;
	}

	@Override
	public Realm getRealm(String domainId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);

		logger.info("Get realm {}", domainId);

		JsonObject response = execute(REALMS_ADMIN_URL + "/" + domainId, "GET");
		if (response.getInteger("statusCode") == 200) {
			return jsonToRealm(response.getJsonObject("body"));
		}

		logger.info("Realm {} not found", domainId);
		return null;
	}

	private Realm jsonToRealm(JsonObject ret) {
		Realm realm = new Realm();
		realm.id = ret.getString("id");
		realm.realm = ret.getString("realm");
		realm.enabled = ret.getBoolean("enabled");
		realm.loginWithEmailAllowed = ret.getBoolean("loginWithEmailAllowed");
		return realm;
	}

}