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

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.auth.AuthTypes;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.utils.KerberosConfigHelper;
import net.bluemind.keycloak.utils.KeycloakHelper;
import net.bluemind.keycloak.verticle.KeycloakVerticleAddress;
import net.bluemind.lib.vertx.VertxPlatform;

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

		if (hasValueChanged(DomainSettingsKeys.external_url.name(), previousSettings, currentSettings)) {
			logger.info("Domain {} external url has changed, update Keycloack configuration", domain.uid);
			notify(domain);
			return;
		}

		if (hasValueChanged(DomainSettingsKeys.other_urls.name(), previousSettings, currentSettings)) {
			logger.info("Domain {} other urls have changed, update Keycloack configuration", domain.uid);
			notify(domain);
		}

	}

	@Override
	public void onUpdated(BmContext context, ItemValue<Domain> previousValue, ItemValue<Domain> domain)
			throws ServerFault {
		propertiesUpdated(domain, previousValue.value.properties, domain.value.properties);
	}

	@Override
	public void onPropertiesUpdated(BmContext context, ItemValue<Domain> domain, Map<String, String> previousProperties,
			Map<String, String> currentProperties) throws ServerFault {
		propertiesUpdated(domain, previousProperties, currentProperties);
	}

	private void propertiesUpdated(ItemValue<Domain> domain, Map<String, String> previousProperties,
			Map<String, String> currentProperties) {

		if (previousProperties.get(AuthDomainProperties.AUTH_TYPE.name()) == null) {
			// skip 1st assignment
			return;
		}

		if (hasValueChanged(AuthDomainProperties.AUTH_TYPE.name(), previousProperties, currentProperties)) {
			logger.info("Domain {} auth type has changed, update Keycloack configuration", domain.uid);
			notify(domain);
			return;
		}

		if (AuthTypes.CAS.name().equals(currentProperties.get(AuthDomainProperties.AUTH_TYPE.name()))
				&& hasValueChanged(AuthDomainProperties.CAS_URL.name(), previousProperties, currentProperties)) {
			logger.info("Domain {} CAS URL has changed, update Keycloack configuration", domain.uid);
			notify(domain);
			return;
		}

		if (AuthTypes.KERBEROS.name().equals(currentProperties.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			boolean needUpdate = hasValueChanged(AuthDomainProperties.KRB_AD_DOMAIN.name(), previousProperties,
					currentProperties)
					|| hasValueChanged(AuthDomainProperties.KRB_AD_IP.name(), previousProperties, currentProperties)
					|| hasValueChanged(AuthDomainProperties.KRB_KEYTAB.name(), previousProperties, currentProperties);
			if (needUpdate) {
				logger.info("Domain {} Kerberos configuration has changed, update Keycloack configuration", domain.uid);
				notify(domain);
				return;
			}
		}

		if (AuthTypes.OPENID.name().equals(currentProperties.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			boolean needUpdate = hasValueChanged(AuthDomainProperties.OPENID_HOST.name(), previousProperties,
					currentProperties)
					|| hasValueChanged(AuthDomainProperties.OPENID_CLIENT_ID.name(), previousProperties,
							currentProperties)
					|| hasValueChanged(AuthDomainProperties.OPENID_CLIENT_SECRET.name(), previousProperties,
							currentProperties);
			if (needUpdate) {
				logger.info("Domain {} Kerberos configuration has changed, update Keycloack configuration", domain.uid);
				notify(domain);
				return;
			}
		}

	}

	private boolean hasValueChanged(String key, Map<String, String> previousProperties,
			Map<String, String> currentProperties) {
		boolean ret = previousProperties.get(key) != null
				? !previousProperties.get(key).equals(currentProperties.get(key))
				: currentProperties.get(key) != null;

		if (ret) {
			logger.info("{} has changed. {} -> {}", key, previousProperties.get(key), currentProperties.get(key));
		}

		return ret;
	}

	private void notify(ItemValue<Domain> domain) {
		VertxPlatform.eventBus().publish(KeycloakVerticleAddress.UPDATED,
				new JsonObject().put("containerUid", domain.uid));
	}

}
