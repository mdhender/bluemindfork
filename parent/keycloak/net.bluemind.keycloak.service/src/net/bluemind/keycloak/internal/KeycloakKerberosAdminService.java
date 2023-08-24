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
import net.bluemind.keycloak.api.IKeycloakKerberosAdmin;
import net.bluemind.keycloak.api.KerberosComponent;
import net.bluemind.keycloak.utils.adapters.KerberosComponentAdapter;
import net.bluemind.role.api.BasicRoles;

public class KeycloakKerberosAdminService extends ComponentService implements IKeycloakKerberosAdmin {
	private static final Logger logger = LoggerFactory.getLogger(KeycloakKerberosAdminService.class);

	public KeycloakKerberosAdminService(BmContext context, String domainId) {
		super(context, domainId);
	}

	@Override
	public void create(KerberosComponent component) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		createComponent(new KerberosComponentAdapter(component).toJson());

	}

	@Override
	public List<KerberosComponent> allKerberosProviders() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Realm {}: Get all Kerberos providers", domainId);

		List<KerberosComponent> ret = new ArrayList<>();
		allComponents(ComponentProvider.KERBEROS)
				.forEach(cmp -> KerberosComponentAdapter.fromJson(cmp).ifPresent(ret::add));
		return ret;
	}

	@Override
	public KerberosComponent getKerberosProvider(String componentName) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Realm {}: Get Kerberos provider {}", domainId, componentName);

		return KerberosComponentAdapter.fromJson(getComponent(ComponentProvider.KERBEROS, componentName)).orElse(null);
	}

	@Override
	public void deleteKerberosProvider(String componentName) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN);
		logger.info("Realm {}: Delete kerberos provider {}", domainId, componentName);

		deleteComponent(ComponentProvider.KERBEROS, componentName);
	}
}
