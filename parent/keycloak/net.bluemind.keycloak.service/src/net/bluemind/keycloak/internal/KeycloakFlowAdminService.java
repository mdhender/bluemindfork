/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.keycloak.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.keycloak.api.AuthenticationFlow;
import net.bluemind.keycloak.api.IKeycloakFlowAdmin;
import net.bluemind.keycloak.utils.BlueMindFlowManager;
import net.bluemind.keycloak.utils.KeycloakAdminClient;
import net.bluemind.role.api.BasicRoles;

public class KeycloakFlowAdminService extends KeycloakAdminClient implements IKeycloakFlowAdmin {
	private static final Logger logger = LoggerFactory.getLogger(KeycloakFlowAdminService.class);

	private RBACManager rbacManager;
	private String domainId;

	public KeycloakFlowAdminService(BmContext context, String domainId) {
		this.rbacManager = new RBACManager(context);
		this.domainId = domainId;
	}

	@Override
	public void createByCopying(String flowToCopyAlias, String newFlowAlias) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Realm {}: Create flow {}", domainId, newFlowAlias);

		BlueMindFlowManager.fromCopy(domainId).setupBluemindAuthenticator().setupSessionLimits();
	}

	@Override
	public AuthenticationFlow getAuthenticationFlow(String flowAlias) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Realm {}: Get flow {}", domainId, flowAlias);

		JsonObject response = call("/admin/realms/" + domainId + "/authentication/flows", HttpMethod.GET, null);

		if (response == null) {
			logger.warn("Failed to fetch authentication flow {}", flowAlias);
			return null;
		}

		JsonArray results = response.getJsonArray("results");
		AuthenticationFlow res = null;
		for (int i = 0; i < results.size() && res == null; i++) {
			JsonObject cFlow = results.getJsonObject(i);
			if (flowAlias.equals(cFlow.getString("alias"))) {
				res = new AuthenticationFlow();
				res.id = cFlow.getString("id");
				res.alias = cFlow.getString("alias");
			}
		}

		return res;
	}

	@Override
	public void deleteFlow(String flowAlias) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Realm {}: Delete flow {}", domainId, flowAlias);

		String flowId = null;
		try {
			AuthenticationFlow flow = getAuthenticationFlow(flowAlias);
			flowId = flow.id;
		} catch (Throwable t) {
		}

		if (flowId != null) {
			call("/admin/realms/" + domainId + "/authentication/flows/" + flowId, HttpMethod.DELETE, null);
		}
	}

}