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
import net.bluemind.keycloak.api.Component;
import net.bluemind.keycloak.utils.KeycloakAdminClient;

public abstract class ComponentService extends KeycloakAdminClient {
	private static final Logger logger = LoggerFactory.getLogger(ComponentService.class);

	protected RBACManager rbacManager;
	protected String domainId;

	public enum ComponentProvider {
		BLUEMIND("Bluemind"), KERBEROS("kerberos");

		private String providerId;

		ComponentProvider(String providerId) {
			this.providerId = providerId;
		}

		public String getProviderId() {
			return providerId;
		}
	}

	protected ComponentService(BmContext context, String domainId) {
		this.rbacManager = new RBACManager(context);
		this.domainId = domainId;
	}

	protected void createComponent(JsonObject component) {
		logger.info("Create component {}", component);

		CompletableFuture<JsonObject> response = execute(String.format(COMPONENTS_URL, domainId), HttpMethod.POST,
				component);
		try {
			response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault("Failed to create component " + component);
		}
	}

	protected List<JsonObject> allComponents(ComponentProvider provider) {
		CompletableFuture<JsonObject> response = execute(
				String.format(COMPONENTS_URL, domainId) + "?type=" + Component.PROVIDER_TYPE, HttpMethod.GET);
		JsonObject json;
		try {
			json = response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault("Failed to get components for realm " + domainId, e);
		}

		List<JsonObject> ret = new ArrayList<>();
		JsonArray results = json.getJsonArray("results");
		results.forEach(cmp -> {
			if (provider.getProviderId().equals(((JsonObject) cmp).getString("providerId"))) {
				ret.add(((JsonObject) cmp));
			}
		});

		return ret;
	}

	protected JsonObject getComponent(ComponentProvider provider, String componentName) {
		CompletableFuture<JsonObject> response = execute(
				String.format(COMPONENTS_URL, domainId) + "?type=" + Component.PROVIDER_TYPE, HttpMethod.GET);
		JsonObject json;
		try {
			json = response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault("Failed to get components for realm " + domainId, e);
		}
		if (json == null) {
			logger.warn("Failed to fetch component id {}", componentName);
			return null;
		}
		JsonObject ret = null;
		JsonArray results = json.getJsonArray("results");
		if (results != null) {
			for (int i = 0; i < results.size(); i++) {
				if (componentName.equals(results.getJsonObject(i).getString("name"))
						&& provider.getProviderId().equals(results.getJsonObject(i).getString("providerId"))) {
					ret = results.getJsonObject(i);
				}
			}
		}

		return ret;
	}

	protected void deleteComponent(ComponentProvider provider, String componentName) {
		JsonObject cmp = getComponent(provider, componentName);
		if (cmp == null) {
			return;
		}
		CompletableFuture<JsonObject> response = execute(
				String.format(COMPONENTS_URL, domainId) + "/" + cmp.getString("id"), HttpMethod.DELETE);
		try {
			response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault("Failed to delete component " + componentName + " from realm " + domainId, e);
		}

	}
}
