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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.keycloak.api.BluemindProviderComponent;
import net.bluemind.keycloak.api.IKeycloakBluemindProviderAdmin;
import net.bluemind.keycloak.utils.adapters.BlueMindComponentAdapter;
import net.bluemind.role.api.BasicRoles;

public class KeycloakBluemindProviderAdminService extends ComponentService implements IKeycloakBluemindProviderAdmin {
	private static final Logger logger = LoggerFactory.getLogger(KeycloakBluemindProviderAdminService.class);

	public KeycloakBluemindProviderAdminService(BmContext context, String domainId) {
		super(context, domainId);
	}

	@Override
	public void create(BluemindProviderComponent component) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		createComponent(new BlueMindComponentAdapter(component).toJson());
	}

	@Override
	public List<BluemindProviderComponent> allBluemindProviders() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Realm {}: Get all Bluemind providers", domainId);

		List<BluemindProviderComponent> ret = new ArrayList<>();
		allComponents(ComponentProvider.BLUEMIND)
				.forEach(cmp -> BlueMindComponentAdapter.fromJson(cmp).ifPresent(ret::add));
		return ret;
	}

	@Override
	public BluemindProviderComponent getBluemindProvider(String componentName) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Realm {}: Get Bluemind provider {}", domainId, componentName);

		return BlueMindComponentAdapter.fromJson(getComponent(ComponentProvider.BLUEMIND, componentName)).orElse(null);
	}

	@Override
	public void deleteBluemindProvider(String componentName) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Realm {}: Delete bluemind provider {}", domainId, componentName);

		deleteComponent(ComponentProvider.BLUEMIND, componentName);
	}
}
