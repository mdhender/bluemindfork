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

import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.keycloak.api.BluemindProviderComponent;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.IKeycloakBluemindProviderAdmin;
import net.bluemind.keycloak.api.IKeycloakClientAdmin;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.TagDescriptor;

public class DomainHook extends DomainHookAdapter {

	private static final Logger logger = LoggerFactory.getLogger(DomainHook.class);

	@Override
	public void onCreated(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		logger.info("Init Keycloak realm for domain {}", domain.uid);
		IKeycloakAdmin keycloakAdminService = context.provider().instance(IKeycloakAdmin.class);

		String realm = domain.uid;
		String clientId = IKeycloakUids.clientId(domain.uid);

		keycloakAdminService.createRealm(realm);
		
		IKeycloakBluemindProviderAdmin keycloakBluemindProviderService = ServerSideServiceProvider
				.getProvider(SecurityContext.SYSTEM).instance(IKeycloakBluemindProviderAdmin.class, domain.uid);
		BluemindProviderComponent bpComponent = new BluemindProviderComponent();
		bpComponent.setParentId(realm);
		bpComponent.setName(realm + "-bmprovider");
		bpComponent.setBmUrl("https://" + Topology.get().any(TagDescriptor.bm_core.getTag()).value.address()); //ou pas... à vérifier
		bpComponent.setBmCoreToken(Token.admin0());
		keycloakBluemindProviderService.create(bpComponent);

		IKeycloakClientAdmin keycloakRealmAdminService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IKeycloakClientAdmin.class, domain.uid);
		keycloakRealmAdminService.create(clientId);
		String secret = keycloakRealmAdminService.getSecret(clientId);

		IDomainSettings settingsApi = context.provider().instance(IDomainSettings.class, domain.uid);
		Map<String, String> settings = settingsApi.get();
		settings.put(DomainSettingsKeys.openid_realm.name(), realm);
		settings.put(DomainSettingsKeys.openid_client_id.name(), clientId);
		settings.put(DomainSettingsKeys.openid_client_secret.name(), secret);
		settingsApi.set(settings);

	}

	@Override
	public void onDeleted(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		logger.info("Delete Keycloak realm for domain {}", domain.uid);
		IKeycloakAdmin service = context.provider().instance(IKeycloakAdmin.class);
		service.deleteRealm(domain.uid);
	}

}
