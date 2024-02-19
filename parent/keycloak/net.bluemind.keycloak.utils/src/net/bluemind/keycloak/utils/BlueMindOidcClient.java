/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.keycloak.utils;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.TagDescriptor;

public class BlueMindOidcClient {
	private static final Logger logger = LoggerFactory.getLogger(BlueMindOidcClient.class);

	private static final KeycloakAdminClient kcAdminClient = new KeycloakAdminClient();

	private final String kcClientUrl;

	public BlueMindOidcClient(String domainUid) {
		this.kcClientUrl = "/admin/realms/" + domainUid + "/clients/" + IKeycloakUids.clientId(domainUid);
	}

	public void configure() {
		JsonObject bmClient = kcAdminClient.call(kcClientUrl, HttpMethod.GET, null);

		if (bmClient.containsKey("error")) {
			// Not ? Must be created
			logger.error("Error loading BlueMind OIDC client: {}", bmClient);
			return;
		}

		bmClient.put("attributes", setManagedAttributes(Optional.ofNullable(bmClient.getJsonObject("attributes"))));

		kcAdminClient.call(kcClientUrl, HttpMethod.PUT, bmClient);
	}

	private JsonObject setManagedAttributes(Optional<JsonObject> kcBmClientCurrentAttrs) {
		return kcBmClientCurrentAttrs.orElseGet(JsonObject::new) //
				.put("post.logout.redirect.uris", "*") //
				.put("backchannel.logout.url",
						"http://" + Topology.get().any(TagDescriptor.bm_core.getTag()).value.address()
								+ ":8080/bluemind_sso_logout/backchannel") //
				.put("backchannel.logout.session.required", "true");
	}
}
