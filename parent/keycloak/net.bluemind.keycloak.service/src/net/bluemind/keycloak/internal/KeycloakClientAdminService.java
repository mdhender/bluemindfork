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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import net.bluemind.keycloak.api.IKeycloakClientAdmin;
import net.bluemind.keycloak.api.OidcClient;
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
		client.put("directAccessGrantsEnabled", true);

		JsonArray redirectUris = new JsonArray();
		redirectUris.add("*");
		client.put("redirectUris", redirectUris);

		CompletableFuture<JsonObject> response = execute(String.format(CLIENTS_URL, domainId), HttpMethod.POST, client);

		try {
			response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault("Failed to create client");
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
		} catch (Exception e) {
			throw new ServerFault("Failed to get client secret");
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
		} catch (Exception e) {
			logger.error("EXCeptkion " + e.getClass().getName() + " : " + e.getMessage(), e);
			throw new ServerFault("Failed to fetch clients for realm " + domainId);
		}
		
		List<OidcClient> ret = new ArrayList<>();
		JsonArray results = json.getJsonArray("results");
		results.forEach(cli -> {
			if (cli != null && "openid-connect".equals(((JsonObject) cli).getString("protocol") )) {
				ret.add(jsonToOidcClient(((JsonObject) cli)));
			}
		});
		return ret;
	}

	@Override
	public OidcClient getOidcClient(String clientId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		
		logger.info("Realm {}: Get client {}", domainId, clientId);
		String spec = String.format(CLIENTS_URL, domainId);
		try {
			spec += "?clientId=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e) {
			logger.error("UnsupportedEncodingException : " + StandardCharsets.UTF_8.toString());
			throw new ServerFault(e);
		}
		CompletableFuture<JsonObject> response = execute(spec, HttpMethod.GET);
		JsonObject json;
		try {
			json = response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			logger.error("EXCeptkion " + e.getClass().getName() + " : " + e.getMessage(), e);
			throw new ServerFault("Failed to fetch client " + clientId + " in realm " + domainId);
		}
		JsonArray results = json.getJsonArray("results");
		if (results == null || results.size() == 0 || !"openid-connect".equals(json.getJsonArray("results").getJsonObject(0).getString("protocol"))) {
			return null;
		}
		return jsonToOidcClient(json.getJsonArray("results").getJsonObject(0));
	}

	@Override
	public void deleteOidcClient(String clientId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		
		logger.info("Realm {}: Delete client {}", domainId, clientId);
		
		OidcClient oc = null;
		try {
			oc = getOidcClient(clientId);
		} catch (Throwable t) {
			logger.error("Couldn't get client " + clientId + " in realm " + domainId + " to delete it", t);
			throw new ServerFault(t);
		}
		if (oc == null) {
			throw new ServerFault("Couldn't get client " + clientId + " in realm " + domainId + " to delete it");
		}
		
		CompletableFuture<JsonObject> response = execute(String.format(CLIENTS_URL, domainId) + "/" + oc.id, HttpMethod.DELETE);
		try {
			response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault("Failed to delete client");
		}
	}
	
	@SuppressWarnings("unchecked")
	private OidcClient jsonToOidcClient(JsonObject ret) {
		if (ret == null) {
			return null;
		}

		OidcClient cli = new OidcClient();
		cli.id = ret.getString("id");
		cli.clientId = ret.getString("clientId");
		cli.publicClient = ret.getBoolean("publicClient");
		cli.secret = ret.getString("secret");
		cli.standardFlowEnabled = ret.getBoolean("standardFlowEnabled");
		cli.directAccessGrantsEnabled = ret.getBoolean("directAccessGrantsEnabled");
		cli.serviceAccountsEnabled = ret.getBoolean("serviceAccountsEnabled");
		cli.rootUrl = ret.getString("rootUrl");
		cli.redirectUris = ret.getJsonArray("redirectUris").getList();
		cli.webOrigins = ret.getJsonArray("webOrigins").getList();

		return cli;
	}

}
