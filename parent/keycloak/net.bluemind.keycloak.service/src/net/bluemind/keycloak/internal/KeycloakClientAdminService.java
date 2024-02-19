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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.keycloak.api.IKeycloakClientAdmin;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.keycloak.api.OidcClient;
import net.bluemind.keycloak.utils.KeycloakAdminClient;
import net.bluemind.keycloak.utils.adapters.OidcClientAdapter;
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

		// List flows to get our flow Id
		String ourFlowId = null;
		String flowsUri = "/admin/realms/" + domainId + "/authentication/flows";
		JsonObject resp = call(flowsUri, HttpMethod.GET, null);
		JsonArray flows = resp.getJsonArray("results");
		for (int i = 0; i < flows.size(); i++) {
			JsonObject curFlow = flows.getJsonObject(i);
			if (IKeycloakUids.BLUEMIND_FLOW_ALIAS.equals(curFlow.getString("alias"))) {
				ourFlowId = curFlow.getString("id");
			}
		}

		CompletableFuture<JsonObject> response = execute(String.format(CLIENTS_URL, domainId), HttpMethod.POST,
				OidcClientAdapter.build(domainId, clientId, Optional.ofNullable(ourFlowId)).toJson());

		try {
			JsonObject json = response.get(TIMEOUT, TimeUnit.SECONDS);
			if (json.containsKey("error")) {
				throw new ServerFault("Error: " + json.getString("error") + " - " + json.getString("error_description"),
						ErrorCode.UNKNOWN);
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new ServerFault("Failed to create client", e);
		}
	}

	@Override
	public String getSecret(String clientId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Realm {}: Get client secret {}", domainId, clientId);
		CompletableFuture<JsonObject> response = execute(String.format(CLIENTS_CREDS_URL, domainId, clientId),
				HttpMethod.GET);

		JsonObject json;
		try {
			json = response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new ServerFault("Failed to get client secret", e);
		}
		if (json == null) {
			logger.warn("Failed to fetch secret");
			return null;
		}
		return json.getString("value");

	}

	@Override
	public List<OidcClient> allOidcClients() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);

		logger.info("Realm {}: Get OIDC clients", domainId);

		CompletableFuture<JsonObject> response = execute(String.format(CLIENTS_URL, domainId), HttpMethod.GET);
		JsonObject json;
		try {
			json = response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new ServerFault("Failed to fetch clients for realm " + domainId, e);
		}

		List<OidcClient> ret = new ArrayList<>();
		JsonArray results = json.getJsonArray("results");
		results.forEach(cli -> {
			if (cli != null && "openid-connect".equals(((JsonObject) cli).getString("protocol"))) {
				ret.add(OidcClientAdapter.fromJson((JsonObject) cli).oidcClient);
			}
		});
		return ret;
	}

	@Override
	public OidcClient getOidcClient(String clientId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);

		logger.info("Realm {}: Get client {}", domainId, clientId);
		String spec = String.format(CLIENTS_URL, domainId);
		spec += "?clientId=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8);
		CompletableFuture<JsonObject> response = execute(spec, HttpMethod.GET);
		JsonObject json;
		try {
			json = response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.error("EXCeptkion " + e.getClass().getName() + " : " + e.getMessage(), e);
			throw new ServerFault("Failed to fetch client " + clientId + " in realm " + domainId);
		}
		if (json == null) {
			logger.warn("Failed to fetch client id {}", clientId);
			return null;
		}
		JsonArray results = json.getJsonArray("results");
		if (results == null || results.size() == 0
				|| !"openid-connect".equals(json.getJsonArray("results").getJsonObject(0).getString("protocol"))) {
			return null;
		}
		return OidcClientAdapter.fromJson(json.getJsonArray("results").getJsonObject(0)).oidcClient;
	}

	@Override
	public void deleteOidcClient(String clientId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);

		logger.info("Realm {}: Delete client {}", domainId, clientId);

		CompletableFuture<JsonObject> response = execute(String.format(CLIENTS_URL, domainId) + "/" + clientId,
				HttpMethod.DELETE);
		try {
			response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new ServerFault("Failed to delete client", e);
		}
	}

	@Override
	public void updateClient(String clientId, OidcClient oc) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);

		logger.info("Realm {}: Update client {}", domainId, clientId);

		String clid = oc.id;
		if (clid == null) {
			OidcClient cli = getOidcClient(clientId);

			if (cli == null) {
				throw new ServerFault(clientId + " not foubd in realm " + domainId + " to update it");
			}

			clid = cli.id;
		}

		CompletableFuture<JsonObject> response = execute(String.format(CLIENTS_URL, domainId) + "/" + oc.id,
				HttpMethod.PUT, new OidcClientAdapter(oc).toJson());
		try {
			response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new ServerFault("Failed to update client " + clientId, e);
		}
	}

}