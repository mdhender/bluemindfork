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

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.keycloak.api.Component;

public abstract class ComponentService extends KeycloakAdminClient {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakKerberosAdminService.class);

	protected RBACManager rbacManager;
	private String domainId;

	protected ComponentService(BmContext context, String domainId) {
		this.rbacManager = new RBACManager(context);
		this.domainId = domainId;
	}

	protected void createComponent(Component component) {

		logger.info("Create component {}", component);

		JsonObject response = execute(String.format(COMPONENTS_URL, domainId, domainId), "POST", component.toJson());
		if (response.getInteger("statusCode") != 201) {
			if (logger.isWarnEnabled()) {
				logger.warn(response.encodePrettily());
			}
			throw new ServerFault("Failed to create component " + component);
		}

	}
}
