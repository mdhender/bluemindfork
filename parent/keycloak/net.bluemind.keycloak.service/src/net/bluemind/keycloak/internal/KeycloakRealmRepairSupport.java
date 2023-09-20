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

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.keycloak.api.BluemindProviderComponent;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.IKeycloakBluemindProviderAdmin;
import net.bluemind.keycloak.api.IKeycloakClientAdmin;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.keycloak.api.OidcClient;
import net.bluemind.keycloak.api.Realm;
import net.bluemind.keycloak.utils.KeycloakHelper;
import net.bluemind.keycloak.utils.adapters.BlueMindComponentAdapter;
import net.bluemind.keycloak.utils.adapters.OidcClientAdapter;
import net.bluemind.keycloak.utils.adapters.RealmAdapter;

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
		private static class KeycloakConf {
			public static KeycloakConf build(String domainUid) {
				ItemValue<Domain> domain = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IDomains.class).get(domainUid);

				Realm realm = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IKeycloakAdmin.class, domainUid).getRealm(domainUid);
				OidcClient oidcClient = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IKeycloakClientAdmin.class, domainUid)
						.getOidcClient(IKeycloakUids.clientId(domainUid));
				BluemindProviderComponent bluemindProviderService = ServerSideServiceProvider
						.getProvider(SecurityContext.SYSTEM).instance(IKeycloakBluemindProviderAdmin.class, domainUid)
						.getBluemindProvider(IKeycloakUids.bmProviderId(domainUid));

				return new KeycloakConf(domain, realm, oidcClient, bluemindProviderService);
			}

			private ItemValue<Domain> domain;
			private Realm realm;
			private OidcClient oidcClient;
			private BluemindProviderComponent bluemindProviderService;

			private KeycloakConf(ItemValue<Domain> domain, Realm realm, OidcClient oidcClient,
					BluemindProviderComponent bluemindProviderService) {
				this.domain = domain;
				this.realm = realm;
				this.oidcClient = oidcClient;
				this.bluemindProviderService = bluemindProviderService;
			}

			public boolean isOk() {
				boolean ok = true;

				Realm expectedRealm = RealmAdapter.build(domain.uid).realm;
				if (!Objects.equal(expectedRealm, realm)) {
					ok = false;
					logger.error("Realm ko for domain: {} - must be:\n{}\nis: {}", domain.uid, expectedRealm, realm);
				}

				OidcClient expectedOidcClient = OidcClientAdapter.build(domain.uid, IKeycloakUids.clientId(domain.uid),
						null).oidcClient;
				expectedOidcClient.secret = domain.value.properties
						.get(AuthDomainProperties.OPENID_CLIENT_SECRET.name());
				if (!Objects.equal(expectedOidcClient, oidcClient)) {
					ok = false;
					logger.info("OidcClient ko for domain: {} - must be:\n{}\nis: {}", domain.uid, expectedOidcClient,
							oidcClient);
				}

				BluemindProviderComponent expectedBlueMindProviderService = BlueMindComponentAdapter
						.build(domain.uid).component;
				// Can't be checked as Keycloak keep it secret
				expectedBlueMindProviderService.bmCoreToken = null;
				if (!Objects.equal(expectedBlueMindProviderService, bluemindProviderService)) {
					ok = false;
					logger.info("BluemindProvider ko for domain: {} - must be:\n{}\nis: {}", domain.uid,
							expectedBlueMindProviderService, bluemindProviderService);
				}

				return ok;
			}
		}

		public KeycloakRepairImpl() {
			super(keycloakRepair.identifier, null, null, 1);
		}

		@Override
		public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			monitor.begin(1, "Check Keycloak configuration for domain: " + domainUid);

			boolean confOk = KeycloakConf.build(domainUid).isOk();

			monitor.progress(1, "Keycloak configuration is " + (confOk ? "ok" : "ko"));
			monitor.end();
		}

		@Override
		public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			monitor.begin(2, "Check Keycloak configuration for domain: " + domainUid);
			boolean confOk = KeycloakConf.build(domainUid).isOk();

			if (confOk) {
				monitor.progress(2, "Keycloak configuration don't need repair for domain: " + domainUid);
				monitor.end();
				return;
			}

			monitor.progress(1, "Keycloak configuration need repair for domain: " + domainUid);

			ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			IKeycloakAdmin keycloakAdminService = provider.instance(IKeycloakAdmin.class);
			keycloakAdminService.deleteRealm(domainUid);
			KeycloakHelper.initForDomain(domainUid);

			monitor.progress(1, "Reset keycloak configuration for domain: " + domainUid);
			monitor.end();
		}
	}
}
