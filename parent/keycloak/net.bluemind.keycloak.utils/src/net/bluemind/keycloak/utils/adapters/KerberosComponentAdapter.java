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
package net.bluemind.keycloak.utils.adapters;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.keycloak.api.Component.CachePolicy;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.keycloak.api.KerberosComponent;
import net.bluemind.keycloak.utils.KerberosConfigHelper;
import net.bluemind.system.api.SysConfKeys;

public class KerberosComponentAdapter {
	private static final String GLOBAL_VIRT = "global.virt";

	public final KerberosComponent component;

	public KerberosComponentAdapter(KerberosComponent component) {
		this.component = component;
	}

	public static KerberosComponentAdapter build(ItemValue<Domain> domain) {
		KerberosComponent component = new KerberosComponent();

		Map<String, String> domainSettings = MQ.<String, Map<String, String>>sharedMap(Shared.MAP_DOMAIN_SETTINGS)
				.get(domain.uid);
		String domainExternalUrl = domainSettings.get(DomainSettingsKeys.external_url.name());

		if (!GLOBAL_VIRT.equals(domain.uid) && domainExternalUrl == null) {
			component.name = IKeycloakUids.kerberosComponentName(GLOBAL_VIRT);
			component.parentId = GLOBAL_VIRT;
		} else {
			component.name = IKeycloakUids.kerberosComponentName(domain.uid);
			component.parentId = domain.uid;
		}

		component.kerberosRealm = domain.value.properties.get(AuthDomainProperties.KRB_AD_DOMAIN.name()) != null
				? domain.value.properties.get(AuthDomainProperties.KRB_AD_DOMAIN.name()).toUpperCase()
				: null;

		component.keyTab = KerberosConfigHelper.getKeytabFilename(domain.uid);
		component.enabled = true;
		component.debug = true;
		component.cachePolicy = CachePolicy.DEFAULT;

		String globalExternalUrl = MQ.<String, String>sharedMap(Shared.MAP_SYSCONF)
				.get(SysConfKeys.external_url.name());
		component.serverPrincipal = domainExternalUrl != null ? domainExternalUrl : globalExternalUrl;

		return new KerberosComponentAdapter(component);
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.put("id", component.id);
		json.put("providerId", component.providerId);
		json.put("providerType", component.providerType);
		json.put("parentId", component.parentId);
		json.put("name", component.name);

		JsonObject config = new JsonObject();
		config.put("kerberosRealm", Arrays.asList(component.kerberosRealm));
		config.put("serverPrincipal", Arrays.asList(component.serverPrincipal));
		config.put("keyTab", Arrays.asList(component.keyTab));
		config.put("enabled", Arrays.asList(component.enabled.toString()));
		config.put("debug", Arrays.asList(component.debug.toString()));
		config.put("allowPasswordAuthentication", Arrays.asList(component.allowPasswordAuthentication.toString()));
		config.put("updateProfileFirstLogin", Arrays.asList(component.updateProfileFirstLogin.toString()));
		config.put("cachePolicy", Arrays.asList(component.cachePolicy.name()));

		json.put("config", config);

		return json;
	}

	public static Optional<KerberosComponent> fromJson(JsonObject json) {
		if (json == null) {
			return Optional.empty();
		}

		KerberosComponent kc = new KerberosComponent();
		kc.id = json.getString("id");
		kc.parentId = json.getString("parentId");
		kc.name = json.getString("name");

		JsonObject config = json.getJsonObject("config");
		if (config.getJsonArray("kerberosRealm") != null) {
			kc.kerberosRealm = config.getJsonArray("kerberosRealm").getString(0);
		}
		if (config.getJsonArray("serverPrincipal") != null) {
			kc.serverPrincipal = config.getJsonArray("serverPrincipal").getString(0);
		}
		if (config.getJsonArray("keyTab") != null) {
			kc.keyTab = config.getJsonArray("keyTab").getString(0);
		}

		if (config.getJsonArray("enabled") != null) {
			kc.enabled = Boolean.valueOf(config.getJsonArray("enabled").getString(0));
		}
		if (config.getJsonArray("debug") != null) {
			kc.debug = Boolean.valueOf(config.getJsonArray("debug").getString(0));
		}
		if (config.getJsonArray("allowPasswordAuthentication") != null) {
			kc.allowPasswordAuthentication = Boolean
					.valueOf(config.getJsonArray("allowPasswordAuthentication").getString(0));
		}
		if (config.getJsonArray("updateProfileFirstLogin") != null) {
			kc.updateProfileFirstLogin = Boolean.valueOf(config.getJsonArray("updateProfileFirstLogin").getString(0));
		}

		if (config.getJsonArray("cachePolicy") != null) {
			kc.cachePolicy = CachePolicy.valueOf(config.getJsonArray("cachePolicy").getString(0));
		}

		return Optional.of(kc);
	}
}
