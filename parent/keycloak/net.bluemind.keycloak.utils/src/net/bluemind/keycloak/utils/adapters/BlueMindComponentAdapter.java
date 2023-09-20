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
import java.util.Optional;

import io.vertx.core.json.JsonObject;
import net.bluemind.config.Token;
import net.bluemind.keycloak.api.BluemindProviderComponent;
import net.bluemind.keycloak.api.Component.CachePolicy;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.network.topology.Topology;

public class BlueMindComponentAdapter {
	public final BluemindProviderComponent component;

	public BlueMindComponentAdapter(BluemindProviderComponent component) {
		this.component = component;
	}

	public static BlueMindComponentAdapter build(String domainUid) {
		BluemindProviderComponent component = new BluemindProviderComponent();
		component.parentId = domainUid;
		component.id = IKeycloakUids.bmProviderId(domainUid);
		component.name = IKeycloakUids.bmProviderId(domainUid);

		component.bmUrl = "http://" + Topology.get().core().value.address() + ":8090";
		component.bmCoreToken = Token.admin0();

		return new BlueMindComponentAdapter(component);
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.put("id", component.id);
		json.put("providerId", component.providerId);
		json.put("providerType", component.providerType);
		json.put("parentId", component.parentId);
		json.put("name", component.name);

		JsonObject config = new JsonObject();
		if (component.bmDomain != null)
			config.put("bmDomain", Arrays.asList(component.bmDomain));

		if (component.bmUrl != null)
			config.put("bmUrl", Arrays.asList(component.bmUrl));

		if (component.bmCoreToken != null)
			config.put("bmCoreToken", Arrays.asList(component.bmCoreToken));

		config.put("enabled", Arrays.asList(component.enabled.toString()));

		if (component.cachePolicy != null)
			config.put("cachePolicy", Arrays.asList(component.cachePolicy.name()));

		json.put("config", config);

		return json;
	}

	public static Optional<BluemindProviderComponent> fromJson(JsonObject json) {
		if (json == null) {
			return Optional.empty();
		}

		BluemindProviderComponent component = new BluemindProviderComponent();
		component.id = json.getString("id");
		component.parentId = json.getString("parentId");
		component.name = json.getString("name");

		JsonObject config = json.getJsonObject("config");
		if (config.getJsonArray("bmDomain") != null) {
			component.bmDomain = config.getJsonArray("bmDomain").getString(0);
		}

		if (config.getJsonArray("bmUrl") != null) {
			component.bmUrl = config.getJsonArray("bmUrl").getString(0);
		}

		if (config.getJsonArray("enabled") != null) {
			component.enabled = Boolean.valueOf(config.getJsonArray("enabled").getString(0));
		}

		if (config.getJsonArray("cachePolicy") != null) {
			component.cachePolicy = CachePolicy.valueOf(config.getJsonArray("cachePolicy").getString(0));
		}

		return Optional.of(component);
	}
}
