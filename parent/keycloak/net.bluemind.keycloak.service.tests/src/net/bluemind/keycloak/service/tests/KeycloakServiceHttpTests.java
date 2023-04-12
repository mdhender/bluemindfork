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
package net.bluemind.keycloak.service.tests;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.domain.api.IDomains;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.IKeycloakBluemindProviderAdmin;
import net.bluemind.keycloak.api.IKeycloakClientAdmin;
import net.bluemind.keycloak.api.IKeycloakFlowAdmin;
import net.bluemind.keycloak.api.IKeycloakKerberosAdmin;

public class KeycloakServiceHttpTests extends KeycloakServiceTests {
	protected IKeycloakAdmin getKeycloakAdminService() throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://localhost:8090", securityContext.getSessionId())
				.instance(IKeycloakAdmin.class);
	}

	protected IKeycloakClientAdmin getKeycloakClientAdminService() throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://localhost:8090", securityContext.getSessionId())
				.instance(IKeycloakClientAdmin.class, testRealmName);
	}

	protected IKeycloakBluemindProviderAdmin getKeycloakBluemindProviderService() throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://localhost:8090", securityContext.getSessionId())
				.instance(IKeycloakBluemindProviderAdmin.class, testRealmName);
	}

	protected IKeycloakKerberosAdmin getKeycloakKerberosService() throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://localhost:8090", securityContext.getSessionId())
				.instance(IKeycloakKerberosAdmin.class, testRealmName);
	}

	protected IKeycloakFlowAdmin getKeycloakFlowService() throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://localhost:8090", securityContext.getSessionId())
				.instance(IKeycloakFlowAdmin.class, testRealmName);
	}

	protected IDomains getDomainService() throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://localhost:8090", securityContext.getSessionId())
				.instance(IDomains.class);
	}
}
