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
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.IKeycloakUids;

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
			IKeycloakAdmin service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IKeycloakAdmin.class);
			if (service.getRealm(domainUid) == null) {
				logger.info("Repair keycloack configuration for domain {}", domainUid);
				String realm = domainUid;
				String clientId = IKeycloakUids.clientId(domainUid);

				service.createRealm(realm);
				service.createClient(realm, clientId);
				String secret = service.getClientSecret(realm, clientId);

				IDomainSettings settingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
						.instance(IDomainSettings.class, domainUid);
				Map<String, String> settings = settingsApi.get();
				settings.put(DomainSettingsKeys.openid_realm.name(), realm);
				settings.put(DomainSettingsKeys.openid_client_id.name(), clientId);
				settings.put(DomainSettingsKeys.openid_client_secret.name(), secret);
				settingsApi.set(settings);
			} else {
				logger.info("Keycloack configuration: nothing to repair for domain {}", domainUid);
			}
		}

	}

}
