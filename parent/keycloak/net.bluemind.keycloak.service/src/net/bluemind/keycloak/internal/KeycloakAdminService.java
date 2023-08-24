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
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.Realm;
import net.bluemind.keycloak.utils.KeycloakHelper;
import net.bluemind.keycloak.utils.adapters.RealmAdapter;
import net.bluemind.role.api.BasicRoles;

public class KeycloakAdminService extends KeycloakAdminClient implements IKeycloakAdmin {
	private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminService.class);

	private static final String REALMS_ADMIN_URL = BASE_URL + "/admin/realms";

	private RBACManager rbacManager;
	private BmContext context;

	public KeycloakAdminService(BmContext context) {
		rbacManager = new RBACManager(context);
		this.context = context;
	}

	@Override
	public void createRealm(String domainId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);

		logger.info("Create realm {}", domainId);

		CompletableFuture<JsonObject> response = execute(REALMS_ADMIN_URL, HttpMethod.POST,
				RealmAdapter.build(domainId).toJson());

		try {
			response.get(TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault("Failed to create realm", e);
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
			throw new ServerFault("Failed to delete realm", e);
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
		json.getJsonArray("results").forEach(realm -> ret.add(RealmAdapter.fromJson((JsonObject) realm)));
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
			throw new ServerFault("Failed to get realm", e);
		}

		return RealmAdapter.fromJson(json);
	}

	@Override
	public TaskRef initForDomain(String domainId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);

		return context.provider().instance(ITasksManager.class).run(new BlockingServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				monitor.begin(1, String.format("Init keycloak for domain %s ... ", domainId));
				try {
					KeycloakHelper.initForDomain(domainId);
					monitor.end(true, String.format("Init keycloak for domain %s done ", domainId), "[]");
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					monitor.end(false, String.format("Failed to init keycloak for domain %s ", domainId), "[]");
				}

			}
		});
	}

}