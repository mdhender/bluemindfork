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
package net.bluemind.keycloak.service.domainhook;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.auth.AuthTypes;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.utils.ConfigUpdateHelper;
import net.bluemind.keycloak.utils.KerberosConfigHelper;
import net.bluemind.keycloak.utils.KeycloakHelper;

public class DomainHook extends DomainHookAdapter {

	private static final Logger logger = LoggerFactory.getLogger(DomainHook.class);

	@Override
	public void onCreated(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		if ("global.virt".equals(domain.uid)) {
			return;
		}
		KeycloakHelper.initForDomain(domain);
	}

	@Override
	public void onDeleted(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		if ("global.virt".equals(domain.uid)) {
			return;
		}
		logger.info("Delete Keycloak realm for domain {}", domain.uid);
		IKeycloakAdmin service = context.provider().instance(IKeycloakAdmin.class);
		service.deleteRealm(domain.uid);
		if (AuthTypes.KERBEROS.name().equals(domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			KerberosConfigHelper.removeKrb5Conf(domain.uid);
		}
	}

	@Override
	public void onSettingsUpdated(BmContext context, ItemValue<Domain> domain, Map<String, String> previousSettings,
			Map<String, String> currentSettings) throws ServerFault {
		ConfigUpdateHelper.updateRealmFor(domain.uid);
	}

}
