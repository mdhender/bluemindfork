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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.keycloak.api.BluemindProviderComponent;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.IKeycloakBluemindProviderAdmin;
import net.bluemind.keycloak.api.IKeycloakClientAdmin;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.config.Token;

public class KeycloakRealmRepairSupport implements IDirEntryRepairSupport {
	private static final Logger logger = LoggerFactory.getLogger(KeycloakRealmRepairSupport.class);

	public static final MaintenanceOperation keycloakRepair = MaintenanceOperation.create("domain.keycloak",
			"Ensure that domain has a correct keycloak configuration (realm, client)");

	public static class Factory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new KeycloakRealmRepairSupport(context);
		}
	}

	public KeycloakRealmRepairSupport(BmContext context) {
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.DOMAIN) {
			return Set.of(keycloakRepair);
		}
		return Collections.emptySet();
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.DOMAIN) {
			return Set.of(new KeycloakRepairImpl());
		}
		return Collections.emptySet();

	}

	private static class KeycloakRepairImpl extends InternalMaintenanceOperation {

		public KeycloakRepairImpl() {
			super(keycloakRepair.identifier, null, null, 1);
		}

		@Override
		public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			monitor.end();
		}

		@Override
		public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			IKeycloakAdmin keycloakAdminService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IKeycloakAdmin.class);
			if (keycloakAdminService.getRealm(domainUid) == null) {
				logger.info("Repair keycloack configuration for domain {}", domainUid);
				String realm = domainUid;
				String clientId = IKeycloakUids.clientId(domainUid);

				keycloakAdminService.createRealm(realm);
				
				IKeycloakBluemindProviderAdmin keycloakBluemindProviderService = ServerSideServiceProvider
						.getProvider(SecurityContext.SYSTEM).instance(IKeycloakBluemindProviderAdmin.class, domainUid);
				BluemindProviderComponent bpComponent = new BluemindProviderComponent();
				bpComponent.setParentId(realm);
				bpComponent.setName(realm + "-bmprovider");
				bpComponent.setBmUrl("https://" + Topology.get().any(TagDescriptor.bm_core.getTag()).value.address()); //ou pas... à vérifier
				bpComponent.setBmCoreToken(Token.admin0());
				keycloakBluemindProviderService.create(bpComponent);
				

				IKeycloakClientAdmin keycloakClientService = ServerSideServiceProvider
						.getProvider(SecurityContext.SYSTEM).instance(IKeycloakClientAdmin.class, domainUid);

				keycloakClientService.create(clientId);
				String secret = keycloakClientService.getSecret(clientId);

				String opendIdHost = IKeycloakUids
						.defaultHost(Topology.get().any(TagDescriptor.bm_keycloak.getTag()).value.address(), realm);

				URI uri;
				try {
					uri = new URI(opendIdHost);
				} catch (URISyntaxException e) {
					logger.error(e.getMessage(), e);
					monitor.end(false, e.getMessage(), "[]");
					return;
				}

				HttpClient client = initHttpClient(uri);
				client.request(HttpMethod.GET, uri.getPath())
						.onSuccess(req -> req.send().onSuccess(res -> res.bodyHandler(body -> {
							JsonObject conf = new JsonObject(new String(body.getBytes()));

							IDomainSettings settingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
									.instance(IDomainSettings.class, domainUid);
							Map<String, String> settings = settingsApi.get();
							settings.put(DomainSettingsKeys.openid_host.name(), opendIdHost);
							settings.put(DomainSettingsKeys.openid_realm.name(), realm);
							settings.put(DomainSettingsKeys.openid_client_id.name(), clientId);
							settings.put(DomainSettingsKeys.openid_client_secret.name(), secret);

							settings.put(DomainSettingsKeys.openid_authorization_endpoint.name(),
									conf.getString("authorization_endpoint"));

							settings.put(DomainSettingsKeys.openid_token_endpoint.name(),
									conf.getString("token_endpoint"));

							settings.put(DomainSettingsKeys.openid_jwks_uri.name(), conf.getString("jwks_uri"));

							String accessTokenIssuer = Optional.ofNullable(conf.getString("issuer"))
									.orElse(conf.getString("access_token_issuer"));
							settings.put(DomainSettingsKeys.openid_issuer.name(), accessTokenIssuer);

							settings.put(DomainSettingsKeys.openid_end_session_endpoint.name(),
									conf.getString("end_session_endpoint"));

							settingsApi.set(settings);
						}))).onFailure(t -> logger.error(t.getMessage(), t));
			} else {
				logger.info("Keycloack configuration: nothing to repair for domain {}", domainUid);
			}

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

	}

}
