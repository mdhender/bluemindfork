/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.keycloak.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.keycloak.api.IKeycloakUids;

public class BlueMindFlowManager {
	private static final Logger logger = LoggerFactory.getLogger(BlueMindFlowManager.class);

	private static final String BM_AUTHENTICATOR_PROVIDER_ID = "bm-auth-usr-pwd-pubpriv-form";
	private static final String USER_SESSION_LIMITS = "user-session-limits";

	private static final KeycloakAdminClient kcAdminClient = new KeycloakAdminClient();

	private final String domainUid;

	private JsonArray domainFlowExecutions;

	public static BlueMindFlowManager fromCopy(String domainUid) {
		String copyUri = "/admin/realms/" + domainUid + "/authentication/flows/" + IKeycloakUids.KEYCLOAK_FLOW_ALIAS
				+ "/copy";
		JsonObject reqData = new JsonObject();
		reqData.put("newName", IKeycloakUids.BLUEMIND_FLOW_ALIAS);
		kcAdminClient.call(copyUri, HttpMethod.POST, reqData);

		return forDomain(domainUid);
	}

	public static BlueMindFlowManager forDomain(String domainUid) {
		return new BlueMindFlowManager(domainUid).refreshDomainFlowExecutions().deleteDefaultFormExecution()
				.refreshDomainFlowExecutions();
	}

	private BlueMindFlowManager(String domainUid) {
		this.domainUid = domainUid;
	}

	private BlueMindFlowManager refreshDomainFlowExecutions() {
		domainFlowExecutions = kcAdminClient.call("/admin/realms/" + domainUid + "/authentication/flows/"
				+ IKeycloakUids.BLUEMIND_FLOW_ALIAS + "/executions", HttpMethod.GET, null).getJsonArray("results");
		return this;
	}

	private String flowExecutionUri(String formExecutionId) {
		return "/admin/realms/" + domainUid + "/authentication/executions/" + formExecutionId;
	}

	private BlueMindFlowManager deleteDefaultFormExecution() {
		JsonObject defaultFormFlowExecution = getFormExecutionOf("auth-username-password-form");
		if (defaultFormFlowExecution == null || defaultFormFlowExecution.getString("id") == null) {
			return this;
		}

		// Remove default form flow execution
		kcAdminClient.call(flowExecutionUri(defaultFormFlowExecution.getString("id")), HttpMethod.DELETE, null);

		return this;
	}

	private String getParentFlowId() {
		for (int i = 0; i < domainFlowExecutions.size(); i++) {
			JsonObject currentDomainFlowExecution = domainFlowExecutions.getJsonObject(i);
			if (currentDomainFlowExecution.getInteger("level") == 0
					&& Boolean.TRUE.equals(currentDomainFlowExecution.getBoolean("authenticationFlow", Boolean.FALSE)))
				return currentDomainFlowExecution.getString("flowId");
		}

		return null;
	}

	private JsonObject getFormExecutionOf(String providerId) {
		for (int i = 0; i < domainFlowExecutions.size(); i++) {
			JsonObject currentDomainFlowExecution = domainFlowExecutions.getJsonObject(i);
			if (providerId.equals(currentDomainFlowExecution.getString("providerId"))) {
				return currentDomainFlowExecution;
			}
		}

		return null;
	}

	public BlueMindFlowManager setupBluemindAuthenticator() {
		JsonObject bmFormExec = new JsonObject();
		bmFormExec.put("authenticator", BM_AUTHENTICATOR_PROVIDER_ID);
		bmFormExec.put("authenticatorFlow", false);
		bmFormExec.put("requirement", "REQUIRED");
		bmFormExec.put("priority", 10);
		bmFormExec.put("parentFlow", getParentFlowId());
		kcAdminClient.call("/admin/realms/" + domainUid + "/authentication/executions", HttpMethod.POST, bmFormExec);

		// Reload from Keycloak
		refreshDomainFlowExecutions();

		JsonObject bmFormExecution = getFormExecutionOf(BM_AUTHENTICATOR_PROVIDER_ID);
		if (bmFormExecution == null || bmFormExecution.getString("id") == null) {
			logger.error("Unable to setup " + BM_AUTHENTICATOR_PROVIDER_ID);
			return this;
		}

		kcAdminClient.call("/admin/realms/" + domainUid + "/authentication/executions/"
				+ bmFormExecution.getString("id") + "/raise-priority", HttpMethod.POST, null);
		return this;
	}

	public BlueMindFlowManager setupSessionLimits() {
		JsonObject sessionLimits = getFormExecutionOf(USER_SESSION_LIMITS);

		if (sessionLimits == null) {
			JsonObject bmSessionLimits = new JsonObject();
			bmSessionLimits.put("authenticator", USER_SESSION_LIMITS);
			bmSessionLimits.put("authenticatorFlow", false);
			bmSessionLimits.put("requirement", "REQUIRED");
			bmSessionLimits.put("parentFlow", getParentFlowId());
			kcAdminClient.call("/admin/realms/" + domainUid + "/authentication/executions", HttpMethod.POST,
					bmSessionLimits);

			// Reload from Keycloak
			refreshDomainFlowExecutions();

			sessionLimits = getFormExecutionOf(USER_SESSION_LIMITS);
		}

		if (sessionLimits == null || sessionLimits.getString("id") == null) {
			logger.error("Unable to setup " + USER_SESSION_LIMITS);
			return this;
		}

		JsonObject config = new JsonObject();
		config.put("userRealmLimit", 5);
		config.put("userClientLimit", 0);
		config.put("behavior", "Terminate oldest session");
		config.put("errorMessage", "");

		JsonObject parameters = new JsonObject();
		parameters.put("alias", "bm-session-limits");
		parameters.put("config", config);

		kcAdminClient.call("/admin/realms/" + domainUid + "/authentication/executions/" + sessionLimits.getString("id")
				+ "/config", HttpMethod.POST, parameters);
		return this;
	}
}
