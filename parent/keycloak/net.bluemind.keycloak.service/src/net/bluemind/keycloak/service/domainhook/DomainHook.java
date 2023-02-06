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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.keycloak.api.BluemindProviderComponent;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.IKeycloakBluemindProviderAdmin;
import net.bluemind.keycloak.api.IKeycloakClientAdmin;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.Topology;
import net.bluemind.openid.api.OpenIdProperties;
import net.bluemind.server.api.TagDescriptor;

public class DomainHook extends DomainHookAdapter {

	private static final Logger logger = LoggerFactory.getLogger(DomainHook.class);

	@Override
	public void onCreated(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		if ("global.virt".equals(domain.uid)) {
			return;
		}
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
		bpComponent.setBmUrl("https://" + Topology.get().any(TagDescriptor.bm_core.getTag()).value.address()); // ou
																												// pas...
																												// à
																												// vérifier
		bpComponent.setBmCoreToken(Token.admin0());
		keycloakBluemindProviderService.create(bpComponent);

		IKeycloakClientAdmin keycloakRealmAdminService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IKeycloakClientAdmin.class, domain.uid);
		keycloakRealmAdminService.create(clientId);
		String secret = keycloakRealmAdminService.getSecret(clientId);

		domain.value.properties.put(OpenIdProperties.OPENID_REALM.name(), realm);
		domain.value.properties.put(OpenIdProperties.OPENID_CLIENT_ID.name(), clientId);
		domain.value.properties.put(OpenIdProperties.OPENID_CLIENT_SECRET.name(), secret);

		IDomains domainService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		domainService.update(domain.uid, domain.value);

		String opendIdHost = IKeycloakUids
				.defaultHost(Topology.get().any(TagDescriptor.bm_keycloak.getTag()).value.address(), domain.uid);

		URI uri;
		try {
			uri = new URI(opendIdHost);
		} catch (URISyntaxException e) {
			logger.error(e.getMessage(), e);
			return;
		}

		HttpClient client = initHttpClient(uri);
		client.request(HttpMethod.GET, uri.getPath())
				.onSuccess(req -> req.send().onSuccess(res -> res.bodyHandler(body -> {
					JsonObject conf = new JsonObject(new String(body.getBytes()));
					if (domain.value.properties == null) {
						domain.value.properties = new HashMap<>();
					}
					domain.value.properties.put(OpenIdProperties.OPENID_HOST.name(), opendIdHost);
					domain.value.properties.put(OpenIdProperties.OPENID_REALM.name(), domain.uid);
					domain.value.properties.put(OpenIdProperties.OPENID_CLIENT_ID.name(), IKeycloakUids.clientId(domain.uid));
					domain.value.properties.put(OpenIdProperties.OPENID_CLIENT_SECRET.name(), secret);
					domain.value.properties.put(OpenIdProperties.OPENID_AUTHORISATION_ENDPOINT.name(),
							conf.getString("authorization_endpoint"));
					domain.value.properties.put(OpenIdProperties.OPENID_TOKEN_ENDPOINT.name(),
							conf.getString("token_endpoint"));
					domain.value.properties.put(OpenIdProperties.OPENID_JWKS_URI.name(), conf.getString("jwks_uri"));
					String accessTokenIssuer = Optional.ofNullable(conf.getString("issuer"))
							.orElse(conf.getString("access_token_issuer"));
					domain.value.properties.put(OpenIdProperties.OPENID_ISSUER.name(), accessTokenIssuer);
					domain.value.properties.put(OpenIdProperties.OPENID_END_SESSION_ENDPOINT.name(),
							conf.getString("end_session_endpoint"));

					domainService.update(domain.uid, domain.value);

				}))).onFailure(t -> logger.error(t.getMessage(), t));

	}

	private HttpClient initHttpClient(URI uri) {
		HttpClientOptions opts = new HttpClientOptions();
		opts.setDefaultHost(uri.getHost());
		opts.setSsl(uri.getScheme().equalsIgnoreCase("https"));
		opts.setDefaultPort(
				uri.getPort() != -1 ? uri.getPort() : (uri.getScheme().equalsIgnoreCase("https") ? 443 : 80));
		if (opts.isSsl()) {
			opts.setTrustAll(true);
			opts.setVerifyHost(false);
		}
		return VertxPlatform.getVertx().createHttpClient(opts);
	}

	@Override
	public void onDeleted(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		if ("global.virt".equals(domain.uid)) {
			return;
		}
		logger.info("Delete Keycloak realm for domain {}", domain.uid);
		IKeycloakAdmin service = context.provider().instance(IKeycloakAdmin.class);
		service.deleteRealm(domain.uid);
	}

}
