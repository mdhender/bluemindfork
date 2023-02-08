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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpMethod;
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

		CompletableFuture<JsonObject> response = execute(REALMS_ADMIN_URL, HttpMethod.POST, realm);

		try {
			response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault("Failed to create realm");
		}

	}

	@Override
	public void deleteRealm(String domainId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Delete realm {}", domainId);
		CompletableFuture<JsonObject> response = execute(REALMS_ADMIN_URL + "/" + domainId, HttpMethod.DELETE);
		try {
			response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault("Failed to delete realm");
		}
	}

	@Override
	public List<Realm> allRealms() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);

		logger.info("Get realms");

		CompletableFuture<JsonObject> response = execute(REALMS_ADMIN_URL, HttpMethod.GET);

		JsonObject json;
		try {
			json = response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault("Failed fetch realms");
		}

		List<Realm> ret = new ArrayList<>();
		JsonArray results = json.getJsonArray("results");
		results.forEach(realm -> ret.add(jsonToRealm((JsonObject) realm)));
		return ret;
	}

	@Override
	public Realm getRealm(String domainId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);

		logger.info("Get realm {}", domainId);

		CompletableFuture<JsonObject> response = execute(REALMS_ADMIN_URL + "/" + domainId, HttpMethod.GET);

		JsonObject json;
		try {
			json = response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault("Failed to get realm");
		}

		return jsonToRealm(json);
	}

	private Realm jsonToRealm(JsonObject ret) {
		if (ret == null) {
			return null;
		}

		Realm realm = new Realm();
		realm.id = ret.getString("id");
		realm.realm = ret.getString("realm");
		realm.enabled = ret.getBoolean("enabled");
		realm.loginWithEmailAllowed = ret.getBoolean("loginWithEmailAllowed");
		return realm;
	}

}