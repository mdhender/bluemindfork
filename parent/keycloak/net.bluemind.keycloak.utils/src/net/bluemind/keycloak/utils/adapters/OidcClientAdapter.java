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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.keycloak.api.OidcClient;
import net.bluemind.keycloak.utils.KeycloakHelper;

public class OidcClientAdapter {
	private static class ProtocolMapper {
		public String name;
		public String protocol;
		public String protocolMapper;
		public boolean consentRequired;
		public Map<String, String> config = new HashMap<>();

		public static ProtocolMapper build() {
			ProtocolMapper protocolMapper = new ProtocolMapper();
			protocolMapper.name = "bm_pubpriv";
			protocolMapper.protocol = "openid-connect";
			protocolMapper.protocolMapper = "oidc-usermodel-attribute-mapper";
			protocolMapper.consentRequired = false;
			protocolMapper.config = Map.of("user.attribute", "bm_pubpriv", //
					"claim.name", "bm_pubpriv", //
					"aggregate.attrs", "false", //
					"multivalued", "false", //
					"access.token.claim", "true", //
					"userinfo.token.claim", "false", //
					"id.token.claim", "false");

			return protocolMapper;
		}

		public JsonArray toJson() {
			JsonObject mapper = new JsonObject();
			mapper.put("name", name);
			mapper.put("protocol", protocol);
			mapper.put("protocolMapper", protocolMapper);
			mapper.put("consentRequired", consentRequired);

			JsonObject c = new JsonObject();
			mapper.put("config", c);
			config.forEach(c::put);

			JsonArray protocolMappers = new JsonArray();
			protocolMappers.add(mapper);

			return protocolMappers;
		}
	}

	public final OidcClient oidcClient;
	public final Optional<String> flowId;
	public final Optional<ProtocolMapper> protocolMapper;

	private OidcClientAdapter(OidcClient oidcClient, Optional<String> flowId, Optional<ProtocolMapper> protocolMapper) {
		this.oidcClient = oidcClient;
		this.flowId = flowId;
		this.protocolMapper = protocolMapper;
	}

	private OidcClientAdapter(OidcClient oidcClient, Optional<String> flowId) {
		this(oidcClient, flowId, Optional.empty());
	}

	public OidcClientAdapter(OidcClient oidcClient) {
		this(oidcClient, Optional.empty(), Optional.empty());
	}

	public static OidcClientAdapter build(String domainUid, String clientId, Optional<String> flowId) {
		OidcClient oidcClient = new OidcClient();
		oidcClient.enabled = true;

		oidcClient.id = clientId;
		oidcClient.clientId = clientId;

		oidcClient.directAccessGrantsEnabled = true;
		oidcClient.redirectUris = KeycloakHelper.getDomainUrls(domainUid);
		oidcClient.webOrigins = Arrays.asList("+");
		oidcClient.attributes = Map.of("post.logout.redirect.uris", "*");
		oidcClient.baseUrl = KeycloakHelper.getExternalUrl(domainUid);

		return new OidcClientAdapter(oidcClient, flowId, Optional.of(ProtocolMapper.build()));
	}

	@SuppressWarnings("unchecked")
	public static OidcClientAdapter fromJson(JsonObject json) {
		if (json == null) {
			return null;
		}

		OidcClient oidcClient = new OidcClient();
		oidcClient.id = json.getString("id");
		oidcClient.clientId = json.getString("clientId");
		oidcClient.publicClient = json.getBoolean("publicClient");
		oidcClient.secret = json.getString("secret");
		oidcClient.standardFlowEnabled = json.getBoolean("standardFlowEnabled");
		oidcClient.directAccessGrantsEnabled = json.getBoolean("directAccessGrantsEnabled");
		oidcClient.serviceAccountsEnabled = json.getBoolean("serviceAccountsEnabled");
		oidcClient.rootUrl = json.getString("rootUrl");
		oidcClient.redirectUris = json.getJsonArray("redirectUris").getList();
		oidcClient.webOrigins = json.getJsonArray("webOrigins").getList();
		oidcClient.baseUrl = json.getString("baseUrl");

		Optional<String> flowId = Optional.ofNullable(json.getJsonObject("authenticationFlowBindingOverrides"))
				.map(afbo -> afbo.getString("browser"));

		return new OidcClientAdapter(oidcClient, flowId);
	}

	public JsonObject toJson() {
		JsonObject client = new JsonObject();
		client.put("enabled", oidcClient.enabled);

		client.put("id", oidcClient.id);
		client.put("clientId", oidcClient.clientId);
		client.put("publicClient", oidcClient.publicClient);

		if (oidcClient.secret != null) {
			client.put("secret", oidcClient.secret);
		}

		client.put("redirectUris", oidcClient.redirectUris);
		client.put("baseUrl", oidcClient.baseUrl);
		client.put("rootUrl", oidcClient.rootUrl);

		JsonArray wo = new JsonArray();
		oidcClient.webOrigins.forEach(wo::add);
		client.put("webOrigins", wo);

		if (oidcClient.attributes != null) {
			JsonObject attrs = new JsonObject();
			oidcClient.attributes.forEach(attrs::put);
			client.put("attributes", attrs);
		}

		client.put("standardFlowEnabled", oidcClient.standardFlowEnabled);
		client.put("directAccessGrantsEnabled", oidcClient.directAccessGrantsEnabled);
		client.put("serviceAccountsEnabled", oidcClient.serviceAccountsEnabled);

		flowId.ifPresent(fi -> {
			JsonObject overrides = new JsonObject();
			overrides.put("browser", fi);
			client.put("authenticationFlowBindingOverrides", overrides);
		});

		protocolMapper.ifPresent(pm -> client.put("protocolMappers", pm.toJson()));

		return client;
	}
}
